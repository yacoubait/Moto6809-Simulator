package gui.dialogs;

import gui.theme.Theme;
import gui.theme.RoundedBorder;
import gui.components.ComponentFactory;
import gui.components.StyledButton;
import gui.components.StyledTextField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class HexDecConverterDialog extends BaseDialog {

    private StyledTextField hexField;  
    private StyledTextField decField;
    private StyledTextField binField;

    public HexDecConverterDialog(JFrame parent) {
        super(parent, "Convertisseur Hex ↔ Dec ↔ Bin", 400, 320);
        initContent(); 
        initFooter();
        setupListeners(); 
        // NOUVEVEAU: Assurer que les panneaux sont bien validés et repeints
        SwingUtilities.invokeLater(() -> {
            rootPanel.revalidate();
            rootPanel.repaint();
        });
    }

    @Override
    protected void initContent() {
        hexField = new StyledTextField(12);
        decField = new StyledTextField(12);
        binField = new StyledTextField(18);
        binField.setEditable(false);

        contentPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        contentPanel.add(ComponentFactory.createLabel("Hexadécimal :"), gbc);
        gbc.gridx = 1;
        contentPanel.add(hexField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        contentPanel.add(ComponentFactory.createLabel("Décimal :"), gbc);
        gbc.gridx = 1;
        contentPanel.add(decField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        contentPanel.add(ComponentFactory.createLabel("Binaire :"), gbc);
        gbc.gridx = 1;
        contentPanel.add(binField, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.setOpaque(false);

        StyledButton btnHexToDec = new StyledButton("Hex → Dec", Theme.BTN_INFO, Color.WHITE);
        StyledButton btnDecToHex = new StyledButton("Dec → Hex", Theme.BTN_PRIMAIRE, Color.WHITE);
        StyledButton btnClear = new StyledButton("Effacer", Theme.BTN_NEUTRE.brighter(), Color.WHITE);

        btnHexToDec.addActionListener(e -> convertFromHex());
        btnDecToHex.addActionListener(e -> convertFromDec());
        btnClear.addActionListener(e -> clearAll());

        btnPanel.add(btnHexToDec);
        btnPanel.add(btnDecToHex);
        btnPanel.add(btnClear);

        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 8, 8, 8);
        contentPanel.add(btnPanel, gbc);
    }

    @Override
    protected void initFooter() {
        addButton("Fermer", Theme.BTN_NEUTRE, this::dispose);
    }

    private void setupListeners() {
        hexField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (hexField.getText().trim().isEmpty()) {
                    clearOtherFields(hexField);
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    convertFromHex();
                }
            }
        });
        
        decField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (decField.getText().trim().isEmpty()) {
                    clearOtherFields(decField);
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    convertFromDec();
                }
            }
        });
    }

    private void convertFromHex() {
        try {
            String hex = hexField.getText().trim().replaceAll("^0x|^\\$", "");
            if (hex.isEmpty()) {
                clearOtherFields(hexField);
                return;
            }

            long value = Long.parseLong(hex, 16);
            decField.setText(String.valueOf(value));
            binField.setText(Long.toBinaryString(value));
        } catch (NumberFormatException e) {
            clearOtherFields(hexField);
            showError("Valeur hexadécimale invalide");
        }
    }

    private void convertFromDec() {
        try {
            String dec = decField.getText().trim();
            if (dec.isEmpty()) {
                clearOtherFields(decField);
                return;
            }

            long value = Long.parseLong(dec);
            hexField.setText(Long.toHexString(value).toUpperCase());
            binField.setText(Long.toBinaryString(value));
        } catch (NumberFormatException e) {
            clearOtherFields(decField);
            showError("Valeur décimale invalide");
        }
    }

    private void clearAll() {
        hexField.setText("");
        decField.setText("");
        binField.setText("");
    }

    private void clearOtherFields(JTextField modifiedField) {
        if (modifiedField != hexField) hexField.setText("");
        if (modifiedField != decField) decField.setText("");
        if (modifiedField != binField) binField.setText("");
    }

    private void showError(String message) {
        DialogFactory.showError((JFrame) SwingUtilities.getWindowAncestor(this), "Erreur de conversion", message);
    }
}
