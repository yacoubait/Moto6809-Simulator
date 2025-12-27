package gui;

// Imports de theme
import gui.theme.Theme;
import gui.theme.RoundedBorder;

// Imports de panels
import gui.panels.CPUPanel;
import gui.panels.CodeEditorPanel;
import gui.panels.ToolbarPanel;
import gui.panels.MemoryPanel;
import gui.panels.FlagsPanel;

// Imports de dialogs
import gui.dialogs.AsciiTableDialog;
import gui.dialogs.HexDecConverterDialog;
import gui.dialogs.DialogFactory;
import gui.dialogs.ThemeOptionsDialog;
import gui.dialogs.InstructionSetDialog;
import gui.dialogs.AboutDialog;

// Imports de menu
import gui.menu.MenuBarBuilder;

// Imports du moteur de simulation
import sim.SimulatorEngine;

// Imports du CPU et de la mémoire (pour les références dans les setters)
import cpu.CPU6809;
import mem.Memoire;

// Imports Swing standards
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

// Imports Java AWT et IO standards
import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import javax.imageio.ImageIO;

public class Moto6809 extends JFrame {

    private final CPUPanel cpuPanel;
    private final CodeEditorPanel codePanel;
    private final ToolbarPanel toolbar;

    private File currentFile = null;
    private boolean codeVisible = true;

    private final SimulatorEngine simulatorEngine;

    private JSplitPane splitPane;
    private static final int DEFAULT_CODE_EDITOR_WIDTH = 400;

    private JTabbedPane viewTabbedPane;

    private final MemoryPanel ramMemoryPanel;
    private final MemoryPanel romMemoryPanel;
    private final MemoryPanel stackMemoryPanel;


    public Moto6809() {
        setTitle("Moto 6809 - Simulateur de Microprocesseur");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(900, 700));
        setLocationRelativeTo(null);

        setAppIcon();

        Theme.loadThemeProperties();

        initLookAndFeel();

        simulatorEngine = new SimulatorEngine();

        cpuPanel = new CPUPanel();
        codePanel = new CodeEditorPanel();
        toolbar = new ToolbarPanel(this); // ToolbarPanel s'attend à resumeSimulation() sur this

        ramMemoryPanel = new MemoryPanel();
        ramMemoryPanel.setMemoire(simulatorEngine.getMemoire());
        ramMemoryPanel.setCPU(simulatorEngine.getCPU());
        ramMemoryPanel.setSimulatorEngine(simulatorEngine);
        ramMemoryPanel.setStartAddress(Memoire.RAM_START);
        ramMemoryPanel.setDisplayLength(Memoire.RAM_SIZE);
        ramMemoryPanel.setReadOnly(false);
        ramMemoryPanel.setTitle("RAM");
        ramMemoryPanel.setMemoryRange(Memoire.RAM_START, Memoire.RAM_END);

        romMemoryPanel = new MemoryPanel();
        romMemoryPanel.setMemoire(simulatorEngine.getMemoire());
        romMemoryPanel.setCPU(simulatorEngine.getCPU());
        romMemoryPanel.setSimulatorEngine(simulatorEngine);
        romMemoryPanel.setStartAddress(Memoire.ROM_START);
        romMemoryPanel.setDisplayLength(Memoire.ROM_SIZE);
        romMemoryPanel.setReadOnly(true);
        romMemoryPanel.setTitle("ROM");
        romMemoryPanel.setMemoryRange(Memoire.ROM_START, Memoire.ROM_END);

        stackMemoryPanel = new MemoryPanel();
        stackMemoryPanel.setMemoire(simulatorEngine.getMemoire());
        stackMemoryPanel.setCPU(simulatorEngine.getCPU());
        stackMemoryPanel.setSimulatorEngine(simulatorEngine);
        stackMemoryPanel.setReadOnly(false);
        stackMemoryPanel.setControlsVisible(false);
        stackMemoryPanel.setTitle("Pile");
        stackMemoryPanel.setMemoryRange(Memoire.RAM_START, Memoire.RAM_END);


        cpuPanel.setCPU(simulatorEngine.getCPU());
        cpuPanel.getFlagsPanel().setCPU(simulatorEngine.getCPU());
        codePanel.setCPU(simulatorEngine.getCPU());
        codePanel.setSimulatorEngine(simulatorEngine);
        simulatorEngine.getStepExecutor().setParentFrame(this);

