package gui.theme;

import javax.swing.border.AbstractBorder; // <-- Import manquant ajouté ici
import java.awt.*;

public class RoundedBorder extends AbstractBorder {
    
    private final Color color;
    private final int thickness; // Épaisseur de la bordure
    private final int radius;    // Rayon des coins arrondis
    private final Insets insets; // Marges internes fournies par la bordure
    
    /**
     * Constructeur complet pour une bordure arrondie.
     * @param color La couleur de la bordure.
     * @param thickness L'épaisseur de la ligne de la bordure.
     * @param radius Le rayon d'arrondi des coins.
     * @param insets Les marges internes que la bordure ajoute au composant.
     */
    public RoundedBorder(Color color, int thickness, int radius, Insets insets) {
        this.color = color;
        this.thickness = thickness;
        this.radius = radius;
        this.insets = insets;
    }
    
    /**
     * Constructeur simplifié, utilise les valeurs par défaut de Theme pour la couleur,
     * l'épaisseur (1) et les insets (INSETS_NORMAL).
     * @param radius Le rayon d'arrondi des coins.
     */
    public RoundedBorder(int radius) {
        this(Theme.BORDURE, 1, radius, Theme.INSETS_NORMAL);
    }
    
    /**
     * Dessine la bordure arrondie autour du composant.
     * @param c Le composant sur lequel la bordure est dessinée.
     * @param g L'objet Graphics utilisé pour le dessin.
     * @param x La coordonnée X de l'origine de la bordure.
     * @param y La coordonnée Y de l'origine de la bordure.
     * @param width La largeur de la zone de dessin de la bordure.
     * @param height La hauteur de la zone de dessin de la bordure.
     */
    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2d = (Graphics2D) g.create(); // Crée une copie du contexte graphique
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // Active l'anti-aliasing
        g2d.setColor(color); // Définit la couleur de la bordure
        g2d.setStroke(new BasicStroke(thickness)); // Définit l'épaisseur de la bordure
        // Dessine le rectangle arrondi. Les -1 ajustent la taille pour rester dans les limites du composant.
        g2d.drawRoundRect(x, y, width - 1, height - 1, radius, radius); 
        g2d.dispose(); // Libère les ressources graphiques
    }
    
    /**
     * Retourne les marges internes de cette bordure.
     * @param c Le composant.
     * @return Les Insets définies pour cette bordure.
     */
    @Override
    public Insets getBorderInsets(Component c) {
        // Retourne une copie des insets pour éviter que des modifications externes n'affectent l'original
        return new Insets(insets.top, insets.left, insets.bottom, insets.right); 
    }
    
    /**
     * Définit les marges internes du composant et les retourne.
     * @param c Le composant.
     * @param insets L'objet Insets à modifier et retourner.
     * @return L'objet Insets modifié.
     */
    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.set(this.insets.top, this.insets.left, this.insets.bottom, this.insets.right);
        return insets;
    }
}
