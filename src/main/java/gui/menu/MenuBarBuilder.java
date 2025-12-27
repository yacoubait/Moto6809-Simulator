package gui.menu;

import gui.Moto6809;
import gui.theme.RoundedBorder;
import gui.theme.Theme;
import gui.dialogs.DialogFactory; 
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public final class MenuBarBuilder {
    
    private MenuBarBuilder() {} 
    
    public static JMenuBar build(Moto6809 frame) {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setOpaque(true); 
        menuBar.setBackground(Theme.MENU_BAR); 
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDURE)); 
        
        UIManager.put("PopupMenu.border", new RoundedBorder(Theme.BORDURE, 1, 12, new Insets(2, 2, 2, 2))); 
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

        menuBar.add(createFileMenu(frame));
        menuBar.add(createSimulationMenu(frame));
        menuBar.add(createToolsMenu(frame));
        menuBar.add(createWindowMenu(frame)); 
        menuBar.add(createOptionsMenu(frame)); 
        menuBar.add(createHelpMenu(frame)); 
        
        return menuBar;
    }
    
    private static JMenu createMenu(String name) {
        JMenu menu = new JMenu(name);
        return menu;
    }
    
    private static JMenu createFileMenu(Moto6809 frame) {
        JMenu menu = createMenu("Fichier");
        
        JMenuItem nouveau = new MenuItem("Nouveau", frame::newFile, KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK);
        JMenuItem ouvrir = new MenuItem("Ouvrir...", frame::openFile, KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK);
        JMenuItem sauvegarder = new MenuItem("Sauvegarder", frame::saveFile, KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK);
        JMenuItem sauvegarderSous = new MenuItem("Sauvegarder sous...", frame::saveFileAs, KeyEvent.VK_S, InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK);
        
        JMenuItem quitter = new MenuItem("Quitter", () -> System.exit(0), KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK);
        
        menu.add(nouveau);
        menu.add(ouvrir);
        menu.add(sauvegarder);
        menu.add(sauvegarderSous);
        menu.addSeparator(); 
        menu.add(quitter);
        
        return menu;
    }
    
    private static JMenu createSimulationMenu(Moto6809 frame) {
        JMenu menu = createMenu("Simulation");
        
        JMenuItem executer = new MenuItem("Exécuter", frame::executeCode, KeyEvent.VK_F5, 0); 
        JMenuItem pasAPas = new MenuItem("Pas à pas", frame::stepCode, KeyEvent.VK_F6, 0); 
        JMenuItem pause = new MenuItem("Pause", frame::pauseSimulation, KeyEvent.VK_F7, 0);
        JMenuItem reprendre = new MenuItem("Reprendre", frame::resumeSimulation, KeyEvent.VK_F8, 0);
        JMenuItem stop = new MenuItem("Arrêter", frame::stopSimulation, KeyEvent.VK_F9, 0);
        
        JMenuItem resetSim = new MenuItem("Réinitialiser simulateur", frame::resetSimulation, KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK);
        
        menu.add(executer);
        menu.add(pasAPas);
        menu.add(pause);
        menu.add(reprendre);
        menu.add(stop);
        menu.addSeparator();
        menu.add(resetSim);
        menu.addSeparator();
        
        JMenu breakpointsMenu = createMenu("Breakpoints");
        JMenuItem addBreakpoint = new MenuItem("Ajouter/Supprimer à l'adresse actuelle du PC", () -> {
            if (frame.getSimulatorEngine().getCPU() != null) {
                int currentPC = frame.getSimulatorEngine().getCPU().getPC();
                frame.toggleBreakpoint(currentPC);
            } else {
                DialogFactory.showInfo(frame, "Information", "Le simulateur n'est pas initialisé ou le PC est inconnu.");
            }
        });
        JMenuItem clearBreakpoints = new MenuItem("Effacer tous les breakpoints", frame::clearBreakpoints);
        breakpointsMenu.add(addBreakpoint);
        breakpointsMenu.add(clearBreakpoints);
        menu.add(breakpointsMenu);

        return menu;
    }
    
    private static JMenu createToolsMenu(Moto6809 frame) {
        JMenu menu = createMenu("Outils");
        
        JMenuItem hexDec = new MenuItem("Convertisseur Hex/Dec", frame::showHexDecConverter);
        JMenuItem ascii = new MenuItem("Table ASCII", frame::showAsciiTable);
        JMenuItem calc = new MenuItem("Calculatrice", frame::openCalculator);
        
        menu.add(hexDec);
        menu.add(ascii);
        menu.add(calc);
        
        return menu;
    }
    
    private static JMenu createWindowMenu(Moto6809 frame) {
        JMenu menu = createMenu("Fenêtre");
        
        JMenuItem toggleEditor = new MenuItem("Afficher/Masquer Éditeur de code", frame::toggleCodeEditor, KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK);

        // <<< MODIFICATION: Les actions du menu Fenêtre appellent maintenant des méthodes qui changent l'onglet sélectionné
        menu.add(toggleEditor);
        menu.addSeparator();
        menu.add(new MenuItem("Registres CPU", frame::showRegistersPanel)); 
        menu.add(new MenuItem("Mémoire RAM", frame::showRamMemory)); 
        menu.add(new MenuItem("Mémoire ROM", frame::showRomMemory)); 
        menu.add(new MenuItem("Pile (Stack)", frame::showStackMemory)); 

        return menu;
    }
    
    private static JMenu createOptionsMenu(Moto6809 frame) {
        JMenu menu = createMenu("Options");
        menu.add(new MenuItem("Thème...", frame::showThemeOptionsDialog));
        return menu;
    }
    
    private static JMenu createHelpMenu(Moto6809 frame) {
        JMenu menu = createMenu("Aide");
        menu.add(new MenuItem("Documentation", frame::showDocumentation));    
        menu.add(new MenuItem("Jeu d'instructions", frame::showInstructionSetDialog)); 
        menu.addSeparator();
        menu.add(new MenuItem("À propos", frame::showAboutDialog));         
        return menu;
    }

    private static class MenuItem extends JMenuItem {
        public MenuItem(String text, Runnable action) {
            super(text);
            addActionListener(e -> action.run());
        }

        public MenuItem(String text, Runnable action, int keyCode, int modifiers) {
            super(text);
            addActionListener(e -> action.run());
            setAccelerator(KeyStroke.getKeyStroke(keyCode, modifiers));
        }
    }
}
