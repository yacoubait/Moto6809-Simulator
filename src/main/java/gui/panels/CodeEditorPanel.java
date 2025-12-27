package gui.panels;

import gui.theme.Theme; 
import gui.theme.RoundedBorder;
import gui.components.ComponentFactory;
import gui.components.StyledButton;
import gui.components.LineNumberView;
import gui.syntax.SyntaxHighlighter;
import sim.SimulatorEngine;
import cpu.CPU6809; // <-- Assurez-vous que cet import est bien présent et correct !

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Éditeur de code assembleur — version corrigée.
 * - Non-générique (utilise cpu.CPU6809)
 * - Inscription/désinscription correcte aux PropertyChangeListeners du CPU
 * - DocumentListener ajouté APRÈS la création du SyntaxHighlighter
 */
public class CodeEditorPanel extends JPanel implements PropertyChangeListener {

    private JTextPane codeArea;
    private LineNumberView lineNumberView;
    private SyntaxHighlighter highlighter;
    private Runnable onCloseAction;

    private CPU6809 cpu; // Le type CPU6809 est utilisé ici
    private SimulatorEngine simulatorEngine;
    private Map<Integer, Integer> addressToLineMap;

    private int highlightedPCLine = -1;
    private final Set<Integer> breakpointLines = new HashSet<>();

    private Style stylePCLine;
    private Style styleBreakpointLine;
    private Style stylePCBreakpointLine;
    private StyleContext sc;

    private final UndoManager undoManager;

