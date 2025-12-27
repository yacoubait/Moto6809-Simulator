package gui.components;

import gui.theme.Theme;
import gui.theme.RoundedBorder;
import javax.swing.*;
import java.awt.*;

public class StyledTextField extends JTextField {
    
    private int radius = Theme.RAYON_ARRONDI;
    private boolean showBorder = true; // Pas utilisé directement pour le dessin, mais pour l'API
    
    public StyledTextField() {
        this(10);
    }
    
    public StyledTextField(int columns) {
        super(columns);
        initStyle();
    }
    
    public StyledTextField(String text, int columns) {
        super(text, columns);
        initStyle();
    }
    
    private void initStyle() {
        setOpaque(false); // Dessin du fond personnalisé dans paintComponent
        setFont(Theme.FONT_REGISTRE);
        setBackground(Theme.REGISTRE); // Couleur de fond (pour paintComponent)
        setForeground(Theme.TEXTE);
        setCaretColor(Theme.TEXTE); // Couleur du curseur
        setHorizontalAlignment(JTextField.CENTER);
        setBorder(new RoundedBorder(Theme.BORDURE, 1, radius, Theme.INSETS_NORMAL));
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(getBackground()); // Utilise la couleur de fond définie
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius); // Dessine le fond arrondi
        g2d.dispose();
        super.paintComponent(g); // Appelle la méthode parente pour le texte, curseur, etc.
    }
    
    /**
     * Définit le rayon des coins arrondis du champ de texte.
     * @param radius Le rayon en pixels.
     */
    public void setRadius(int radius) {
        this.radius = radius;
        // Met à jour la bordure pour qu'elle ait aussi le nouveau rayon
        if (showBorder) {
            setBorder(new RoundedBorder(Theme.BORDURE, 1, radius, Theme.INSETS_NORMAL));
        }
        repaint(); // Redessine le composant
    }
    
    /**
     * Définit si la bordure est affichée.
     * @param show true pour afficher la bordure, false pour la masquer (bordure vide).
     */
    public void setShowBorder(boolean show) {
        this.showBorder = show;
        if (!show) {
            setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10)); // Une bordure vide pour maintenir le padding
        } else {
            setBorder(new RoundedBorder(Theme.BORDURE, 1, radius, Theme.INSETS_NORMAL)); // Restaure la bordure arrondie
        }
        repaint();
    }
    
    // ==================== FACTORY METHODS ====================
    
    /**
     * Crée un StyledTextField pour l'affichage des registres (8 colonnes, non éditable).
     * @param columns Le nombre de colonnes à afficher (4 pour 8 bits, 8 pour 16 bits).
     * @return Un StyledTextField pour registre.
     */
    public static StyledTextField forRegister(int columns) {
        StyledTextField tf = new StyledTextField(columns);
        tf.setEditable(false);
        tf.setText(String.format("%0" + columns/2 + "X", 0)); // Ex: "0000" pour 16 bits
        return tf;
    }
    
    /**
     * Crée un StyledTextField pour l'affichage des flags (2 colonnes, non éditable).
     * @return Un StyledTextField pour flag.
     */
    public static StyledTextField forFlag() {
        StyledTextField tf = new StyledTextField(2);
        tf.setEditable(false);
        tf.setRadius(12); // Rayon plus petit pour les flags
        tf.setBorder(new RoundedBorder(Theme.BORDURE, 1, 12, Theme.INSETS_PETIT)); // Bordure spécifique aux flags
        tf.setText("0");
        return tf;
    }
    
    /**
     * Crée un StyledTextField pour la saisie utilisateur (éditable).
     * @param columns Nombre de colonnes préféré.
     * @return Un StyledTextField pour la saisie.
     */
    public static StyledTextField forInput(int columns) {
        return new StyledTextField(columns);
    }
    
    /**
     * Crée un StyledTextField pour afficher un chemin de fichier (non éditable, aligné à gauche).
     * @return Un StyledTextField pour chemin.
     */
    public static StyledTextField forPath() {
        StyledTextField tf = new StyledTextField(30);
        tf.setEditable(false);
        tf.setHorizontalAlignment(JTextField.LEFT);
        tf.setRadius(10);
        return tf;
    }


}
