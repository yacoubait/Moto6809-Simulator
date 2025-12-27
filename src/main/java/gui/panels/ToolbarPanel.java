package gui.panels;

import gui.Moto6809;
import gui.theme.Theme;
import gui.components.ComponentFactory;
import gui.components.StyledButton;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ToolbarPanel extends JToolBar {

    private static final int TOOLBAR_ICON_MAX_SIZE = 16;
    private final Map<String, ImageIcon> iconCache = new HashMap<>();

    public ToolbarPanel(final Moto6809 frame) { // <<< MODIFIÉ: "frame" est maintenant final
        setFloatable(false);
        setBackground(Theme.TOOL_BAR);
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));

        // --- Groupe "Fichier" ---
        JPanel fileGroup = createGroupPanelWithRoundedBackground(new Color(240, 240, 240), 25);
        fileGroup.add(addButton(loadIcon("new.png"), "Nouveau (Ctrl+N)", () -> frame.newFile()));
        fileGroup.add(addButton(loadIcon("open.png"), "Ouvrir (Ctrl+O)", () -> frame.openFile()));
        fileGroup.add(addButton(loadIcon("save.png"), "Sauvegarder (Ctrl+S)", () -> frame.saveFile()));
        add(fileGroup);

        addSeparator(new Dimension(5, 0));

        // --- Groupe "Simulation" ---
        JPanel simGroup = createGroupPanelWithRoundedBackground(new Color(240, 240, 240), 25);
        simGroup.add(addButton(loadIcon("execution.png"), "Exécuter (F5)", () -> frame.executeCode()));
        simGroup.add(addButton(loadIcon("pas_a_pas.png"), "Pas à pas (F6)", () -> frame.stepCode()));
        simGroup.add(addButton(loadIcon("pause.png"), "Pause (F7)", () -> frame.pauseSimulation())); 
        simGroup.add(addButton(loadIcon("reprendre.png"), "Reprendre (F8)", () -> frame.resumeSimulation())); // <<< LIGNE 45
        simGroup.add(addButton(loadIcon("stop.png"), "Arrêter (F9)", () -> frame.stopSimulation()));
        simGroup.add(addButton(loadIcon("refresh.png"), "Réinitialiser (Ctrl+R)", () -> frame.resetSimulation()));
        add(simGroup);

        addSeparator(new Dimension(5, 0));

        // --- Groupe "Outils / Panneaux" ---
        JPanel toolsGroup = createGroupPanelWithRoundedBackground(new Color(240, 240, 240), 25);
        toolsGroup.add(addButton(loadIcon("opened.png"), "Éditeur de code (Ctrl+E)", () -> frame.toggleCodeEditor()));
        toolsGroup.add(addButton(loadIcon("memory.png"), "Mémoire RAM", () -> frame.showRamMemory())); 
        toolsGroup.add(addButton(loadIcon("stack.png"), "Pile", () -> frame.showStackMemory())); 
        toolsGroup.add(addButton(loadIcon("options.png"), "Options du thème", () -> frame.showThemeOptionsDialog())); 
        add(toolsGroup);

        addSeparator(new Dimension(5, 0));

        // --- Groupe "Aide / Utilitaires" ---
        JPanel helpGroup = createGroupPanelWithRoundedBackground(new Color(240, 240, 240), 25);
        helpGroup.add(addButton(loadIcon("convertisseur.png"), "Convertisseur Hex/Dec", () -> frame.showHexDecConverter()));
        helpGroup.add(addButton(loadIcon("ascii.png"), "Table ASCII", () -> frame.showAsciiTable()));
        helpGroup.add(addButton(loadIcon("calculator.png"), "Calculatrice", () -> frame.openCalculator()));
        helpGroup.add(addButton(loadIcon("help.png"), "Aide (PDF)", () -> frame.showDocumentation())); 
        add(helpGroup);
    }

    /** Crée un JPanel avec fond arrondi derrière les boutons */
    private JPanel createGroupPanelWithRoundedBackground(Color bgColor, int radius) {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        panel.setOpaque(false);
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        return panel;
    }

    private ImageIcon loadIcon(String filename) {
        if (filename == null) return emptyIcon();
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        AffineTransform tx = gc.getDefaultTransform();
        double scaleX = tx.getScaleX();
        double scaleY = tx.getScaleY();
        double scale = Math.max(scaleX, scaleY);

        int scaleFactor = scale >= 2.5 ? 3 : scale >= 1.5 ? 2 : 1;

        String cacheKey = filename + "@" + scaleFactor;
        if (iconCache.containsKey(cacheKey)) return iconCache.get(cacheKey);

        int targetPx = (int) Math.round(TOOLBAR_ICON_MAX_SIZE * scale);
        String basePath = "/icons/";
        BufferedImage original = null;

        if (scaleFactor > 1) {
            String highName = filename.replaceFirst("(\\.png|\\.jpg|\\.jpeg)$", "@" + scaleFactor + "$1");
            URL highUrl = getClass().getResource(basePath + highName);
            if (highUrl != null) {
                try { original = ImageIO.read(highUrl); } catch (IOException ignored) {}
            }
        }

        if (original == null) {
            URL imageUrl = getClass().getResource(basePath + filename);
            if (imageUrl == null) return emptyIcon();
            try { original = ImageIO.read(imageUrl); } catch (IOException e) { return emptyIcon(); }
        }

        int ow = original.getWidth(), oh = original.getHeight();
        double downscale = Math.min(1.0, Math.min((double) targetPx / ow, (double) targetPx / oh));
        int finalW = downscale < 1.0 ? (int) Math.round(ow * downscale) : targetPx;
        int finalH = downscale < 1.0 ? (int) Math.round(oh * downscale) : targetPx;

        BufferedImage canvas = new BufferedImage(targetPx, targetPx, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = canvas.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setComposite(AlphaComposite.SrcOver);
        int x = (targetPx - finalW) / 2, y = (targetPx - finalH) / 2;
        g2.drawImage(original, x, y, finalW, finalH, null);
        g2.dispose();

        ImageIcon icon = new ImageIcon(canvas);
        iconCache.put(cacheKey, icon);
        return icon;
    }

    private ImageIcon emptyIcon() {
        BufferedImage img = new BufferedImage(TOOLBAR_ICON_MAX_SIZE, TOOLBAR_ICON_MAX_SIZE, BufferedImage.TYPE_INT_ARGB);
        return new ImageIcon(img);
    }

    /**
     * Ajoute un bouton à la barre d'outils.
     * @param icon L'icône du bouton.
     * @param tooltip Le texte d'info-bulle.
     * @param action L'action à exécuter lorsque le bouton est cliqué (doit être un Runnable).
     * @return Le bouton stylisé créé.
     */
    private StyledButton addButton(ImageIcon icon, String tooltip, Runnable action) {
        StyledButton btn = ComponentFactory.createToolbarIconButton(icon, tooltip);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        int btnHeight = TOOLBAR_ICON_MAX_SIZE + 20;
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, btnHeight));
        btn.addActionListener(e -> action.run()); 
        return btn;
    }
}
