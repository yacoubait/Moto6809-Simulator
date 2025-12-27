package gui.panels;

import gui.theme.Theme;
import gui.theme.RoundedBorder; // Ajout de l'import pour RoundedBorder
import gui.components.StyledTextField; // Utilisation des champs de texte stylisés
import cpu.CPU6809; // Pour s'abonner aux changements du CPU
// import exec.ALU; // Commenté car ALU n'est pas directement utilisé ici, seulement ses constantes de flags si elles étaient utilisées

import javax.swing.*; // Import générique pour les composants Swing
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

public class FlagsPanel extends JPanel implements PropertyChangeListener {
    
    // Ordre des flags tel qu'ils sont définis dans le registre CCR du 6809 (MSB à LSB)
    // E, F, H, I, N, Z, V, C (Bit 7 à Bit 0)
    private static final String[] FLAG_NAMES_DISPLAY_ORDER = {"E", "F", "H", "I", "N", "Z", "V", "C"};
    private static final Map<String, String> FLAG_DESCRIPTIONS = new LinkedHashMap<>(); // Pour garder l'ordre d'insertion
    
    // Initialisation statique des descriptions alignée avec l'ordre d'affichage standard
    static {
        FLAG_DESCRIPTIONS.put("E", "Entire (Fast Interrupt Mask)");
        FLAG_DESCRIPTIONS.put("F", "FIRQ Mask (Fast Interrupt Mask)");
        FLAG_DESCRIPTIONS.put("H", "Half Carry (used for BCD arithmetic)");
        FLAG_DESCRIPTIONS.put("I", "IRQ Mask (Interrupt Request Mask)");
        FLAG_DESCRIPTIONS.put("N", "Negative (result is negative)");
        FLAG_DESCRIPTIONS.put("Z", "Zero (result is zero)");
        FLAG_DESCRIPTIONS.put("V", "Overflow (signed overflow occurred)");
        FLAG_DESCRIPTIONS.put("C", "Carry (unsigned overflow or borrow)");
    }

    private final Map<String, JTextField> flagsFields = new LinkedHashMap<>(); // Utilise LinkedHashMap pour garder l'ordre d'affichage
    private CPU6809 cpu; // Référence au CPU pour l'abonnement

    public FlagsPanel() {
        initPanel();
        buildLayout();
    }
    
    /**
     * Définit l'instance du CPU à observer et s'abonne spécifiquement aux changements du registre CC.
     * @param cpu L'instance de CPU6809.
     */
    public void setCPU(CPU6809 cpu) {
        if (this.cpu != null) {
            // Désabonnement de l'ancien CPU si un nouveau est défini
            // Correction ici: 'this' est le listener (l'instance de FlagsPanel)
            this.cpu.removePropertyChangeListener(this); 
        }
        this.cpu = cpu;
        if (this.cpu != null) {
            // Abonnement spécifique au registre CC avec 'this' comme listener
            this.cpu.addPropertyChangeListener("CC", this); 
            setAllFlags(cpu.getCC()); // Mise à jour initiale des flags
        }
    }

