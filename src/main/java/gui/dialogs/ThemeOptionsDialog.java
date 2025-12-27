package gui.dialogs;

import gui.components.ComponentFactory;
import gui.components.StyledButton;
import gui.components.StyledTextField;
import gui.theme.Theme;
import gui.theme.RoundedBorder;

import javax.swing.*;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.io.File;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class ThemeOptionsDialog extends BaseDialog {

    private JSpinner uiScaleSpinner;
    private JLabel fondColorPreview; 
    private JLabel fondClairColorPreview;
    private JLabel fondHeaderColorPreview;
    private JLabel textColorPreview;
    private JLabel borderColorPreview;
    private JLabel highlightColorPreview;
    private JLabel toolbarColorPreview;

    // <<< NOUVEAU: Prévisualisation des couleurs CPU
    private JLabel cpuGradientTopColorPreview;
    private JLabel cpuGradientBottomColorPreview;
    private JLabel cpuBorderColorPreview;

    private JComboBox<String> fontUiComboBox;
    private JComboBox<String> fontMonoComboBox;

    private Map<String, Color> tempColors; 
    private Map<String, String> tempFontNames;

    public ThemeOptionsDialog(JFrame parent) {
        super(parent, "Options du Thème", 550, 650); // <<< MODIFIÉ: Taille du dialogue pour accueillir les onglets
        tempColors = new HashMap<>(); 
        tempFontNames = new HashMap<>();
        loadCurrentThemeValues(); 
        initContent();
        initFooter();
        SwingUtilities.invokeLater(() -> {
            rootPanel.revalidate();
            rootPanel.repaint();
        });
    }

    private void loadCurrentThemeValues() {
        tempColors.put("FOND", Theme.FOND);
        tempColors.put("FOND_CLAIR", Theme.FOND_CLAIR);
        tempColors.put("FOND_HEADER", Theme.FOND_HEADER);
        tempColors.put("REGISTRE", Theme.REGISTRE);
        tempColors.put("TEXTE", Theme.TEXTE);
        tempColors.put("BORDURE", Theme.BORDURE);
        tempColors.put("HIGHLIGHT", Theme.HIGHLIGHT);
        tempColors.put("BTN_PRIMAIRE", Theme.BTN_PRIMAIRE);
        tempColors.put("BTN_DANGER", Theme.BTN_DANGER);
        tempColors.put("BTN_INFO", Theme.BTN_INFO);
        tempColors.put("BTN_NEUTRE", Theme.BTN_NEUTRE);
        tempColors.put("BTN_TOOLBAR", Theme.BTN_TOOLBAR);
        tempColors.put("MENU_BAR", Theme.MENU_BAR);
        tempColors.put("TOOL_BAR", Theme.TOOL_BAR);
        tempColors.put("SYNTAX_MNEMONIC", Theme.SYNTAX_MNEMONIC);
        tempColors.put("SYNTAX_OPERAND", Theme.SYNTAX_OPERAND);
        tempColors.put("SYNTAX_COMMENT", Theme.SYNTAX_COMMENT);
        tempColors.put("SYNTAX_LABEL", Theme.SYNTAX_LABEL);
        tempColors.put("SYNTAX_DIRECTIVE", Theme.SYNTAX_DIRECTIVE);
        tempColors.put("SYNTAX_NUMBER", Theme.SYNTAX_NUMBER);
        tempColors.put("CPU_GRADIENT_TOP", Theme.CPU_GRADIENT_TOP); // <<< Chargement des couleurs CPU
        tempColors.put("CPU_GRADIENT_BOTTOM", Theme.CPU_GRADIENT_BOTTOM);
        tempColors.put("CPU_BORDER", Theme.CPU_BORDER);
        tempColors.put("BUS_COLOR", Theme.BUS_COLOR);


        tempFontNames.put("FONT_UI_NAME", Theme.FONT_UI_NAME);
        tempFontNames.put("FONT_MONO_NAME", Theme.FONT_MONO_NAME);
    }

    @Override
    protected void initContent() {
        // <<< MODIFICATION MAJEURE: Utilisation d'un JTabbedPane pour organiser les options
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setOpaque(false);
        tabbedPane.setBackground(Theme.FOND_CLAIR); // Fond des onglets
        tabbedPane.setForeground(Theme.TEXTE);
        tabbedPane.setFont(Theme.fontUI(Font.BOLD, 13));

        // --- Onglet "Général & Polices" ---
        JPanel generalPanel = createGeneralOptionsPanel();
        tabbedPane.addTab("Général & Polices", generalPanel);

        // --- Onglet "CPU" ---
        JPanel cpuOptionsPanel = createCPUOptionsPanel();
        tabbedPane.addTab("CPU", cpuOptionsPanel);
        
        // <<< NOUVEAU: Pourrait ajouter d'autres onglets ici (ex: "Syntaxe", "Avancé")

        contentPanel.setLayout(new BorderLayout()); // Le contentPanel principal contient le JTabbedPane
        contentPanel.add(tabbedPane, BorderLayout.CENTER);
    }

    // <<< NOUVEAU: Méthode pour créer le panneau d'options générales
    private JPanel createGeneralOptionsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8); 
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Échelle de l'UI
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        panel.add(ComponentFactory.createLabel("Échelle de l'UI :"), gbc);
        uiScaleSpinner = new JSpinner(new SpinnerNumberModel(Theme.UI_SCALE, 0.8, 2.0, 0.05));
        uiScaleSpinner.setEditor(new JSpinner.NumberEditor(uiScaleSpinner, "0.00"));
        gbc.gridx = 1; gbc.gridy = row++; gbc.gridwidth = 2;
        panel.add(uiScaleSpinner, gbc);

        // Sélecteurs de couleurs générales
        fondColorPreview = new JLabel();
        row = addColorChooserRow(panel, "Fond principal :", "FOND", fondColorPreview, row);
        fondClairColorPreview = new JLabel();
        row = addColorChooserRow(panel, "Fond clair :", "FOND_CLAIR", fondClairColorPreview, row);
        fondHeaderColorPreview = new JLabel();
        row = addColorChooserRow(panel, "Fond en-tête :", "FOND_HEADER", fondHeaderColorPreview, row);
        textColorPreview = new JLabel();
        row = addColorChooserRow(panel, "Texte :", "TEXTE", textColorPreview, row);
        borderColorPreview = new JLabel();
        row = addColorChooserRow(panel, "Bordures :", "BORDURE", borderColorPreview, row);
        highlightColorPreview = new JLabel();
        row = addColorChooserRow(panel, "Surlignage :", "HIGHLIGHT", highlightColorPreview, row);
        toolbarColorPreview = new JLabel();
        row = addColorChooserRow(panel, "Barre d'outils :", "TOOL_BAR", toolbarColorPreview, row);
        
        // Polices
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        panel.add(ComponentFactory.createLabel("Police UI :"), gbc);
        fontUiComboBox = new JComboBox<>(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        fontUiComboBox.setSelectedItem(tempFontNames.get("FONT_UI_NAME"));
        gbc.gridx = 1; gbc.gridy = row++; gbc.gridwidth = 2;
        panel.add(fontUiComboBox, gbc);

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        panel.add(ComponentFactory.createLabel("Police Monospace :"), gbc);
        fontMonoComboBox = new JComboBox<>(getMonospaceFontNames());
        fontMonoComboBox.setSelectedItem(tempFontNames.get("FONT_MONO_NAME"));
        gbc.gridx = 1; gbc.gridy = row++; gbc.gridwidth = 2;
        panel.add(fontMonoComboBox, gbc);

        // Espace vide pour pousser les éléments vers le haut
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 3;
        gbc.weighty = 1.0; 
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
    }

    // <<< NOUVEAU: Méthode pour créer le panneau d'options CPU
    private JPanel createCPUOptionsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8); 
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Couleurs de dégradé CPU
        cpuGradientTopColorPreview = new JLabel();
        row = addColorChooserRow(panel, "Dégradé CPU (Haut) :", "CPU_GRADIENT_TOP", cpuGradientTopColorPreview, row);
        cpuGradientBottomColorPreview = new JLabel();
        row = addColorChooserRow(panel, "Dégradé CPU (Bas) :", "CPU_GRADIENT_BOTTOM", cpuGradientBottomColorPreview, row);
        cpuBorderColorPreview = new JLabel();
        row = addColorChooserRow(panel, "Bordure CPU :", "CPU_BORDER", cpuBorderColorPreview, row);
        
        // Espace vide pour pousser les éléments vers le haut
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 3;
        gbc.weighty = 1.0; 
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
    }


    // <<< MODIFIÉ: addColorChooserRow prend maintenant un JPanel comme premier argument
    private int addColorChooserRow(JPanel targetPanel, String labelText, String colorKey, JLabel colorPreview, int row) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        targetPanel.add(ComponentFactory.createLabel(labelText), gbc);

        colorPreview.setOpaque(true);
        colorPreview.setBackground(tempColors.get(colorKey)); 
        colorPreview.setBorder(new RoundedBorder(Theme.BORDURE, 1, 8, new Insets(2,2,2,2)));
        colorPreview.setPreferredSize(new Dimension(30, 20));
        gbc.gridx = 1; gbc.gridy = row; gbc.gridwidth = 1;
        targetPanel.add(colorPreview, gbc);

        StyledButton selectButton = new StyledButton("Choisir", Theme.BTN_NEUTRE, Color.WHITE);
        selectButton.addActionListener(e -> chooseColor(colorKey, colorPreview));
        gbc.gridx = 2; gbc.gridy = row++; gbc.gridwidth = 1;
        targetPanel.add(selectButton, gbc);
        
        return row;
    }

    private String[] getMonospaceFontNames() {
        return new String[]{"Consolas", "Monospaced", "Courier New", "Lucida Console", "Fira Code"};
    }


    @Override
    protected void initFooter() {
        addButton("Appliquer", Theme.BTN_PRIMAIRE, this::applyTheme);
        addButton("Annuler", Theme.BTN_DANGER, this::dispose);
        addButton("Défaut", Theme.BTN_NEUTRE, this::resetToDefault);
    }

    private void chooseColor(String colorKey, JLabel previewLabel) {
        Color initialColor = tempColors.get(colorKey);
        Color chosenColor = JColorChooser.showDialog(this, "Choisir la couleur " + colorKey, initialColor);
        if (chosenColor != null) {
            tempColors.put(colorKey, chosenColor);
            previewLabel.setBackground(chosenColor);
            previewLabel.repaint();
        }
    }

    private void applyTheme() {
        float oldScale = Theme.UI_SCALE; 
        Theme.UI_SCALE = ((Number) uiScaleSpinner.getValue()).floatValue();
        
        Theme.FOND = tempColors.get("FOND");
        Theme.FOND_CLAIR = tempColors.get("FOND_CLAIR");
        Theme.FOND_HEADER = tempColors.get("FOND_HEADER");
        Theme.REGISTRE = tempColors.get("REGISTRE");
        Theme.TEXTE = tempColors.get("TEXTE");
        Theme.BORDURE = tempColors.get("BORDURE");
        Theme.HIGHLIGHT = tempColors.get("HIGHLIGHT");
        Theme.BTN_PRIMAIRE = tempColors.get("BTN_PRIMAIRE");
        Theme.BTN_DANGER = tempColors.get("BTN_DANGER");
        Theme.BTN_INFO = tempColors.get("BTN_INFO");
        Theme.BTN_NEUTRE = tempColors.get("BTN_NEUTRE");
        Theme.BTN_TOOLBAR = tempColors.get("BTN_TOOLBAR");
        Theme.MENU_BAR = tempColors.get("MENU_BAR");
        Theme.TOOL_BAR = tempColors.get("TOOL_BAR"); 
        Theme.SYNTAX_MNEMONIC = tempColors.get("SYNTAX_MNEMONIC");
        Theme.SYNTAX_OPERAND = tempColors.get("SYNTAX_OPERAND");
        Theme.SYNTAX_COMMENT = tempColors.get("SYNTAX_COMMENT");
        Theme.SYNTAX_LABEL = tempColors.get("SYNTAX_LABEL");
        Theme.SYNTAX_DIRECTIVE = tempColors.get("SYNTAX_DIRECTIVE");
        Theme.SYNTAX_NUMBER = tempColors.get("SYNTAX_NUMBER");
        
        // <<< NOUVEAU: Application des couleurs CPU
        Theme.CPU_GRADIENT_TOP = tempColors.get("CPU_GRADIENT_TOP");
        Theme.CPU_GRADIENT_BOTTOM = tempColors.get("CPU_GRADIENT_BOTTOM");
        Theme.CPU_BORDER = tempColors.get("CPU_BORDER");
        
        Theme.BUS_COLOR = tempColors.get("BUS_COLOR");


        Theme.FONT_UI_NAME = (String) fontUiComboBox.getSelectedItem();
        Theme.FONT_MONO_NAME = (String) fontMonoComboBox.getSelectedItem();

        Theme.loadDefaultFontsAndInsets(); 
        Theme.saveThemeProperties(); 

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                JFrame parentFrame = (JFrame) getParent();
                SwingUtilities.updateComponentTreeUI(parentFrame);
                
                if (Theme.UI_SCALE != oldScale) {
                    int oldWidth = parentFrame.getWidth();
                    int oldHeight = parentFrame.getHeight();
                    int newWidth = Math.round(oldWidth * (Theme.UI_SCALE / oldScale));
                    int newHeight = Math.round(oldHeight * (Theme.UI_SCALE / oldScale));
                    parentFrame.setSize(newWidth, newHeight);
                    parentFrame.setLocationRelativeTo(null); 
                }

                for (Window w : Window.getWindows()) {
                    if (w instanceof JDialog) {
                        SwingUtilities.updateComponentTreeUI(w);
                        w.revalidate();
                        w.repaint();
                    }
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        dispose();
    }

    private void resetToDefault() {
        Theme.loadThemeProperties(); 
        Theme.UI_SCALE = 1.15f; 
        Theme.FONT_UI_NAME = "Segoe UI";
        Theme.FONT_MONO_NAME = "Consolas";
        // <<< NOUVEAU: Réinitialisation explicite des couleurs CPU aux valeurs par défaut codées en dur
        Theme.CPU_GRADIENT_TOP = new Color(240, 255, 255);
        Theme.CPU_GRADIENT_BOTTOM = new Color(0, 0, 0); // Noir par défaut
        Theme.CPU_BORDER = new Color(110, 120, 130);
        
        Theme.loadDefaultFontsAndInsets(); 

        loadCurrentThemeValues(); 
        applyTheme();
        
        dispose();
        new ThemeOptionsDialog((JFrame) getParent()).setVisible(true);
    }
}
