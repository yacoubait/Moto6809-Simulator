// Contenu de gui/components/ComponentFactory.java
package gui.components;

import gui.theme.RoundedBorder;
import gui.theme.Theme;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public final class ComponentFactory {

    private ComponentFactory() {}

    public static StyledButton createToolbarButton(String icon, String tooltip) {
        StyledButton btn = new StyledButton(icon, Theme.BTN_TOOLBAR, Color.WHITE);
        btn.setToolTipText(tooltip);
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        btn.setRadius(12);
        return btn;
    }

    public static StyledButton createToolbarIconButton(ImageIcon icon, String tooltip) {
        StyledButton btn = new StyledButton(icon, new Color(0, 0, 0, 0), Theme.TEXTE);
        btn.setToolTipText(tooltip);
        btn.setRadius(8);

        btn.setColors(
            new Color(0, 0, 0, 0),
            new Color(Theme.HIGHLIGHT.getRed(), Theme.HIGHLIGHT.getGreen(), Theme.HIGHLIGHT.getBlue(), 60),
            new Color(Theme.HIGHLIGHT.getRed(), Theme.HIGHLIGHT.getGreen(), Theme.HIGHLIGHT.getBlue(), 100)
        );
        return btn;
    }

    public static StyledButton createDialogButton(String text, Color bgColor, Color fgColor) {
        StyledButton btn = new StyledButton(text, bgColor, fgColor);
        btn.setFont(Theme.fontUI(Font.BOLD, 13));
        btn.setPreferredSize(new Dimension(130, 35));
        btn.setBorder(new RoundedBorder(bgColor.darker(), 1, 12, new Insets(8, 15, 8, 15)));
        btn.setRadius(12);
        return btn;
    }

    public static StyledButton createCloseButton(JDialog dialog) {
        StyledButton btn = new StyledButton("", Theme.FOND_CLAIR, new Color(100, 100, 100));

        btn.setFont(Theme.fontUI(Font.BOLD, 13));
        btn.setPreferredSize(new Dimension(30, 28)); 
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false); 
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new RoundedBorder(Theme.BORDURE, 1, 10, new Insets(5, 5, 5, 5))); 
        btn.setRadius(10);

        btn.setColors(Theme.FOND_CLAIR, new Color(232, 17, 35), new Color(200, 15, 30));
        btn.setForeground(new Color(100, 100, 100)); 

        btn.addActionListener(e -> dialog.dispose());

        return btn;
    }

    public static StyledButton createNavButton(String text, String tooltip) {
        StyledButton btn = new StyledButton(text, Theme.BTN_NEUTRE, Color.WHITE);
        btn.setToolTipText(tooltip);
        btn.setFont(Theme.fontUI(Font.BOLD, 14));
        btn.setPreferredSize(new Dimension(36, 32));
        btn.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        btn.setRadius(12);
        return btn;
    }

    public static StyledTextField createRegisterField(int columns) {
        return StyledTextField.forRegister(columns);
    }

    public static StyledTextField createFlagField() {
        return StyledTextField.forFlag();
    }

    public static StyledTextField createInputField(int columns) {
        return StyledTextField.forInput(columns);
    }

    public static JPanel createRoundedPanel() {
        return createRoundedPanel(Theme.FOND_CLAIR);
    }

    public static JPanel createRoundedPanel(Color bgColor) {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(getBackground());
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(),
                    Theme.RAYON_ARRONDI_GRAND, Theme.RAYON_ARRONDI_GRAND);
                g2d.dispose();
                super.paintComponent(g);
            }
        };

        panel.setOpaque(false);
        panel.setBackground(bgColor);

        return panel;
    }

    public static JPanel createHorizontalBus() {
        JPanel ligne = new JPanel();
        ligne.setPreferredSize(new Dimension(Theme.ESPACEMENT, Theme.EPAISSEUR_BUS));
        ligne.setBackground(Theme.BUS_COLOR);
        return ligne;
    }

    public static JPanel createVerticalBus(int height) {
        JPanel ligne = new JPanel();
        ligne.setPreferredSize(new Dimension(Theme.EPAISSEUR_BUS, height));
        ligne.setBackground(Theme.BUS_COLOR);
        return ligne;
    }

    public static JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.FONT_LABEL);
        label.setForeground(Theme.TEXTE);
        return label;
    }

    public static JLabel createTitleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.FONT_TITRE);
        label.setForeground(Theme.TEXTE);
        return label;
    }
}
