package gui.components;

import gui.theme.Theme;
import gui.theme.RoundedBorder;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL; // Nécessaire pour charger les icônes

public class StyledButton extends JButton {

    private Color baseColor;
    private Color hoverColor;
    private Color pressedColor;
    private int radius = 12; // Rayon par défaut pour les coins arrondis
	private ImageIcon icon;

    // Constructeur par défaut, utilise une couleur neutre
    public StyledButton(String text) {
        this(text, Theme.BTN_NEUTRE, Color.WHITE);
    }

    // Constructeur principal avec couleurs personnalisées
    public StyledButton(String text, Color bgColor, Color fgColor) {
        super(text);
        this.baseColor = bgColor;
        this.hoverColor = Theme.hover(bgColor);
        this.pressedColor = Theme.pressed(bgColor);

        // La couleur de fond est gérée par paintComponent, mais setBackground est utile pour les effets de survol/pression
        setBackground(baseColor);
        setForeground(fgColor);     // Couleur du texte
        setFont(Theme.fontUI(Font.BOLD, 13)); // Police standard
        setFocusPainted(false);     // Ne pas dessiner le contour de focus
        setContentAreaFilled(false); // Indique que le composant dessinera sa propre zone de contenu
        setBorderPainted(false);    // Ne pas dessiner la bordure par défaut de Swing
        setOpaque(false);           // Important pour que paintComponent fonctionne comme prévu
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // Curseur main au survol

        addMouseListener(new HoverListener()); // Ajoute l'effet de survol
    }

    // --- NOUVEAU CONSTRUCTEUR ET MÉTHODE POUR LES ICÔNES ---
    /**
     * Constructeur pour les boutons avec icône.
     * @param icon L'icône du bouton.
     * @param bgColor La couleur de fond du bouton.
     * @param fgColor La couleur du texte (si texte est présent, sinon moins pertinent).
     */
    public StyledButton(ImageIcon icon, Color bgColor, Color fgColor) {
        this("", bgColor, fgColor); // Appelle le constructeur principal avec un texte vide
        setIcon(icon); // Définit l'icône
    }

    /**
     * Définit l'icône du bouton.
     * @param icon L'ImageIcon à afficher.
     */
    public void setIcon(ImageIcon icon) {
        super.setIcon(icon); // Définit l'icône dans le JButton parent
        this.icon = icon; // Stocke l'icône si besoin pour des calculs de taille personnalisés
        // Ajustement de la taille préférée basée sur l'icône pour un meilleur rendu
        if (icon != null) {
            // Le padding interne est de 8px haut/bas et 12px gauche/droite (voir createToolbarButton)
            // Le radius est de 8.
            // On veut que le bouton soit juste assez grand pour contenir l'icône + padding + un peu pour l'arrondi.
            // L'espacement est géré par le parent (JToolBar) et le layout.
            // On peut fixer une taille minimale ou ajuster la taille préférée pour que l'icône soit bien centrée.
            // Une approche simple est de s'assurer que la hauteur est suffisante.
            int preferredHeight = Math.max(icon.getIconHeight() + 16, 30); // Min hauteur de 30px, ou icône + padding
            int preferredWidth = icon.getIconWidth() + 24; // Padding gauche/droite
            setPreferredSize(new Dimension(preferredWidth, preferredHeight));
        }
    }
    // --- FIN DES NOUVEAUTÉS POUR LES ICÔNES ---


    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(getBackground()); // Utilise la couleur de fond actuelle (gérée par HoverListener)
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius); // Dessine le fond arrondi
        g2d.dispose();
        super.paintComponent(g); // Appelle la méthode super pour dessiner le texte, l'icône, etc.
    }

    /**
     * Définit le rayon des coins arrondis du bouton.
     * @param radius Le rayon en pixels.
     */
    public void setRadius(int radius) {
        this.radius = radius;
        repaint(); // Redessine le bouton avec le nouveau rayon
    }

    /**
     * Définit les couleurs de base, de survol et de pression du bouton.
     * @param base La couleur de base.
     * @param hover La couleur au survol.
     * @param pressed La couleur à la pression.
     */
    public void setColors(Color base, Color hover, Color pressed) {
        this.baseColor = base;
        this.hoverColor = hover;
        this.pressedColor = pressed;
        setBackground(baseColor); // Met à jour la couleur de fond actuelle
    }

    // Listener interne pour gérer les effets de survol et de pression
    private class HoverListener extends MouseAdapter {
        @Override
        public void mouseEntered(MouseEvent e) {
            setBackground(hoverColor);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            setBackground(baseColor);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            setBackground(pressedColor);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            // Il est plus logique de revenir à la couleur de survol si la souris est toujours sur le bouton,
            // sinon à la couleur de base. Mais pour la simplicité, revenir à hoverColor est aussi courant.
            // Vérifions si la souris est toujours sur le bouton pour un comportement plus précis.
            if (e.getComponent().contains(e.getPoint())) {
                setBackground(hoverColor);
            } else {
                setBackground(baseColor);
            }
        }
    }

    // ==================== FACTORY METHODS ====================
    // Méthodes statiques pour créer des StyledButton avec des styles prédéfinis

    public static StyledButton primary(String text) {
        StyledButton btn = new StyledButton(text, Theme.BTN_PRIMAIRE, Color.WHITE);
        btn.setPreferredSize(new Dimension(130, 35));
        return btn;
    }

    public static StyledButton danger(String text) {
        StyledButton btn = new StyledButton(text, Theme.BTN_DANGER, Color.WHITE);
        btn.setPreferredSize(new Dimension(130, 35));
        return btn;
    }

    public static StyledButton info(String text) {
        StyledButton btn = new StyledButton(text, Theme.BTN_INFO, Color.WHITE);
        btn.setPreferredSize(new Dimension(130, 35));
        return btn;
    }

    public static StyledButton neutral(String text) {
        StyledButton btn = new StyledButton(text, Theme.BTN_NEUTRE, Color.WHITE);
        btn.setPreferredSize(new Dimension(130, 35));
        return btn;
    }
}
