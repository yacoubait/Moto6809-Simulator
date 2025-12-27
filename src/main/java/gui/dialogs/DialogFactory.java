package gui.dialogs;

import gui.theme.*;
import gui.components.ComponentFactory; 
import gui.components.StyledButton; 
import javax.swing.*;
import java.awt.*;

public final class DialogFactory {
    
    private DialogFactory() {} 
    
    // ==================== DIALOGUE DE CONFIRMATION (Oui/Non) ====================
    
    public static boolean showConfirmation(JFrame parent, String title, String message) {
        final boolean[] result = {false}; 
        
        BaseDialog dialog = new BaseDialog(parent, title, 420, 180) {
            @Override
            protected void initContent() {
                JLabel label = new JLabel("<html><center>" + message.replace("\n", "<br>") + "</center></html>");
                label.setFont(Theme.FONT_NORMAL);
                label.setForeground(Theme.TEXTE);
                label.setHorizontalAlignment(JLabel.CENTER);
                contentPanel.add(label, BorderLayout.CENTER);
            }
            
            @Override
            protected void initFooter() {
                addButton("Oui", Theme.BTN_PRIMAIRE, () -> {
                    result[0] = true;
                    dispose(); 
                });
                addButton("Non", Theme.BTN_DANGER, this::dispose); 
            }
        };
        // NOUVEAU: Appels initContent/initFooter et revalidate/repaint pour ce dialogue spécifique
        dialog.initContent();
        dialog.initFooter();
        SwingUtilities.invokeLater(() -> {
            dialog.rootPanel.revalidate();
            dialog.rootPanel.repaint();
        });
        
        dialog.setVisible(true); 
        return result[0]; 
    }
    
    // ==================== DIALOGUE D'OPTIONS (Multiples boutons) ====================
    
    public static int showOptions(JFrame parent, String title, String message, String... options) {
        final int[] result = {-1}; 
        Color[] colors = {Theme.BTN_PRIMAIRE, Theme.BTN_DANGER, Theme.BTN_NEUTRE};
        
        int width = 420; 
        if (options.length > 2) {
            width += (options.length - 2) * 140; 
        }
        if (options.length == 1) { 
            width = 300;
        }
        
        BaseDialog dialog = new BaseDialog(parent, title, width, 180) {
            @Override
            protected void initContent() {
                JLabel label = new JLabel("<html><center>" + message.replace("\n", "<br>") + "</center></html>");
                label.setFont(Theme.FONT_NORMAL);
                label.setForeground(Theme.TEXTE);
                label.setHorizontalAlignment(JLabel.CENTER);
                contentPanel.add(label, BorderLayout.CENTER);
            }
            
            @Override
            protected void initFooter() {
                for (int i = 0; i < options.length; i++) {
                    final int index = i; 
                    Color color = (i < colors.length) ? colors[i] : Theme.BTN_NEUTRE;
                    addButton(options[i], color, () -> {
                        result[0] = index;
                        dispose();
                    });
                }
            }
        };
        // NOUVEAU: Appels initContent/initFooter et revalidate/repaint pour ce dialogue spécifique
        dialog.initContent();
        dialog.initFooter();
        SwingUtilities.invokeLater(() -> {
            dialog.rootPanel.revalidate();
            dialog.rootPanel.repaint();
        });
        
        dialog.setVisible(true);
        return result[0];
    }
    
    // ==================== DIALOGUES DE MESSAGES SIMPLES (Succès, Erreur, Info) ====================
    
    public static void showSuccess(JFrame parent, String title, String message) {
        showMessage(parent, title, message, "✓", Theme.BTN_PRIMAIRE);
    }
    
    public static void showError(JFrame parent, String title, String message) {
        showMessage(parent, title, message, "✕", Theme.BTN_DANGER);
    }
    
    public static void showInfo(JFrame parent, String title, String message) {
        showMessage(parent, title, message, "ℹ", Theme.BTN_INFO);
    }
    
    private static void showMessage(JFrame parent, String title, String message, String icon, Color iconColor) {
        BaseDialog dialog = new BaseDialog(parent, title, 450, 200) {
            @Override
            protected void initContent() {
                JPanel panel = new JPanel(new BorderLayout(15, 10));
                panel.setOpaque(false);
                
                JLabel iconLabel = new JLabel(icon);
                iconLabel.setFont(new Font("Segoe UI", Font.BOLD, 32)); 
                iconLabel.setForeground(iconColor);
                iconLabel.setVerticalAlignment(JLabel.TOP);
                iconLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 15));
                panel.add(iconLabel, BorderLayout.WEST);
                
                JTextArea textMessage = new JTextArea(message);
                textMessage.setFont(Theme.FONT_NORMAL);
                textMessage.setForeground(Theme.TEXTE);
                textMessage.setEditable(false); 
                textMessage.setLineWrap(true); 
                textMessage.setWrapStyleWord(true); 
                textMessage.setOpaque(false); 
                textMessage.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 5));
                
                // NOUVEAU: Définir une taille préférée pour le JTextArea
                textMessage.setPreferredSize(new Dimension(300, 80)); // Une taille raisonnable pour le message
                
                panel.add(textMessage, BorderLayout.CENTER);
                
                contentPanel.add(panel, BorderLayout.CENTER);
            }
            
            @Override
            protected void initFooter() {
                addButton("OK", iconColor, this::dispose);
            }
        };
        // NOUVEAU: Appels initContent/initFooter et revalidate/repaint pour ce dialogue spécifique
        dialog.initContent();
        dialog.initFooter();
        SwingUtilities.invokeLater(() -> {
            dialog.rootPanel.revalidate();
            dialog.rootPanel.repaint();
        });
        
        dialog.setVisible(true);
    }
}
