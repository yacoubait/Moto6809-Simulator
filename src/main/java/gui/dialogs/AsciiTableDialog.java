package gui.dialogs;

import gui.components.ComponentFactory;
import gui.components.StyledButton;
import gui.components.StyledTextField;
import gui.theme.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Vector;

public class AsciiTableDialog extends BaseDialog {

    private static final String[] COLUMNS = {"Dec", "Hex", "Char", "Description"};
    private JTable table;

    public AsciiTableDialog(JFrame parent) {
        super(parent, "Table ASCII", 550, 450);
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
        table = createAsciiTable();

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);

        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.add(createSearchBar(table), BorderLayout.NORTH);
    }

    @Override
    protected void initFooter() {
        addButton("Fermer", Theme.BTN_NEUTRE, this::dispose);
    }

    private JPanel createSearchBar(JTable table) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JLabel label = ComponentFactory.createLabel("Rechercher :");
        JTextField searchField = StyledTextField.forInput(15);
        searchField.setHorizontalAlignment(JTextField.LEFT);
        searchField.setBorder(new RoundedBorder(Theme.BORDURE, 1, 12, new Insets(6, 10, 6, 10)));

        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String query = searchField.getText().toLowerCase();
                TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) table.getRowSorter();

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

    private JTable createAsciiTable() {
        Object[][] data = new Object[128][4]; 
        for (int i = 0; i < 128; i++) {
            data[i][0] = i; 
            data[i][1] = String.format("%02X", i); 
            if (i >= 32 && i <= 126) {
                data[i][2] = Character.toString((char) i); 
            } else {
                data[i][2] = ""; 
            }
            data[i][3] = getDescription(i);
        }
        
        DefaultTableModel model = new DefaultTableModel(data, COLUMNS) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; 
            }
        };

        JTable table = new JTable(model); 
        table.setRowHeight(24);
        table.setFont(Theme.fontMono(Font.PLAIN, 13));
        table.getTableHeader().setFont(Theme.fontUI(Font.BOLD, 13));
        table.getTableHeader().setBackground(Theme.FOND_HEADER);
        table.setFillsViewportHeight(true);
        table.setGridColor(new Color(230, 230, 230));
        table.setSelectionBackground(Theme.HIGHLIGHT);
        table.setSelectionForeground(Color.WHITE);
        table.setOpaque(true);
        table.setBackground(Color.WHITE);
        table.setForeground(Color.BLACK);

        table.setAutoCreateRowSorter(true);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); 
        table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer); 
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer); 

        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(50);
        table.getColumnModel().getColumn(2).setPreferredWidth(60);
        table.getColumnModel().getColumn(3).setPreferredWidth(200);
        
        return table;
    }

    private String getDescription(int code) {
        switch (code) {
            case 0: return "NUL (Null)"; case 1: return "SOH (Start of Heading)"; case 2: return "STX (Start of Text)";
            case 3: return "ETX (End of Text)"; case 4: return "EOT (End of Transmission)"; case 5: return "ENQ (Enquiry)";
            case 6: return "ACK (Acknowledge)"; case 7: return "BEL (Bell)"; case 8: return "BS (Backspace)";
            case 9: return "HT (Horizontal Tab)"; case 10: return "LF (Line Feed)"; case 11: return "VT (Vertical Tab)";
            case 12: return "FF (Form Feed)"; case 13: return "CR (Carriage Return)"; case 14: return "SO (Shift Out)";
            case 15: return "SI (Shift In)"; case 16: return "DLE (Data Link Escape)"; case 17: return "DC1 (Device Control 1)";
            case 18: return "DC2 (Device Control 2)"; case 19: return "DC3 (Device Control 3)"; case 20: return "DC4 (Device Control 4)";
            case 21: return "NAK (Negative Acknowledge)"; case 22: return "SYN (Synchronous Idle)"; case 23: return "ETB (End of Trans. Block)";
            case 24: return "CAN (Cancel)"; case 25: return "EM (End of Medium)"; case 26: return "SUB (Substitute)";
            case 27: return "ESC (Escape)"; case 28: return "FS (File Separator)"; case 29: return "GS (Group Separator)";
            case 30: return "RS (Record Separator)"; case 31: return "US (Unit Separator)"; case 32: return "Space";
            case 127: return "DEL (Delete)";
            default:
                if (code >= 48 && code <= 57) return "Digit";
                if (code >= 65 && code <= 90) return "Uppercase Letter";
                if (code >= 97 && code <= 122) return "Lowercase Letter";
                if ((code >= 33 && code <= 47) || (code >= 58 && code <= 64) ||
                    (code >= 91 && code <= 96) || (code >= 123 && code <= 126)) return "Symbol/Punctuation";
                return "Control Character";
        }
    }
}