    public CodeEditorPanel() {
        setLayout(new BorderLayout(10, 10));
        setPreferredSize(new Dimension(400, 0));
        setOpaque(false);
        setBackground(Theme.FOND_CLAIR);
        setBorder(new RoundedBorder(Theme.BORDURE, 1, 20, new Insets(18, 18, 33, 18)));

        // Styles de surbrillance
        sc = StyleContext.getDefaultStyleContext();
        stylePCLine = sc.addStyle("pcLine", null);
        StyleConstants.setBackground(stylePCLine, Theme.HIGHLIGHT.darker().darker());
        StyleConstants.setForeground(stylePCLine, Color.WHITE);

        styleBreakpointLine = sc.addStyle("breakpointLine", null);
        StyleConstants.setBackground(styleBreakpointLine, Color.RED.darker().darker());
        StyleConstants.setForeground(styleBreakpointLine, Color.WHITE);

        stylePCBreakpointLine = sc.addStyle("pcBreakpointLine", null);
        StyleConstants.setBackground(stylePCBreakpointLine, Color.ORANGE.darker());
        StyleConstants.setForeground(stylePCBreakpointLine, Color.BLACK);

        add(createHeader(), BorderLayout.NORTH);

        // Créer la zone de texte, puis le highlighter et la lineNumberView
        codeArea = createCodeAreaWithoutDocListener(); // ne pas ajouter le DocumentListener ici
        highlighter = new SyntaxHighlighter(codeArea);
        lineNumberView = new LineNumberView(codeArea);

        // Maintenant que highlighter existe, on peut ajouter le DocumentListener qui l'utilise
        installDocumentListener();

        JScrollPane scrollPane = createScrollPane();
        add(scrollPane, BorderLayout.CENTER);

        // Click sur la colonne des numéros de ligne -> toggle breakpoint via simulatorEngine
        lineNumberView.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (simulatorEngine != null && addressToLineMap != null && !addressToLineMap.isEmpty()) {
                    int viewToModelOffset = lineNumberView.viewToModel(e.getPoint());
                    if (viewToModelOffset < 0) return;

                    int lineNumberClicked = lineNumberView.getDocument().getDefaultRootElement()
                            .getElementIndex(viewToModelOffset) + 1;
                    int address = getAddressForLine(lineNumberClicked);
                    if (address != -1) {
                        simulatorEngine.toggleBreakpoint(address);
                    }
                }
            }
        });

        // Undo manager
        undoManager = new UndoManager();
        codeArea.getDocument().addUndoableEditListener(new UndoableEditListener() {
            @Override
            public void undoableEditHappened(UndoableEditEvent e) {
                undoManager.addEdit(e.getEdit());
            }
        });

        // Key bindings (undo / redo) — WHEN_FOCUSED pour que ça n'affecte que l'éditeur
        InputMap inputMap = codeArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = codeArea.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo");
        actionMap.put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { undo(); }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "redo");
        actionMap.put("redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { redo(); }
        });
    }

    // ---------------- création UI ----------------

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel titre = ComponentFactory.createTitleLabel("Éditeur de code assembleur");
        
        // MODIFICATION ICI: Créer un bouton de fermeture stylisé pour le panneau
        StyledButton btnFermer = new StyledButton("", Theme.FOND_CLAIR, new Color(100, 100, 100)); // Texte vide
        btnFermer.setRadius(10);
        btnFermer.setFont(Theme.fontUI(Font.BOLD, 13));
        btnFermer.setPreferredSize(new Dimension(30, 28)); // Taille cohérente avec les boutons de dialogue
        btnFermer.setColors(Theme.FOND_CLAIR, new Color(232, 17, 35), new Color(200, 15, 30)); // Couleurs de hover/press
        btnFermer.setForeground(new Color(100, 100, 100)); // Couleur initiale de la croix si elle était là
        btnFermer.setBorder(new RoundedBorder(Theme.BORDURE, 1, 10, new Insets(5, 5, 5, 5))); // Bordure cohérente

        btnFermer.addActionListener(e -> { 
            if (onCloseAction != null) onCloseAction.run(); 
        });

        header.add(titre, BorderLayout.WEST);
        header.add(btnFermer, BorderLayout.EAST);
        return header;
    }

    /** Crée le JTextPane mais n'installe pas le DocumentListener (sera installé après création du highlighter). */
    private JTextPane createCodeAreaWithoutDocListener() {
        JTextPane pane = new JTextPane();
        pane.setFont(Theme.FONT_CODE);
        pane.setBackground(Theme.FOND);
        pane.setForeground(Theme.TEXTE);
        pane.setCaretColor(Theme.TEXTE);
        pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pane.setEditorKit(new StyledEditorKit());
        return pane;
    }

    private void installDocumentListener() {
        Document doc = codeArea.getDocument();
        doc.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    highlighter.highlight();
                    lineNumberView.update();
                });
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    highlighter.highlight();
                    lineNumberView.update();
                });
            }

            @Override
            public void changedUpdate(DocumentEvent e) { /* no-op for plain text */ }
        });
    }

    private JScrollPane createScrollPane() {
        JScrollPane scrollPane = new JScrollPane(codeArea);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(new RoundedBorder(Theme.BORDURE, 1, 15, new Insets(6, 6, 6, 6)));
        scrollPane.setRowHeaderView(lineNumberView);
        return scrollPane;
    }

    // ---------------- API publique ----------------

    public String getCode() { return codeArea.getText(); }

    public void setCode(String code) {
        undoManager.discardAllEdits();
        codeArea.setText(code);
    }

    public void clear() {
        codeArea.setText("");
        clearHighlight();
        clearAllBreakpointHighlights();
        undoManager.discardAllEdits();
    }

    public boolean isEmpty() { return codeArea.getText().trim().isEmpty(); }

    public void setOnCloseAction(Runnable action) { this.onCloseAction = action; }

    /**
     * (Important) Attacher le CPU à l'éditeur pour écouter les changements de PC.
     * On désinscrit l'ancien listener (si présent) puis on inscrit le nouveau.
     */
    public void setCPU(CPU6809 cpu) {
        if (this.cpu != null) {
            // désinscrire l'ancien listener
            this.cpu.removePropertyChangeListener(this);
        }
        this.cpu = cpu;
        if (this.cpu != null) {
            // écouter uniquement la propriété "PC" (si CPU6809 la publie)
            this.cpu.addPropertyChangeListener("PC", this);
        }
    }

    public void setSimulatorEngine(SimulatorEngine engine) {
        this.simulatorEngine = engine;
    }

    public void setAddressToLineMap(Map<Integer, Integer> addressToLineMap) {
        this.addressToLineMap = addressToLineMap;
        clearAllBreakpointHighlights();
        if (simulatorEngine != null && simulatorEngine.getStepExecutor() != null) {
            for (int bpAddress : simulatorEngine.getStepExecutor().getBreakpoints()) {
                toggleBreakpointHighlight(bpAddress, true);
            }
        }
    }

    // --------- mapping adresse <-> ligne -----------
    private int getAddressForLine(int lineNumber) {
        if (addressToLineMap == null || addressToLineMap.isEmpty()) return -1;
        for (Map.Entry<Integer, Integer> entry : addressToLineMap.entrySet()) {
            if (entry.getValue() == lineNumber) return entry.getKey();
        }
        return -1;
    }

    private int getLineForAddress(int address) {
        if (addressToLineMap == null || addressToLineMap.isEmpty()) return -1;
        Integer line = addressToLineMap.get(address);
        if (line != null) return line;
        int currentLine = -1, currentAddress = -1;
        for (Map.Entry<Integer, Integer> entry : addressToLineMap.entrySet()) {
            if (entry.getKey() <= address && entry.getKey() > currentAddress) {
                currentAddress = entry.getKey();
                currentLine = entry.getValue();
            }
        }
        return currentLine;
    }

    // --------- surbrillance / breakpoints ----------
    public void highlightPCLine(int pcAddress) {
        clearHighlight();
        int lineNumber = getLineForAddress(pcAddress);
        if (lineNumber != -1) {
            highlightLine(lineNumber);
            this.highlightedPCLine = lineNumber;
        }
    }

    public void clearHighlight() {
        if (highlightedPCLine != -1) {
            try {
                Element root = codeArea.getDocument().getDefaultRootElement();
                Element lineElement = root.getElement(highlightedPCLine - 1);
                if (lineElement != null) {
                    highlighter.highlightLineRange(lineElement.getStartOffset(),
                            lineElement.getEndOffset() - lineElement.getStartOffset());
                }
            } catch (BadLocationException e) {
                System.err.println("Erreur effacement surbrillance PC: " + e.getMessage());
            } finally {
                highlightedPCLine = -1;
            }
        }
    }

    public void highlightLine(int lineNumber) {
        if (lineNumber < 1 || lineNumber > codeArea.getDocument().getDefaultRootElement().getElementCount()) return;
        try {
            Element root = codeArea.getDocument().getDefaultRootElement();
            Element lineElement = root.getElement(lineNumber - 1);
            int start = lineElement.getStartOffset();
            int length = lineElement.getEndOffset() - start;

            Style targetStyle = stylePCLine;
            if (breakpointLines.contains(lineNumber)) targetStyle = stylePCBreakpointLine;
            codeArea.getStyledDocument().setCharacterAttributes(start, length, targetStyle, true);

            Rectangle rect = codeArea.modelToView(start);
            if (rect != null) codeArea.scrollRectToVisible(rect);
        } catch (BadLocationException e) {
            System.err.println("Erreur surbrillance ligne " + lineNumber + ": " + e.getMessage());
        }
    }

    public void toggleBreakpointHighlight(int address, boolean set) {
        int lineNumber = getLineForAddress(address);
        if (lineNumber != -1) {
            if (set) {
                breakpointLines.add(lineNumber);
                if (highlightedPCLine == lineNumber) highlightLine(lineNumber);
                else applyBreakpointHighlight(lineNumber, true);
            } else {
                breakpointLines.remove(lineNumber);
                if (highlightedPCLine == lineNumber) highlightLine(lineNumber);
                else applyBreakpointHighlight(lineNumber, false);
            }
            lineNumberView.repaint();
        }
    }

    private void applyBreakpointHighlight(int lineNumber, boolean set) {
        if (lineNumber < 1 || lineNumber > codeArea.getDocument().getDefaultRootElement().getElementCount()) return;
        try {
            Element root = codeArea.getDocument().getDefaultRootElement();
            Element lineElement = root.getElement(lineNumber - 1);
            int start = lineElement.getStartOffset();
            int length = lineElement.getEndOffset() - start;
            if (set) codeArea.getStyledDocument().setCharacterAttributes(start, length, styleBreakpointLine, true);
            else highlighter.highlightLineRange(start, length);
        } catch (BadLocationException e) {
            System.err.println("Erreur breakpoint highlight: " + e.getMessage());
        }
    }

    public void clearAllBreakpointHighlights() {
        for (int ln : breakpointLines) {
            try {
                Element root = codeArea.getDocument().getDefaultRootElement();
                Element lineElement = root.getElement(ln - 1);
                highlighter.highlightLineRange(lineElement.getStartOffset(),
                        lineElement.getEndOffset() - lineElement.getStartOffset());
            } catch (BadLocationException e) {
                System.err.println("Erreur clear breakpoint highlight: " + e.getMessage());
            }
        }
        breakpointLines.clear();
        lineNumberView.repaint();
    }

    // -------------- undo / redo ----------------
    public void undo() {
        try {
            if (undoManager.canUndo()) undoManager.undo();
        } catch (CannotUndoException ex) {
            System.err.println("Impossible d'annuler: " + ex.getMessage());
        }
    }

    public void redo() {
        try {
            if (undoManager.canRedo()) undoManager.redo();
        } catch (CannotRedoException ex) {
            System.err.println("Impossible de rétablir: " + ex.getMessage());
        }
    }

    // ---------------- PropertyChangeListener ----------------
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("PC".equals(evt.getPropertyName())) {
            SwingUtilities.invokeLater(() -> {
                Object nv = evt.getNewValue();
                if (nv instanceof Integer) {
                    highlightPCLine((Integer) nv);
                } else if (nv instanceof Number) {
                    highlightPCLine(((Number) nv).intValue());
                }
            });
        }
    }
}
