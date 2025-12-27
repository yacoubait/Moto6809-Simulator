package gui.dialogs;

import gui.components.ComponentFactory;
import gui.theme.Theme;

import javax.swing.*;
import java.awt.*;

public class AboutDialog extends BaseDialog {

    public AboutDialog(JFrame parent) {
        super(parent, "À propos de Moto 6809", 400, 250);
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
        contentPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel appTitle = ComponentFactory.createTitleLabel("Moto 6809 Simulateur");
        appTitle.setFont(Theme.fontUI(Font.BOLD, 18));
        contentPanel.add(appTitle, gbc);

        JLabel version = ComponentFactory.createLabel("Version : 1.0.0");
        contentPanel.add(version, gbc);

        JLabel author = ComponentFactory.createLabel("Développé par : Etudiants FSTS");
        contentPanel.add(author, gbc);

        JLabel year = ComponentFactory.createLabel("Année : 2025-2026");
        contentPanel.add(year, gbc);

        JLabel description = ComponentFactory.createLabel("<html><center>Un simulateur de Motorola 6809<br>avec environnement d'assemblage et de débogage.</center></html>");
        description.setFont(Theme.FONT_NORMAL.deriveFont(Font.ITALIC));
        contentPanel.add(description, gbc);
    }

    @Override
    protected void initFooter() {
        addButton("Fermer", Theme.BTN_NEUTRE, this::dispose);
    }
}
