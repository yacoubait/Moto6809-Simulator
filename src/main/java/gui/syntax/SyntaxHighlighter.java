package gui.syntax;

import gui.theme.Theme; 
import asm.TableOpcodes; // Pour obtenir la liste des mnémoniques et directives

import javax.swing.*;
import javax.swing.text.*; 
import java.awt.*;
import java.util.Collections; // <-- Import manquant ajouté ici
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxHighlighter {
    
    private final StyledDocument doc; 
    
    // Styles pour les différentes catégories de syntaxe
    private final Style styleNormal;
    private final Style styleMnemonic;
    private final Style styleOperand;
    private final Style styleComment;
    private final Style styleLabel;
    private final Style styleDirective;
    private final Style styleNumber; // Style pour les nombres
    
    // Listes de mots-clés pour la coloration syntaxique (obtenues de TableOpcodes)
    private static final Set<String> MNEMONICS = new HashSet<>();
    private static final Set<String> DIRECTIVES = new HashSet<>();
    
    // Patterns regex pour une détection plus fine
    private static final Pattern COMMENT_PATTERN = Pattern.compile(";.*");
    private static final Pattern LABEL_PATTERN = Pattern.compile("^\\s*([A-Za-z_][A-Za-z0-9_]*):");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\$[0-9A-Fa-f]+)|(#[0-9A-Fa-f]+h?)|(%[01]+)|([0-9]+)");

    public SyntaxHighlighter(JTextPane textPane) {
        this.doc = textPane.getStyledDocument();
        
        // Initialiser les sets de mnémoniques et directives depuis TableOpcodes
        MNEMONICS.addAll(TableOpcodes.getAllMnemonics()); // Ligne originale incorrecte
       // MNEMONICS.addAll(new HashSet<>(TableOpcodes.getAllMnemonics())); // Créer un HashSet mutable à partir du Set immuable
        // Les directives doivent être ajoutées manuellement si TableOpcodes ne les gère pas directement
        // Pour cet exemple, je vais ajouter les directives reconnues par Assembleur.java
        Collections.addAll(DIRECTIVES, "ORG", "EQU", "FCC", "FCS", "FDB", "FCB", "FILL", "RMB", "SETDP", "END");
        
        StyleContext sc = StyleContext.getDefaultStyleContext();
        
        styleNormal = sc.addStyle("normal", null);
        StyleConstants.setForeground(styleNormal, Theme.TEXTE);
        
        styleMnemonic = sc.addStyle("mnemonic", null);
        StyleConstants.setForeground(styleMnemonic, Theme.SYNTAX_MNEMONIC);
        StyleConstants.setBold(styleMnemonic, true);
        
        styleOperand = sc.addStyle("operand", null);
        StyleConstants.setForeground(styleOperand, Theme.SYNTAX_OPERAND);
        
        styleComment = sc.addStyle("comment", null);
        StyleConstants.setForeground(styleComment, Theme.SYNTAX_COMMENT);
        StyleConstants.setItalic(styleComment, true);
        
        styleLabel = sc.addStyle("label", null);
        StyleConstants.setForeground(styleLabel, Theme.SYNTAX_LABEL);
        
        styleDirective = sc.addStyle("directive", null);
        StyleConstants.setForeground(styleDirective, Theme.SYNTAX_DIRECTIVE); 
        StyleConstants.setBold(styleDirective, true);

        styleNumber = sc.addStyle("number", null);
        StyleConstants.setForeground(styleNumber, Theme.SYNTAX_NUMBER); 
        
        // Ajouter un DocumentListener pour déclencher la coloration à chaque modification
        doc.addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { highlight(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { highlight(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {} 
        });
    }
    
    /**
     * Applique la coloration syntaxique à tout le document.
     * Cette méthode doit être appelée après chaque modification du texte.
     */
    public void highlight() {
        SwingUtilities.invokeLater(() -> {
            try {
                String text = doc.getText(0, doc.getLength());
                doc.setCharacterAttributes(0, text.length(), styleNormal, true); // Réinitialise tout en style normal

                String[] lines = text.split("\n");
                int pos = 0; 
                
                for (String line : lines) {
                    highlightLine(line, pos);
                    pos += line.length() + 1; 
                }
            } catch (BadLocationException e) {
                System.err.println("Erreur de localisation du document lors de la coloration: " + e.getMessage());
            }
        });
    }

    /**
     * Applique la coloration syntaxique à une plage spécifique du document.
     * Utile pour réappliquer la coloration après qu'une surbrillance de PC/breakpoint ait été effacée.
     * @param offset Le début de la plage.
     * @param length La longueur de la plage.
     * @throws BadLocationException si la plage est invalide.
     */
    public void highlightLineRange(int offset, int length) throws BadLocationException {
        String text = doc.getText(offset, length);
        doc.setCharacterAttributes(offset, length, styleNormal, true); // Réinitialise la plage

        String[] lines = text.split("\n");
        int currentPos = offset;
        for (String line : lines) {
            highlightLine(line, currentPos);
            currentPos += line.length() + 1;
        }
    }
    
    /**
     * Applique la coloration syntaxique à une seule ligne de code.
     * @param line Le texte de la ligne.
     * @param pos La position de début de cette ligne dans le document global.
     */
    private void highlightLine(String line, int pos) {
        String trimmedLine = line.trim();
        if (trimmedLine.isEmpty()) return;

        int lineStartOffsetInDoc = pos + line.indexOf(trimmedLine.charAt(0)); // Offset des espaces en début de ligne

        // --- 1. Commentaires ---
        Matcher commentMatcher = COMMENT_PATTERN.matcher(line);
        if (commentMatcher.find()) { // Cherche le commentaire dans la ligne originale pour son index
            int commentStart = pos + commentMatcher.start();
            doc.setCharacterAttributes(commentStart, line.length() - commentMatcher.start(), styleComment, true);
            // Si le commentaire est au début de la ligne, le reste n'a pas besoin d'être traité
            trimmedLine = line.substring(0, commentMatcher.start()).trim();
            if (trimmedLine.isEmpty()) return;
        }

        // --- 2. Labels ---
        Matcher labelMatcher = LABEL_PATTERN.matcher(line);
        if (labelMatcher.find()) { // Cherche le label dans la ligne originale
            String label = labelMatcher.group(1);
            int labelStart = pos + labelMatcher.start(1);
            int labelEnd = pos + labelMatcher.end(0); // Inclut le ':'
            doc.setCharacterAttributes(labelStart, labelEnd - labelStart, styleLabel, true);
            // Mettre à jour la partie de la ligne à analyser
            trimmedLine = line.substring(labelMatcher.end(0)).trim();
            lineStartOffsetInDoc = pos + line.indexOf(trimmedLine, labelMatcher.end(0));
        }

        // --- 3. Mnémonique / Directive ---
        String[] parts = trimmedLine.split("\\s+", 2);
        if (parts.length > 0 && !parts[0].isEmpty()) {
            String firstWord = parts[0].toUpperCase();
            int wordStartInDoc = lineStartOffsetInDoc; // Position de la première partie dans le document

            if (DIRECTIVES.contains(firstWord)) {
                doc.setCharacterAttributes(wordStartInDoc, firstWord.length(), styleDirective, true);
            } else if (MNEMONICS.contains(firstWord)) {
                doc.setCharacterAttributes(wordStartInDoc, firstWord.length(), styleMnemonic, true);
            }

            // --- 4. Opérande ---
            if (parts.length > 1) {
                String operandPart = parts[1];
                int operandStartInDoc = wordStartInDoc + firstWord.length();
                // Ajuster l'offset si des espaces blancs se trouvent entre le mnémonique et l'opérande
                operandStartInDoc += trimmedLine.substring(firstWord.length()).indexOf(operandPart);

                doc.setCharacterAttributes(operandStartInDoc, operandPart.length(), styleOperand, true);

                // --- 5. Nombres dans l'opérande (Hex, Dec, Bin) ---
                highlightNumbersInOperand(operandPart, operandStartInDoc);
            }
        }
    }

    /**
     * Met en surbrillance les nombres hexadécimaux, décimaux et binaires dans une chaîne d'opérande.
     * @param operand La chaîne de l'opérande.
     * @param startOffset La position de début de l'opérande dans le document.
     */
    private void highlightNumbersInOperand(String operand, int startOffset) {
        Matcher m = NUMBER_PATTERN.matcher(operand);

        while (m.find()) {
            doc.setCharacterAttributes(startOffset + m.start(), m.end() - m.start(), styleNumber, true);
        }
    }
}
