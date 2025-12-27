package gui.panels;

import gui.theme.Theme;
import gui.theme.RoundedBorder;
import gui.components.ComponentFactory;
import gui.components.StyledButton;      
import gui.components.StyledTextField;   
import gui.dialogs.DialogFactory;
import mem.Memoire;
import cpu.CPU6809; 
import sim.SimulatorEngine; 

import javax.swing.*; 
import javax.swing.table.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.BorderLayout;
import java.awt.RenderingHints;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList; 
import java.util.Collections; 
import java.util.List;    
import java.util.Set;     

public class MemoryPanel extends JPanel implements PropertyChangeListener {
    
    private final JTable memTable;
    private final DefaultTableModel tableModel;
    private final StyledTextField addrField; 
    private final JSpinner lengthSpinner;
    private final JScrollPane scrollPane;
    
    private Memoire memoire;
    private CPU6809 cpu; 
    private SimulatorEngine simulatorEngine; 
    private int startAddress = 0x0000;
    private int displayLength = 256;
    
    public static final int BYTES_PER_ROW = 16; 
    private int highlightedAddress = -1;
    private int instructionSize = 0; 
    
    private boolean readOnly = false; 
    private String panelTitle = "Visualisation de la Mémoire"; 
    private JPanel headerPanel; 
    private JPanel controlsPanel; 

    private int memoryRangeStart = 0x0000; 
    private int memoryRangeEnd = 0xFFFF;   

