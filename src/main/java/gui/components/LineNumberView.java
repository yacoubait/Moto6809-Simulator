package gui.components;

import gui.theme.Theme;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;

public class LineNumberView extends JTextArea implements DocumentListener {
    
    private final JTextPane textPane;
    private int lastKnownLineCount = 0; // Pour optimiser les mises à jour

    public LineNumberView(JTextPane textPane) {
        this.textPane = textPane;
        
        setEditable(false);
        setBackground(new Color(220, 225, 230));
        setForeground(new Color(120, 130, 140));
        setFont(Theme.FONT_CODE);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Marge interne
        
        textPane.getDocument().addDocumentListener(this);
        
        // S'assurer que le LineNumberView défile avec le TextPane
        JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, textPane);
        if (scrollPane != null) {
            scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
                // Synchronise la position de défilement vertical du LineNumberView
                SwingUtilities.invokeLater(() -> this.scrollRectToVisible(
                    new Rectangle(0, e.getValue(), 1, 1) // Juste pour déclencher la mise à jour visuelle
                ));
            });
        }

        update(); // Mise à jour initiale des numéros de ligne
    }
    
    /**
     * Met à jour les numéros de ligne affichés dans le JTextArea.
     * Ajuste également la largeur préférée du JTextArea.
     */
    public void update() {
        // Obtenir le nombre de lignes dans le document de l'éditeur
        int currentLineCount = textPane.getDocument().getDefaultRootElement().getElementCount();

        // N'exécuter la mise à jour complète que si le nombre de lignes a changé
        if (currentLineCount == lastKnownLineCount && !getText().isEmpty()) {
            return; // Pas de changement de lignes, pas besoin de reconstruire le texte
        }
        lastKnownLineCount = currentLineCount;

        StringBuilder sb = new StringBuilder();
        // Construire la chaîne des numéros de ligne (1, 2, 3...)
        for (int i = 1; i <= currentLineCount; i++) {
            sb.append(i).append("\n");
        }
        // Définir le texte du JTextArea
        setText(sb.toString());
        
        // Ajuster la largeur préférée du JTextArea pour accommoder le plus grand numéro de ligne
        // (nombre de chiffres du max + padding)
        int maxLineNumberWidth = getFontMetrics(getFont()).stringWidth(String.valueOf(currentLineCount)) + getInsets().left + getInsets().right;
        setPreferredSize(new Dimension(maxLineNumberWidth + 5, getPreferredSize().height)); // Ajout d'un petit padding
    }
    
    @Override
    public void insertUpdate(DocumentEvent e) { 
        SwingUtilities.invokeLater(this::update); // Exécute sur l'EDT
    }
    
    @Override
    public void removeUpdate(DocumentEvent e) { 
        SwingUtilities.invokeLater(this::update); // Exécute sur l'EDT
    }
    
    @Override
    public void changedUpdate(DocumentEvent e) { /* Non utilisé pour un document en texte simple */ }
}
