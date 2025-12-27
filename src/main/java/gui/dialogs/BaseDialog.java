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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;    
import java.awt.event.MouseMotionAdapter; 
import java.awt.event.MouseMotionListener; 

import java.util.Arrays;
import java.util.Vector;

public abstract class BaseDialog extends JDialog {

    protected final JPanel rootPanel;       
    protected final JPanel headerPanel;     
    protected final JPanel contentPanel;    
    protected final JPanel footerPanel;     

    public BaseDialog(JFrame parent, String title, int width, int height) {
        super(parent, true); 
        setUndecorated(true); 
        setSize(width, height); 
        setLocationRelativeTo(parent); 
        
        rootPanel = createRootPanel(); 
        setContentPane(rootPanel);     
        
        // --- NOUVEAU/MODIFIÉ: Assurez-vous que le rootPanel est opaque avant le paintComponent personnalisé
        rootPanel.setOpaque(false); // Il était déjà à false, mais la surcharge paintComponent doit le rendre opaque pour son propre dessin.
                                    // Cependant, le dessin personnalisé de fillRoundRect rend déjà le fond opaque.
                                    // Laisser à false est correct si le dessin remplit bien toute la zone.


        headerPanel = createHeader(title); 
        rootPanel.add(headerPanel, BorderLayout.NORTH); 

        contentPanel = new JPanel(new BorderLayout(10, 10)); 
        contentPanel.setOpaque(false); 
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20)); 
        rootPanel.add(contentPanel, BorderLayout.CENTER); 

        footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0)); 
        footerPanel.setOpaque(false); 
        footerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0)); 
        rootPanel.add(footerPanel, BorderLayout.SOUTH); 
        
        SwingUtilities.invokeLater(() -> {
            enableDragging(headerPanel);
            // On peut ajouter une revalidation finale ici pour le cas général
            rootPanel.revalidate(); // Revalider le rootPanel pour s'assurer que les sous-classes ont une base stable
            rootPanel.repaint();
        });
    }

    protected abstract void initContent();

    protected abstract void initFooter();

    private JPanel createRootPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.setColor(Theme.FOND_CLAIR);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

                g2d.setColor(Theme.FOND_HEADER);
                g2d.fillRoundRect(0, 0, getWidth(), 50, 20, 20); 
                g2d.fillRect(0, 30, getWidth(), 20); 

                g2d.setColor(Theme.BORDURE);
                g2d.setStroke(new BasicStroke(1));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20); 
                g2d.drawLine(0, 50, getWidth(), 50); 

                g2d.dispose();
            }

            @Override
            protected void paintBorder(Graphics g) {}
        };
        // L'opacité est gérée par le paintComponent, mais s'assurer qu'elle est bien définie ici pour Swing
        panel.setOpaque(false); // Laisser à false, le paintComponent gérera le dessin du fond.
        return panel;
    }

    private JPanel createHeader(String title) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false); 
        header.setPreferredSize(new Dimension(0, 50)); 
        header.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15)); 

        JLabel labelTitre = ComponentFactory.createTitleLabel(title); 
        StyledButton btnFermer = ComponentFactory.createCloseButton(this); 

        header.add(labelTitre, BorderLayout.WEST); 
        header.add(btnFermer, BorderLayout.EAST);  

        return header;
    }

    private void enableDragging(JPanel panel) {
        final Point[] clickPoint = new Point[1]; 

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                clickPoint[0] = e.getPoint(); 
            }
        });

        panel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point location = getLocation(); 
                if (location != null && clickPoint[0] != null) {
                    setLocation(
                        location.x + e.getX() - clickPoint[0].x,
                        location.y + e.getY() - clickPoint[0].y
                    );
                }
            }
        });
    }

    protected void addButton(String text, Color color, Runnable action) {
        StyledButton btn = new StyledButton(text, color, Color.WHITE);
        btn.setPreferredSize(new Dimension(130, 35)); 

        btn.addActionListener(e -> action.run());
        footerPanel.add(btn); 
    }
}