    public MemoryPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Theme.FOND); 
        setBorder(new RoundedBorder(Theme.BORDURE, 1, 20, new Insets(15, 15, 15, 15)));
        
        headerPanel = createHeader(); 
        add(headerPanel, BorderLayout.NORTH);
        
        String[] columns = createColumns();
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                if (readOnly) return false; 
                return column >= 1 && column <= BYTES_PER_ROW; 
            }
            @Override
            public void setValueAt(Object aValue, int row, int column) {
                if (column >= 1 && column <= BYTES_PER_ROW) {
                    try {
                        String hexValue = ((String) aValue).trim();
                        int byteValue = Integer.parseInt(hexValue, 16);
                        if (byteValue >= 0 && byteValue <= 255) {
                            int address = startAddress + (row * BYTES_PER_ROW) + (column - 1);
                            if (address >= memoryRangeStart && address <= memoryRangeEnd) { 
                                memoire.ecrire(address, byteValue); 
                            } else {
                                DialogFactory.showError(null, "Adresse hors plage", 
                                    "L'adresse $" + String.format("%04X", address) + " n'est pas dans la plage de " + panelTitle + 
                                    " ($" + String.format("%04X", memoryRangeStart) + "-$" + String.format("%04X", memoryRangeEnd) + ").");
                            }
                        } else {
                            DialogFactory.showError(null, "Valeur invalide", "Veuillez entrer une valeur Hexadécimale entre 00 et FF.");
                        }
                    } catch (NumberFormatException e) { 
                        DialogFactory.showError(null, "Format invalide", "Valeur Hexadécimale invalide.");
                    } catch (RuntimeException e) { 
                        DialogFactory.showError(null, "Erreur d'écriture", e.getMessage());
                    }
                }
            }
        }; 

        memTable = new JTable(tableModel);
        setupTable(); 
        
        scrollPane = new JScrollPane(memTable);
        scrollPane.setBorder(new RoundedBorder(Theme.BORDURE, 1, 15, new Insets(5, 5, 5, 5)));
        scrollPane.setBackground(Theme.FOND);
        scrollPane.getViewport().setBackground(Theme.FOND);
        scrollPane.setColumnHeaderView(memTable.getTableHeader());
        scrollPane.setMinimumSize(new Dimension(400, 200));
        scrollPane.setPreferredSize(new Dimension(800, 400));
        
        add(scrollPane, BorderLayout.CENTER);
        
        addrField = StyledTextField.forInput(8); 
        addrField.setText(String.format("%04X", startAddress));
        
        lengthSpinner = new JSpinner(new SpinnerNumberModel(256, 16, 4096, 16));
        lengthSpinner.setEditor(new JSpinner.NumberEditor(lengthSpinner, "#")); 
        lengthSpinner.addChangeListener(e -> {
            displayLength = (Integer) lengthSpinner.getValue();
            refresh();
        });

        controlsPanel = createControls(); 
        add(controlsPanel, BorderLayout.SOUTH);
    }

    public void setTitle(String title) {
        this.panelTitle = title;
        JLabel titleLabel = (JLabel) ((BorderLayout) headerPanel.getLayout()).getLayoutComponent(BorderLayout.WEST);
        if (titleLabel != null) {
            titleLabel.setText(title);
        }
    }

    public void setMemoryRange(int start, int end) {
        this.memoryRangeStart = start & 0xFFFF;
        this.memoryRangeEnd = end & 0xFFFF;
    }
    
    private JPanel createHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false); 
        
        JLabel title = ComponentFactory.createTitleLabel(panelTitle); 
        panel.add(title, BorderLayout.WEST); 
        
        return panel;
    }
    
    private String[] createColumns() {
        String[] cols = new String[BYTES_PER_ROW + 2];
        cols[0] = "Adresse"; 
        for (int i = 0; i < BYTES_PER_ROW; i++) {
            cols[i + 1] = String.format("+%X", i); 
        }
        cols[BYTES_PER_ROW + 1] = "ASCII"; 
        return cols;
    }
    
    private void setupTable() {
        memTable.setFont(Theme.FONT_REGISTRE); 
        memTable.setRowHeight(24); 
        memTable.setGridColor(new Color(220, 220, 220)); 
        memTable.setSelectionBackground(Theme.HIGHLIGHT); 
        memTable.setSelectionForeground(Color.WHITE);
        memTable.setBackground(Theme.FOND);
        memTable.setForeground(Theme.TEXTE);
        memTable.setOpaque(true);
        
        memTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        memTable.getTableHeader().setReorderingAllowed(false);
        memTable.getTableHeader().setResizingAllowed(true);
        
        JTableHeader header = memTable.getTableHeader();
        header.setFont(Theme.fontUI(Font.BOLD, 11)); 
        header.setBackground(Theme.FOND_HEADER);
        header.setForeground(Theme.TEXTE);
        header.setOpaque(true);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 32));
        
        header.setDefaultRenderer(new InstructionSizeHeaderRenderer());
        
        DefaultTableCellRenderer cellRenderer = createCellRenderer();
        for (int i = 0; i < memTable.getColumnCount(); i++) {
            memTable.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
        
        configureColumnWidths();
    }
    
    private class InstructionSizeHeaderRenderer extends DefaultTableCellRenderer {
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
            
            label.setBackground(Theme.FOND_HEADER);
            label.setForeground(Theme.TEXTE);
            label.setFont(Theme.fontUI(Font.BOLD, 11));
            label.setHorizontalAlignment(JLabel.CENTER);
            label.setOpaque(true);
            
            // <<< MODIFICATION ICI: Condition pour le highlight du header (uniquement ROM)
            boolean isInstructionColumn = false;
            if (panelTitle.equals("ROM")) { 
                isInstructionColumn = isColumnInCurrentInstruction(column);
            }
            
            if (isInstructionColumn) {
                label.setBackground(Theme.HIGHLIGHT.darker());
                label.setForeground(Color.WHITE);
                label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 4, 1, Theme.HIGHLIGHT),
                    BorderFactory.createEmptyBorder(2, 4, 0, 4)
                ));
            } else {
                label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 1, Theme.BORDURE),
                    BorderFactory.createEmptyBorder(2, 4, 2, 4)
                ));
            }
            
            return label;
        }
        
        private boolean isColumnInCurrentInstruction(int column) {
            // Cette méthode est déjà appelée sous condition panelTitle.equals("ROM")
            // donc la première vérification n'est plus strictement nécessaire ici.
            // Cependant, on la garde pour la clarté et la robustesse.
            if (!panelTitle.equals("ROM") || highlightedAddress < startAddress || 
                highlightedAddress >= startAddress + displayLength ||
                instructionSize <= 0) {
                return false;
            }
            
            if (column < 1 || column > BYTES_PER_ROW) {
                return false;
            }
            
            int byteOffset = column - 1; 
            int highlightOffset = (highlightedAddress - startAddress) % BYTES_PER_ROW;
            
            return byteOffset >= highlightOffset && byteOffset < highlightOffset + instructionSize;
        }
    }
    
    private DefaultTableCellRenderer createCellRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
                
                setOpaque(true);
                
                Color bg = table.getBackground();
                Color fg = table.getForeground();
                
                if (row % 2 == 1) {
                    bg = new Color(
                        Math.max(0, bg.getRed() - 8),
                        Math.max(0, bg.getGreen() - 8),
                        Math.max(0, bg.getBlue() - 8)
                    );
                }
                
                // <<< MODIFICATION ICI: Condition pour le highlight des cellules (uniquement ROM)
                if (panelTitle.equals("ROM") && highlightedAddress != -1 && instructionSize > 0 &&
                    column >= 1 && column <= BYTES_PER_ROW) {
                    
                    int currentRowAddress = startAddress + (row * BYTES_PER_ROW);
                    int byteAddress = currentRowAddress + (column - 1);
                    
                    if (byteAddress >= highlightedAddress && 
                        byteAddress < highlightedAddress + instructionSize) {
                        bg = Theme.HIGHLIGHT;
                        fg = Color.WHITE;
                    }
                }
                
                if (isSelected) {
                    bg = Theme.HIGHLIGHT.brighter();
                    fg = Color.WHITE;
                }
                
                setBackground(bg);
                setForeground(fg);
                
                if (column == 0) {
                    setHorizontalAlignment(JLabel.CENTER);
                    setFont(Theme.FONT_REGISTRE.deriveFont(Font.BOLD));
                } else if (column == BYTES_PER_ROW + 1) {
                    setHorizontalAlignment(JLabel.LEFT);
                    setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                } else {
                    setHorizontalAlignment(JLabel.CENTER);
                    setFont(Theme.FONT_REGISTRE);
                }
                
                return c;
            }
        };
    }
    
    private void configureColumnWidths() {
        TableColumnModel columnModel = memTable.getColumnModel();
        
        columnModel.getColumn(0).setPreferredWidth(70);
        columnModel.getColumn(0).setMinWidth(60);
        columnModel.getColumn(0).setMaxWidth(100);
        
        for (int i = 1; i <= BYTES_PER_ROW; i++) {
            columnModel.getColumn(i).setPreferredWidth(28);
            columnModel.getColumn(i).setMinWidth(24);
            columnModel.getColumn(i).setMaxWidth(40);
        }
        
        columnModel.getColumn(BYTES_PER_ROW + 1).setPreferredWidth(140);
        columnModel.getColumn(BYTES_PER_ROW + 1).setMinWidth(100);
        columnModel.getColumn(BYTES_PER_ROW + 1).setMaxWidth(200);
    }
    
    private JPanel createControls() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        panel.setOpaque(false); 
        
        panel.add(ComponentFactory.createLabel("Adresse de début:"));
        panel.add(addrField); 
        
        panel.add(ComponentFactory.createLabel("Taille:"));
        panel.add(lengthSpinner); 
        
        StyledButton btnRefresh = new StyledButton("Actualiser", Theme.BTN_PRIMAIRE, Color.WHITE); 
        btnRefresh.addActionListener(e -> refresh());
        panel.add(btnRefresh);
        
        StyledButton btnGoto = new StyledButton("Aller à...", Theme.BTN_INFO, Color.WHITE); 
        btnGoto.addActionListener(e -> gotoAddress());
        panel.add(btnGoto);
        
        return panel;
    }
    
    public void setMemoire(Memoire mem) {
        if (this.memoire != null) {
            this.memoire.removePropertyChangeListener(this); 
        }
        this.memoire = mem;
        if (this.memoire != null) {
            this.memoire.addPropertyChangeListener(this); 
        }
        refresh();
    }

    public void setCPU(CPU6809 cpu) {
        if (this.cpu != null) {
            this.cpu.removePropertyChangeListener("PC", this); 
            this.cpu.removePropertyChangeListener("S", this);   
            this.cpu.removePropertyChangeListener("U", this);   
        }
        this.cpu = cpu;
        if (this.cpu != null) {
            if (panelTitle.equals("ROM")) { 
                this.cpu.addPropertyChangeListener("PC", this); 
            }
            if (panelTitle.equals("Pile")) {
                this.cpu.addPropertyChangeListener("S", this);
                this.cpu.addPropertyChangeListener("U", this);
            }
        }
    }

    public void setSimulatorEngine(SimulatorEngine simulatorEngine) {
        this.simulatorEngine = simulatorEngine;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        tableModel.fireTableStructureChanged(); 
    }

    public void setControlsVisible(boolean visible) {
        controlsPanel.setVisible(visible);
        headerPanel.setVisible(true); // Header toujours visible pour les onglets
    }
    
    public void refresh() {
        if (memoire == null) return;
        
        tableModel.setRowCount(0); 
        
        if (panelTitle.equals("Pile")) {
            Set<Integer> stackAddresses = memoire.getStackMemoryUsage();
            if (stackAddresses.isEmpty()) {
                startAddress = Memoire.RAM_END; 
                displayLength = BYTES_PER_ROW; 
                addrField.setText(String.format("$%04X", startAddress));
                lengthSpinner.setValue(displayLength);
                
                Object[] emptyRowData = new Object[BYTES_PER_ROW + 2];
                emptyRowData[0] = String.format("$%04X", startAddress);
                for(int i = 1; i <= BYTES_PER_ROW; i++) emptyRowData[i] = "";
                emptyRowData[BYTES_PER_ROW + 1] = "";
                tableModel.addRow(emptyRowData);
                
                configureColumnWidths();
                memTable.revalidate();
                memTable.repaint();
                memTable.getTableHeader().repaint();
                return; 
            }

            int minAddr = Collections.min(stackAddresses);
            int maxAddr = Collections.max(stackAddresses);

            // Ajuster startAddress et displayLength pour englober toutes les adresses utilisées
            startAddress = (minAddr / BYTES_PER_ROW) * BYTES_PER_ROW; // Aligne sur le début de la ligne
            displayLength = (maxAddr - startAddress + BYTES_PER_ROW) ; // Englobe toutes les adresses + au moins une ligne complète
            displayLength = Math.max(BYTES_PER_ROW, ((displayLength + BYTES_PER_ROW -1) / BYTES_PER_ROW) * BYTES_PER_ROW);
            
            // Assurer que la plage ne déborde pas de la RAM
            if (startAddress + displayLength > Memoire.RAM_END + 1) { 
                displayLength = (Memoire.RAM_END + 1) - startAddress;
                displayLength = Math.max(BYTES_PER_ROW, ((displayLength + BYTES_PER_ROW -1) / BYTES_PER_ROW) * BYTES_PER_ROW);
            }

            addrField.setText(String.format("$%04X", startAddress));
            lengthSpinner.setValue(displayLength);
        }
        
        int rows = (displayLength + BYTES_PER_ROW - 1) / BYTES_PER_ROW; 
        
        for (int row = 0; row < rows; row++) {
            int addr = startAddress + (row * BYTES_PER_ROW); 
            Object[] rowData = new Object[BYTES_PER_ROW + 2];
            
            rowData[0] = String.format("$%04X", addr);
            
            StringBuilder ascii = new StringBuilder();
            for (int i = 0; i < BYTES_PER_ROW; i++) {
                int byteAddr = addr + i;
                
                boolean isInPanelLogicalRange = (byteAddr >= memoryRangeStart && byteAddr <= memoryRangeEnd);
                boolean isStackView = panelTitle.equals("Pile");
                boolean isInGlobalMemoryRange = (byteAddr >= Memoire.RAM_START && byteAddr <= Memoire.ROM_END);


                // Pour la vue Pile, on affiche seulement si l'adresse est activement utilisée
                if (isStackView) { 
                    if (memoire.getStackMemoryUsage().contains(byteAddr)) {
                        int value = memoire.lire(byteAddr); 
                        rowData[i + 1] = String.format("%02X", value); 
                        
                        if (value >= 32 && value <= 126) { 
                            ascii.append((char) value);
                        } else {
                            ascii.append('.'); 
                        }
                    } else { // Adresse non utilisée par la pile, affiche vide
                        rowData[i + 1] = "";
                        ascii.append(' ');
                    }
                } else if (isInPanelLogicalRange && isInGlobalMemoryRange) { // RAM/ROM, si dans leur plage logique ET globale
                    int value = memoire.lire(byteAddr); 
                    rowData[i + 1] = String.format("%02X", value); 
                    
                    if (value >= 32 && value <= 126) { 
                        ascii.append((char) value);
                    } else {
                        ascii.append('.'); 
                    }
                }
                else { // Hors plage pour ce panneau, afficher vide
                    rowData[i + 1] = ""; 
                    ascii.append(' ');
                }
            }
            
            rowData[BYTES_PER_ROW + 1] = ascii.toString(); 
            tableModel.addRow(rowData); 
        }
        
        configureColumnWidths();
        memTable.revalidate();
        memTable.repaint();
        memTable.getTableHeader().repaint(); 
    }
    
    public void gotoAddress() {
        try {
            String addrStr = addrField.getText().trim();
            if (addrStr.startsWith("$")) {
                addrStr = addrStr.substring(1); 
            }
            int targetAddr = Integer.parseInt(addrStr, 16) & 0xFFFF;
            
            setStartAddress(targetAddr); 
        } catch (NumberFormatException ex) {
            DialogFactory.showError((JFrame) SwingUtilities.getWindowAncestor(this), 
                "Erreur de saisie", 
                "Adresse invalide: " + addrField.getText());
        }
    }
    
    public void setStartAddress(int address) {
        int newStartAddress = address & 0xFFFF;

        if (panelTitle.equals("Pile")) {
            if (newStartAddress < Memoire.RAM_START || newStartAddress > Memoire.RAM_END) {
                 DialogFactory.showError((JFrame) SwingUtilities.getWindowAncestor(this), "Adresse hors plage", 
                    "L'adresse $" + String.format("%04X", newStartAddress) + " n'est pas dans la plage RAM ($" + String.format("%04X", Memoire.RAM_START) + "-$" + String.format("%04X", Memoire.RAM_END) + ").");
                 return;
            }
        } else { 
            if (newStartAddress < memoryRangeStart || newStartAddress > memoryRangeEnd) {
                DialogFactory.showError((JFrame) SwingUtilities.getWindowAncestor(this), "Adresse hors plage", 
                    "L'adresse $" + String.format("%04X", newStartAddress) + " n'est pas dans la plage de " + panelTitle + 
                    " ($" + String.format("%04X", memoryRangeStart) + "-$" + String.format("%04X", memoryRangeEnd) + ").");
                return; 
            }
        }
        
        this.startAddress = newStartAddress;
        addrField.setText(String.format("$%04X", startAddress)); 
        refresh();
    }
    
    public void setDisplayLength(int length) {
        this.displayLength = length;
        lengthSpinner.setValue(length); 
        refresh();
    }
    
    /**
     * Définit l'adresse de début de l'affichage et tente de centrer la vue autour de cette adresse.
     * @param address L'adresse à afficher (et potentiellement centrer).
     * @param size La taille de l'instruction (pour le highlight ROM), ou 0 pour la pile.
     */
    public void highlightInstruction(int address, int size) {
        if (panelTitle.equals("ROM")) {
            this.highlightedAddress = address;
            this.instructionSize = size;
        } else {
            this.highlightedAddress = -1; 
            this.instructionSize = 0;
        }
        
        // Centrer la vue sur l'adresse si elle n'est pas déjà visible ou si c'est la pile (pour toujours recentrer)
        if (address < startAddress || address >= startAddress + displayLength || panelTitle.equals("Pile")) { 
             int newCenterAddress = (address - displayLength / 2);

             if (panelTitle.equals("Pile")) {
                newCenterAddress = Math.max(Memoire.RAM_START, newCenterAddress);
                newCenterAddress = Math.min(newCenterAddress, Memoire.RAM_END - displayLength + 1);
             } else { // RAM ou ROM
                newCenterAddress = Math.max(memoryRangeStart, newCenterAddress);
                newCenterAddress = Math.min(newCenterAddress, memoryRangeEnd - displayLength + 1);
             }
             
             this.startAddress = newCenterAddress & 0xFFFF;
             addrField.setText(String.format("$%04X", this.startAddress));
        }
        refresh(); 
    }
    
    public void highlightAddress(int address) {
        highlightInstruction(address, 1);
    }

    public void clearHighlight() {
        this.highlightedAddress = -1;
        this.instructionSize = 0;
        memTable.repaint();
        memTable.getTableHeader().repaint();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        SwingUtilities.invokeLater(() -> {
            String propertyName = evt.getPropertyName();
            if ("memoryChange".equals(propertyName)) { 
                Object changedAddressObj = evt.getOldValue(); 
                if (changedAddressObj instanceof Integer) {
                    int changedAddress = (Integer) changedAddressObj;
                    boolean isInPanelLogicalRange = (changedAddress >= memoryRangeStart && changedAddress <= memoryRangeEnd);
                    boolean isStackView = panelTitle.equals("Pile"); 
                    boolean isRamChange = (changedAddress >= Memoire.RAM_START && changedAddress <= Memoire.RAM_END);

                    if (isStackView && isRamChange) { 
                        refresh(); 
                    } else if (isInPanelLogicalRange) { 
                        refresh(); 
                    }
                } else { 
                    refresh(); 
                }
            } 
            else if ("PC".equals(propertyName)) { 
                int newPC = (Integer) evt.getNewValue();
                if (panelTitle.equals("ROM") && newPC >= Memoire.ROM_START && newPC <= Memoire.ROM_END) { 
                    int size = 1; 
                    if (simulatorEngine != null) { 
                        size = simulatorEngine.getInstructionSize(newPC);
                    }
                    highlightInstruction(newPC, size);
                } else {
                    clearHighlight();
                }
            }
            else if (("S".equals(propertyName) || "U".equals(propertyName)) && panelTitle.equals("Pile")) {
                int newPointerValue = (Integer) evt.getNewValue();
                highlightInstruction(newPointerValue, 0); 
            }
        });
    }
        
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(getBackground()); 
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20); 
        g2d.dispose();
        super.paintComponent(g); 
    }
    
    @Override
    public void doLayout() {
        super.doLayout();
        if (memTable != null && memTable.getColumnCount() > 0) {
            configureColumnWidths();
        }
    }
}
