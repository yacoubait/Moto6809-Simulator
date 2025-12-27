package gui.dialogs;

import gui.components.ComponentFactory;
import gui.components.StyledButton;
import gui.components.StyledTextField; 
import gui.theme.Theme;
import gui.theme.RoundedBorder;
import asm.TableOpcodes; 

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InstructionSetDialog extends BaseDialog {

    private JTable instrTable;
    private JTextField searchField;

    public InstructionSetDialog(JFrame parent) {
        super(parent, "Jeu d'Instructions 6809", 800, 600);
        initContent();
        initFooter();
        // NOUVEVEAU: Assurer que les panneaux sont bien validés et repeints
        SwingUtilities.invokeLater(() -> {
            rootPanel.revalidate();
            rootPanel.repaint();
        });
    }

    @Override
    protected void initContent() {
        contentPanel.setLayout(new BorderLayout(10, 10));

        instrTable = createInstructionTable();

        JScrollPane scrollPane = new JScrollPane(instrTable);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Theme.FOND_CLAIR); 

        contentPanel.add(createSearchBar(), BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    protected void initFooter() {
        addButton("Fermer", Theme.BTN_NEUTRE, this::dispose);
    }

    private JPanel createSearchBar() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JLabel label = ComponentFactory.createLabel("Rechercher :");
        searchField = StyledTextField.forInput(20); 
        searchField.setHorizontalAlignment(JTextField.LEFT);
        searchField.setBorder(new RoundedBorder(Theme.BORDURE, 1, 12, new Insets(6, 10, 6, 10)));

        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String query = searchField.getText().toLowerCase();
                TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) instrTable.getRowSorter();

                if (query.isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + query));
                }
            }
        });

        panel.add(label, BorderLayout.WEST);
        panel.add(searchField, BorderLayout.CENTER);

        return panel;
    }

    private JTable createInstructionTable() {
        String[] columns = {"Mnémonique", "Mode d'Adressage", "Opcode", "Bytes"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        Set<String> mnemonics = TableOpcodes.getAllMnemonics();
        List<String> sortedMnemonics = new ArrayList<>(mnemonics);
        Collections.sort(sortedMnemonics); 

        for (String mnemonic : sortedMnemonics) {
            Map<asm.ModeAdressage, Integer> modes = TableOpcodes.OPCODES.get(mnemonic); 
            
            if (TableOpcodes.INHERENT_OPCODES.containsKey(mnemonic)) {
                int opcode = TableOpcodes.INHERENT_OPCODES.get(mnemonic);
                model.addRow(new Object[]{
                    mnemonic,
                    asm.ModeAdressage.INHERENT.toString(),
                    String.format("$%02X", opcode),
                    (opcode > 0xFF ? 2 : 1) 
                });
            }

            if (modes != null) {
                for (Map.Entry<asm.ModeAdressage, Integer> entry : modes.entrySet()) {
                    asm.ModeAdressage mode = entry.getKey();
                    int opcode = entry.getValue();
                    
                    int bytes = (opcode > 0xFF ? 2 : 1); 
                    
                    switch (mode) {
                        case IMMEDIAT:
                            bytes += TableOpcodes.est16bits(mnemonic) ? 2 : 1;
                            break;
                        case DIRECT:
                            bytes += 1;
                            break;
                        case ETENDU:
                            bytes += 2;
                            break;
                        case INDEXE:
                            bytes += 1; 
                            break;
                        case RELATIF:
                            bytes += (opcode > 0xFF ? 2 : 1); 
                            break;
                        case REGISTRE: 
                            bytes += 1; 
                            break;
                        default:
                            break;
                    }

                    model.addRow(new Object[]{
                        mnemonic,
                        mode.toString(),
                        String.format("$%0" + (opcode > 0xFF ? 4 : 2) + "X", opcode), 
                        bytes
                    });
                }
            }
        }
        
        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.setFont(Theme.fontMono(Font.PLAIN, 12));
        table.getTableHeader().setFont(Theme.fontUI(Font.BOLD, 13));
        table.getTableHeader().setBackground(Theme.FOND_HEADER);
        table.setFillsViewportHeight(true);
        table.setGridColor(new Color(230, 230, 230));
        table.setSelectionBackground(Theme.HIGHLIGHT);
        table.setSelectionForeground(Color.WHITE);
        table.setOpaque(true);
        table.setBackground(Theme.FOND_CLAIR);
        table.setForeground(Theme.TEXTE);

        table.setAutoCreateRowSorter(true);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); 
        table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer); 
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer); 
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); 

        table.getColumnModel().getColumn(0).setPreferredWidth(120);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(60);

        return table;
    }
}