        cpuPanel.setSimulatorEngine(simulatorEngine);

        codePanel.setVisible(codeVisible);
        codePanel.setOnCloseAction(this::closeCodeEditor);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(Theme.FOND);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, codePanel, createRightPanel());
        splitPane.setDividerLocation(DEFAULT_CODE_EDITOR_WIDTH);
        splitPane.setResizeWeight(0.25);
        splitPane.setOpaque(false);
        splitPane.setBorder(null);

        splitPane.setDividerSize(5);
        splitPane.setOneTouchExpandable(true);
        splitPane.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

        mainPanel.add(splitPane, BorderLayout.CENTER);

        setJMenuBar(MenuBarBuilder.build(this));
        add(toolbar, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * Définit l'icône de la fenêtre JFrame.
     * Charge l'icône depuis les ressources du projet.
     */
    private void setAppIcon() {
        try {
            InputStream imgStream = getClass().getResourceAsStream("/icons/logo.png");

            if (imgStream != null) {
                Image icon = ImageIO.read(imgStream);
                setIconImage(icon);
            } else {
                System.err.println("Icône de l'application 'logo.png' introuvable dans les ressources. Assurez-vous qu'elle est dans src/main/resources/icons/.");
            }
        } catch (IOException e) {
            System.err.println("Erreur de chargement de l'icône de l'application: " + e.getMessage());
        }
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);

        viewTabbedPane = new JTabbedPane();
        viewTabbedPane.setOpaque(false);
        viewTabbedPane.setBackground(Theme.FOND);
        viewTabbedPane.setForeground(Theme.TEXTE);
        viewTabbedPane.setFont(Theme.fontUI(Font.BOLD, 13));
        viewTabbedPane.setTabPlacement(JTabbedPane.TOP);

        viewTabbedPane.addTab("Registres", cpuPanel);
        viewTabbedPane.addTab("RAM", ramMemoryPanel);
        viewTabbedPane.addTab("ROM", romMemoryPanel);
        viewTabbedPane.addTab("Pile", stackMemoryPanel);

        panel.add(viewTabbedPane, BorderLayout.CENTER);

        return panel;
    }

    private void initLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        UIManager.put("PopupMenu.border", new RoundedBorder(
            Theme.BORDURE, 1, 12, new Insets(2, 2, 2, 2)
        ));
        UIManager.put("MenuItem.opaque", true);
        UIManager.put("MenuItem.background", Theme.FOND_CLAIR);
        UIManager.put("MenuItem.foreground", Theme.TEXTE);
        UIManager.put("MenuItem.selectionBackground", Theme.HIGHLIGHT);
        UIManager.put("MenuItem.selectionForeground", Color.WHITE);
        UIManager.put("MenuItem.font", Theme.fontUI(Font.PLAIN, 14));

        UIManager.put("Menu.opaque", true);
        UIManager.put("Menu.background", Theme.MENU_BAR);
        UIManager.put("Menu.foreground", Color.BLACK);
        UIManager.put("Menu.selectionBackground", Theme.HIGHLIGHT);
        UIManager.put("Menu.selectionForeground", Color.WHITE);
        UIManager.put("Menu.font", Theme.fontUI(Font.PLAIN, 15));

        UIManager.put("TabbedPane.background", Theme.FOND);
        UIManager.put("TabbedPane.foreground", Theme.TEXTE);
        UIManager.put("TabbedPane.selectedBackground", Theme.HIGHLIGHT);
        UIManager.put("TabbedPane.selectedForeground", Color.WHITE);
        UIManager.put("TabbedPane.contentBorderInsets", new Insets(0,0,0,0));
        UIManager.put("TabbedPane.tabAreaInsets", new Insets(0,0,0,0));
    }

    public void newFile() {
        showCodeEditor();
        if (!codePanel.isEmpty()) {
            int choice = DialogFactory.showOptions(this,
                "Nouveau programme",
                "Voulez-vous enregistrer le programme actuel ?",
                "Enregistrer", "Ne pas enregistrer", "Annuler"
            );
            if (choice == 0) {
                if (!saveFile()) return;
            } else if (choice == 2 || choice == -1) {
                return;
            }
        }
        codePanel.clear();
        currentFile = null;
        setTitle("Moto 6809 - Nouveau programme");
        simulatorEngine.resetSimulationState();
        codePanel.clearHighlight();
        ramMemoryPanel.clearHighlight();
        romMemoryPanel.clearHighlight();
        stackMemoryPanel.clearHighlight();
    }

    public void showCodeEditor() {
        if (!codeVisible) {
            codePanel.setVisible(true);
            codeVisible = true;
            setTitle("Moto 6809 - Nouveau programme");
            if (splitPane != null) {
                splitPane.setDividerLocation(DEFAULT_CODE_EDITOR_WIDTH);
            }
            revalidate();
            repaint();
        }
    }

    public void openFile() {
        showCodeEditor();
        if (!codePanel.isEmpty()) {
            int choice = DialogFactory.showOptions(this,
                "Fichier non enregistré",
                "Voulez-vous enregistrer le fichier actuel ?",
                "Enregistrer", "Ne pas enregistrer", "Annuler"
            );
            if (choice == 0) {
                if (!saveFile()) return;
            } else if (choice == 2 || choice == -1) {
                return;
            }
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        chooser.setFileFilter(new FileNameExtensionFilter(
            "Fichiers Assembleur", "asm", "s", "txt"
        ));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadFile(chooser.getSelectedFile());
        }
    }

    public boolean saveFile() {
        if (currentFile == null) {
            return saveFileAs();
        }
        return writeFile(currentFile);
    }

    public boolean saveFileAs() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(currentFile != null ? currentFile.getParentFile() : new File(System.getProperty("user.home")));
        chooser.setFileFilter(new FileNameExtensionFilter(
            "Fichiers Assembleur", "asm"
        ));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".asm")) {
                file = new File(file.getAbsolutePath() + ".asm");
            }
            if (file.exists()) {
                if (!DialogFactory.showConfirmation(this, "Fichier existant",
                    "Le fichier existe déjà. Voulez-vous le remplacer ?")) {
                    return false;
                }
            }
            return writeFile(file);
        }
        return false;
    }

    private void loadFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            codePanel.setCode(content.toString());
            currentFile = file;
            setTitle("Moto 6809 - " + file.getName());
            simulatorEngine.resetSimulationState();
            codePanel.clearHighlight();
            ramMemoryPanel.clearHighlight();
            romMemoryPanel.clearHighlight();
            stackMemoryPanel.clearHighlight();

        } catch (IOException e) {
            DialogFactory.showError(this, "Erreur de lecture",
                "Impossible d'ouvrir le fichier :\n" + e.getMessage());
        }
    }

    private boolean writeFile(File file) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(codePanel.getCode());
            currentFile = file;
            setTitle("Moto 6809 - " + file.getName());

            DialogFactory.showSuccess(this, "Fichier enregistré",
                "Le fichier a été enregistré avec succès :\n" + file.getAbsolutePath());

            return true;

        } catch (IOException e) {
            DialogFactory.showError(this, "Erreur de sauvegarde",
                "Impossible d'enregistrer le fichier :\n" + e.getMessage());
            return false;
        }
    }

    public void executeCode() {
        if (codePanel.isEmpty()) {
            DialogFactory.showError(this, "Aucun code",
                "Veuillez d'abord entrer du code assembleur dans l'éditeur.");
            return;
        }
        try {
            simulatorEngine.loadAndAssemble(codePanel.getCode());
            codePanel.setAddressToLineMap(simulatorEngine.getAddressToLineMap());
            ramMemoryPanel.refresh();
            romMemoryPanel.refresh();
            stackMemoryPanel.refresh();

            int currentPC = simulatorEngine.getCPU().getPC();
            if (currentPC >= Memoire.ROM_START && currentPC <= Memoire.ROM_END) {
                romMemoryPanel.setStartAddress(currentPC - (currentPC % MemoryPanel.BYTES_PER_ROW));
                viewTabbedPane.setSelectedComponent(romMemoryPanel);
            } else if (currentPC >= Memoire.RAM_START && currentPC <= Memoire.RAM_END) {
                ramMemoryPanel.setStartAddress(currentPC - (currentPC % MemoryPanel.BYTES_PER_ROW));
                viewTabbedPane.setSelectedComponent(ramMemoryPanel);
            }

            simulatorEngine.run();
        } catch (Exception e) {
            DialogFactory.showError(this, "Erreur d'exécution", e.getMessage());
            simulatorEngine.stop();
        }
    }

    public void stepCode() {
        if (codePanel.isEmpty()) {
            DialogFactory.showError(this, "Aucun code",
                "Veuillez d'abord entrer du code assembleur dans l'éditeur.");
            return;
        }
        if (!simulatorEngine.getStepExecutor().isRunning() && !simulatorEngine.getStepExecutor().isPaused()) {
            try {
                simulatorEngine.loadAndAssemble(codePanel.getCode());
                codePanel.setAddressToLineMap(simulatorEngine.getAddressToLineMap());
                ramMemoryPanel.refresh();
                romMemoryPanel.refresh();
                stackMemoryPanel.refresh();
            } catch (Exception e) {
                DialogFactory.showError(this, "Erreur d'assemblage", e.getMessage());
                return;
            }
        }
        try {
            simulatorEngine.getStepExecutor().stepWithFeedback();
        } catch (RuntimeException e) {
            DialogFactory.showError(this, "Erreur d'exécution", e.getMessage());
            simulatorEngine.stop();
        }
    }

    public void stopSimulation() {
        simulatorEngine.stop();
    }

    /**
     * Met la simulation en pause si elle est en cours d'exécution.
     * Après la pause, on bascule l'affichage mémoire selon le PC courant.
     */
    public void pauseSimulation() {
        try {
            if (simulatorEngine.getStepExecutor().isRunning() && !simulatorEngine.getStepExecutor().isPaused()) {
                simulatorEngine.getStepExecutor().pause();
                int currentPC = simulatorEngine.getCPU().getPC();
                if (currentPC >= Memoire.ROM_START && currentPC <= Memoire.ROM_END) {
                    viewTabbedPane.setSelectedComponent(romMemoryPanel);
                } else if (currentPC >= Memoire.RAM_START && currentPC <= Memoire.RAM_END) {
                    viewTabbedPane.setSelectedComponent(ramMemoryPanel);
                }
            }
        } catch (Exception e) {
            // garde la robustesse en affichant un message au besoin
            System.err.println("Erreur lors de la mise en pause: " + e.getMessage());
        }
    }

    /**
     * Reprend la simulation si elle est en pause.
     * Méthode ajoutée pour être appelée depuis ToolbarPanel.
     */
    public void resumeSimulation() {
        try {
            if (simulatorEngine.getStepExecutor().isPaused()) {
                simulatorEngine.resume();
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la reprise de la simulation: " + e.getMessage());
        }
    }

    public void resetSimulation() {
        if (DialogFactory.showConfirmation(this, "Réinitialiser",
            "Voulez-vous vraiment réinitialiser tout l'état du simulateur (registres, mémoire, breakpoints) ?")) {
            simulatorEngine.resetSimulationState();
            DialogFactory.showSuccess(this, "Réinitialisation terminée",
                "Tous les registres, la mémoire et les breakpoints ont été réinitialisés.");
            codePanel.clearHighlight();
            ramMemoryPanel.clearHighlight();
            romMemoryPanel.clearHighlight();
            stackMemoryPanel.clearHighlight();
            ramMemoryPanel.refresh();
            romMemoryPanel.refresh();
            stackMemoryPanel.refresh();
            viewTabbedPane.setSelectedComponent(cpuPanel);
        }
    }

    private void closeCodeEditor() {
        if (codePanel.isEmpty()) {
            hideCodeEditor();
            return;
        }
        int choice = DialogFactory.showOptions(this,
            "Fermer l'éditeur",
            "Voulez-vous enregistrer votre travail avant de fermer ?",
            "Enregistrer", "Ne pas enregistrer", "Annuler"
        );
        if (choice == 0) {
            if (!saveFile()) return;
        } else if (choice == 2 || choice == -1) {
            return;
        }
        hideCodeEditor();
    }

    private void hideCodeEditor() {
        codePanel.setVisible(false);
        codeVisible = false;
        if (splitPane != null) {
            splitPane.setDividerLocation(0);
        }
        codePanel.clear();
        currentFile = null;
        setTitle("Moto 6809 - Simulateur de Microprocesseur");
        simulatorEngine.resetSimulationState();
        codePanel.clearHighlight();
        ramMemoryPanel.clearHighlight();
        romMemoryPanel.clearHighlight();
        stackMemoryPanel.clearHighlight();
        ramMemoryPanel.refresh();
        romMemoryPanel.refresh();
        stackMemoryPanel.refresh();
        viewTabbedPane.setSelectedComponent(cpuPanel);
        revalidate();
    }

    public SimulatorEngine getSimulatorEngine() {
        return simulatorEngine;
    }

    public void showRegistersPanel() {
        viewTabbedPane.setSelectedComponent(cpuPanel);
    }

    public void showRomMemory() {
        romMemoryPanel.setStartAddress(Memoire.ROM_START);
        romMemoryPanel.setDisplayLength(Memoire.ROM_SIZE);
        viewTabbedPane.setSelectedComponent(romMemoryPanel);
        romMemoryPanel.refresh();
    }

    public void showRamMemory() {
        ramMemoryPanel.setStartAddress(Memoire.RAM_START);
        ramMemoryPanel.setDisplayLength(Memoire.RAM_SIZE);
        viewTabbedPane.setSelectedComponent(ramMemoryPanel);
        ramMemoryPanel.refresh();
    }

    public void showStackMemory() {
        int spAddress = simulatorEngine.getCPU().getS();
        int displayLength = 64;
        int start = (spAddress - displayLength / 2) & 0xFFFF;

        start = Math.max(Memoire.RAM_START, start);
        if (start + displayLength > Memoire.RAM_END + 1) {
            start = Math.max(Memoire.RAM_START, Memoire.RAM_END - displayLength + 1);
        }

        stackMemoryPanel.setStartAddress(start);
        stackMemoryPanel.setDisplayLength(displayLength);
        viewTabbedPane.setSelectedComponent(stackMemoryPanel);
        stackMemoryPanel.refresh();
    }

    public void showHexDecConverter() {
        new HexDecConverterDialog(this).setVisible(true);
    }

    public void showAsciiTable() {
        new AsciiTableDialog(this).setVisible(true);
    }

    public void openCalculator() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                new ProcessBuilder("calc").start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", "-a", "Calculator").start();
            } else {
                new ProcessBuilder("gnome-calculator").start();
            }
        } catch (Exception e) {
            DialogFactory.showError(this, "Erreur",
                "Impossible d'ouvrir la calculatrice du système:\n" + e.getMessage());
        }
    }

    public void showDocumentation() {
        URL docUrl = getClass().getResource("/documentation.pdf");
        if (docUrl != null) {
            try {
                File docFile = new File(docUrl.toURI());
                if (docFile.exists()) {
                    Desktop.getDesktop().open(docFile);
                } else {
                    File tempFile = File.createTempFile("documentation", ".pdf");
                    tempFile.deleteOnExit();

                    try (InputStream in = docUrl.openStream();
                         FileOutputStream out = new FileOutputStream(tempFile)) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                    Desktop.getDesktop().open(tempFile);
                }
            } catch (IOException | URISyntaxException e) {
                DialogFactory.showError(this, "Erreur", "Impossible d'ouvrir la documentation PDF:\n" + e.getMessage());
            } catch (IllegalArgumentException e) {
                DialogFactory.showError(this, "Erreur", "La fonction d'ouverture de fichier n'est pas supportée par votre système.");
            }
        } else {
            DialogFactory.showError(this, "Erreur", "Ressource PDF 'documentation.pdf' introuvable dans le JAR ou les chemins de classe.");
        }
    }

    public void showInstructionSetDialog() {
        new InstructionSetDialog(this).setVisible(true);
    }

    public void showAboutDialog() {
        new AboutDialog(this).setVisible(true);
    }

    public void toggleBreakpoint(int address) {
        if (simulatorEngine.getStepExecutor().hasBreakpoint(address)) {
            simulatorEngine.getStepExecutor().removeBreakpoint(address);
        } else {
            simulatorEngine.getStepExecutor().addBreakpoint(address);
        }
        codePanel.toggleBreakpointHighlight(address, simulatorEngine.getStepExecutor().hasBreakpoint(address));
    }

    public void toggleCodeEditor() {
        codeVisible = !codeVisible;
        codePanel.setVisible(codeVisible);
        if (splitPane != null) {
            if (codeVisible) {
                splitPane.setDividerLocation(DEFAULT_CODE_EDITOR_WIDTH);
            } else {
                splitPane.setDividerLocation(0);
            }
        }
        revalidate();
        repaint();
    }

    public void showThemeOptionsDialog() {
        new ThemeOptionsDialog(this).setVisible(true);
    }

    public void clearBreakpoints() {
        simulatorEngine.getStepExecutor().clearBreakpoints();
        codePanel.clearAllBreakpointHighlights();
    }
}