    private void initPanel() {
        setLayout(new FlowLayout(FlowLayout.CENTER, 15, 10)); // Disposition des drapeaux
        setOpaque(false); // Rendu transparent pour que le fond du conteneur parent soit visible
        setBackground(Theme.FOND); // Couleur de fond (utilisée par paintComponent)
        
        // Bordure composée : arrondie extérieurement, et titrée intérieurement
        setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(Theme.BORDURE, 1, 20, new Insets(20, 15, 10, 15)), // Bordure extérieure arrondie
            BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(), // Pas de bordure pour le titre lui-même
                "Registre d'état (CCR)", // Titre
                TitledBorder.CENTER, // Alignement du titre
                TitledBorder.TOP,    // Position du titre
                Theme.fontUI(Font.BOLD, 13), // Police du titre
                Theme.TEXTE          // Couleur du titre
            )
        ));
    }
    
    private void buildLayout() {
        // Crée et ajoute un panneau pour chaque drapeau dans l'ordre défini par FLAG_NAMES_DISPLAY_ORDER
        for (String name : FLAG_NAMES_DISPLAY_ORDER) {
            add(createFlagPanel(name, FLAG_DESCRIPTIONS.getOrDefault(name, "Unknown Flag")));
        }
    }
    
    /**
     * Crée un petit panneau pour afficher un seul drapeau.
     * @param name Le nom du drapeau (ex: "C").
     * @param description La description du drapeau (pour le tooltip).
     * @return Le JPanel contenant le label du drapeau et son champ de valeur.
     */
    private JPanel createFlagPanel(String name, String description) {
        JPanel panel = new JPanel(new BorderLayout(2, 2)); // Disposition verticale pour label et valeur
        panel.setOpaque(false); // Transparent
        panel.setToolTipText(description); // Tooltip pour afficher la description complète
        
        JLabel label = new JLabel(name); // Nom du drapeau (ex: "C")
        label.setFont(Theme.fontMono(Font.BOLD, 12)); // Police monospace et gras
        label.setHorizontalAlignment(JLabel.CENTER); // Centre le texte
        label.setForeground(Theme.TEXTE); // Couleur du texte
        
        JTextField field = StyledTextField.forFlag(); // Utilise le champ de texte stylisé pour les flags
        flagsFields.put(name, field); // Stocke le champ dans la map
        
        panel.add(label, BorderLayout.NORTH); // Label en haut
        panel.add(field, BorderLayout.CENTER); // Valeur au centre
        
        return panel;
    }
    
    /**
     * Surcharge de paintComponent pour dessiner le fond arrondi du panneau.
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(getBackground()); // Utilise la couleur de fond définie dans initPanel
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20); // Dessine un rectangle arrondi
        g2d.dispose();
        super.paintComponent(g); // Appelle la méthode parente pour le reste du dessin (enfants, bordure)
    }
    
    // ==================== API PUBLIQUE ====================
    
    /**
     * Définit la valeur d'un drapeau spécifique.
     * @param name Le nom du drapeau (ex: "C", "N", "Z").
     * @param value La valeur du drapeau (0 ou 1).
     */
    public void setFlag(String name, int value) {
        setFlag(name, String.valueOf(value));
    }
    
    /**
     * Définit la valeur d'un drapeau spécifique.
     * @param name Le nom du drapeau.
     * @param value La valeur du drapeau sous forme de chaîne.
     */
    public void setFlag(String name, String value) {
        JTextField field = flagsFields.get(name.toUpperCase()); // Recherche insensible à la casse
        if (field != null) {
            field.setText(value);
        }
    }
    
    /**
     * Retourne la valeur actuelle d'un drapeau spécifique.
     * @param name Le nom du drapeau.
     * @return La valeur du drapeau (0 ou 1), ou 0 si non trouvé/erreur.
     */
    public int getFlag(String name) {
        JTextField field = flagsFields.get(name.toUpperCase());
        if (field != null) {
            try {
                return Integer.parseInt(field.getText());
            } catch (NumberFormatException e) {
                return 0; // Retourne 0 en cas d'erreur de parsing
            }
        }
        return 0; // Retourne 0 si le drapeau n'existe pas
    }
    
    /**
     * Définit l'état de tous les drapeaux en utilisant la valeur du registre CCR (8 bits).
     * @param ccr La valeur du registre Condition Codes.
     */
    public void setAllFlags(int ccr) {
        // Utilise les décalages de bits pour extraire chaque flag (ordre E, F, H, I, N, Z, V, C)
        setFlag("C", (ccr >> 0) & 1); // Bit 0
        setFlag("V", (ccr >> 1) & 1); // Bit 1
        setFlag("Z", (ccr >> 2) & 1); // Bit 2
        setFlag("N", (ccr >> 3) & 1); // Bit 3
        setFlag("I", (ccr >> 4) & 1); // Bit 4
        setFlag("H", (ccr >> 5) & 1); // Bit 5
        setFlag("F", (ccr >> 6) & 1); // Bit 6
        setFlag("E", (ccr >> 7) & 1); // Bit 7
    }
    
    /**
     * Construit la valeur du registre CCR (8 bits) à partir de l'état des drapeaux affichés.
     * @return La valeur calculée du registre CCR.
     */
    public int getAllFlags() {
        int ccr = 0;
        ccr |= getFlag("C") << 0;
        ccr |= getFlag("V") << 1;
        ccr |= getFlag("Z") << 2;
        ccr |= getFlag("N") << 3;
        ccr |= getFlag("I") << 4;
        ccr |= getFlag("H") << 5;
        ccr |= getFlag("F") << 6;
        ccr |= getFlag("E") << 7;
        return ccr;
    }
    
    /**
     * Réinitialise tous les drapeaux à 0.
     */
    public void resetAll() {
        flagsFields.values().forEach(f -> f.setText("0"));
    }
    
    /**
     * Génère une représentation textuelle de l'état des drapeaux.
     * @return Une chaîne de caractères représentant l'état des drapeaux (ex: "[E-HINZ-C]").
     */
    public String getFlagsString() {
        StringBuilder sb = new StringBuilder("[");
        // Itère dans l'ordre d'affichage pour la cohérence
        for (String name : FLAG_NAMES_DISPLAY_ORDER) {
            sb.append(getFlag(name) == 1 ? name : "-");
        }
        sb.append("]");
        return sb.toString();
    }

    // ==================== PropertyChangeListener Implementation ====================

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // L'événement "CC" est envoyé par CPU6809.setCC()
        if ("CC".equals(evt.getPropertyName())) {
            SwingUtilities.invokeLater(() -> {
                int newCC = (Integer) evt.getNewValue();
                setAllFlags(newCC); // Met à jour tous les champs de flag
            });
        }
    }
}
