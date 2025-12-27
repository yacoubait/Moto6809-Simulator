package exec;

import cpu.CPU6809;
import mem.Memoire;
import java.util.HashSet;
import java.util.Set;
import gui.dialogs.DialogFactory; // Assurez-vous que ce package et cette classe existent
import javax.swing.SwingUtilities;
import javax.swing.JFrame; // Nécessaire si DialogFactory utilise JFrame pour les parents

/**
 * Gestionnaire d'exécution pas-à-pas avec support des breakpoints.
 */
public class StepExecutor {
    
    private final CPU6809 cpu;
    private final Memoire mem;
    private final UniteExecution exec;
    
    private final Set<Integer> breakpoints = new HashSet<>();
    private volatile boolean running = false;
    private volatile boolean paused = false;
    
    private int instructionsExecuted = 0;
    private int maxInstructions = 100000;
    
    private int adresseDebut;
    private int adresseFin;
    private JFrame parentFrame;

    // --- CORRECTION : Numéro de ligne de la directive END ---
    private int endDirectiveLineNumber; // Ligne à surligner à la fin du programme

    // ===== CORRECTION: Exception personnalisée pour fin de programme =====
    public static class ProgramTerminatedException extends RuntimeException {
        private final int finalPC;
        private final int instructionsExecuted;
        // --- CORRECTION : Numéro de ligne de END dans l'exception ---
        private final int endLineNumber; 
        
        public ProgramTerminatedException(int finalPC, int instructionsExecuted, int endLineNumber) {
            super("Programme terminé");
            this.finalPC = finalPC;
            this.instructionsExecuted = instructionsExecuted;
            this.endLineNumber = endLineNumber; // Stocke la ligne de END
        }
        
        public int getFinalPC() { return finalPC; }
        public int getInstructionsExecuted() { return instructionsExecuted; }
        public int getEndLineNumber() { return endLineNumber; } // Getter pour la ligne de END
    }

    public StepExecutor(CPU6809 cpu, Memoire mem, UniteExecution exec) {
        this.cpu = cpu;
        this.mem = mem;
        this.exec = exec;
        this.endDirectiveLineNumber = -1; // Initialisation
    }

    public void setParentFrame(JFrame parentFrame) {
        this.parentFrame = parentFrame;
    }

    /**
     * Exécute une seule instruction à l'adresse actuelle du PC.
     * @throws ProgramTerminatedException si le programme est terminé (fin normale)
     * @throws RuntimeException si une vraie erreur se produit
     */
    public void step() {
        if (instructionsExecuted >= maxInstructions) {
            stop(); 
            throw new RuntimeException("Limite d'instructions atteinte (" + maxInstructions + ")");
        }
        
        int pcAvant = cpu.getPC();
        
        // ===== CORRECTION: Distinguer fin normale vs vraie erreur =====
        if (pcAvant >= adresseFin) {
            // Fin normale du programme - PC a atteint ou dépassé la fin
            stop();
            // --- CORRECTION : Passe le numéro de ligne de END à l'exception ---
            throw new ProgramTerminatedException(pcAvant, instructionsExecuted, endDirectiveLineNumber);
        }
        
        if (pcAvant < adresseDebut) {
            // Vraie erreur - PC est avant le début du programme
            stop(); 
            throw new RuntimeException(String.format(
                "Erreur: PC ($%04X) est avant le début du programme ($%04X)", 
                pcAvant, adresseDebut
            ));
        }
        
        exec.executerInstruction();
        instructionsExecuted++;
        
        System.out.printf("Step #%d: PC $%04X -> $%04X%n", 
            instructionsExecuted, pcAvant, cpu.getPC());
    }

    /**
     * Lance l'exécution continue de la simulation sur un thread séparé.
     */
    public void run() {
        if (running && !paused) {
            System.out.println("L'exécution est déjà en cours.");
            return;
        }
        
        running = true;
        paused = false;
        
        new Thread(() -> {
            while (running && !paused) {
                try {
                    if (breakpoints.contains(cpu.getPC())) {
                        System.out.printf("Breakpoint atteint à $%04X%n", cpu.getPC());
                        SwingUtilities.invokeLater(() -> 
                            DialogFactory.showInfo(parentFrame, "Breakpoint", 
                                String.format("Breakpoint atteint à l'adresse $%04X", cpu.getPC()))
                        );
                        pause();
                        break;
                    }
                    
                    step();
                    
                } catch (ProgramTerminatedException e) {
                    // ===== CORRECTION : FIN NORMALE: Message de succès (identique à stepWithFeedback) =====
                    final int finalPC = e.getFinalPC();
                    final int totalInstructions = e.getInstructionsExecuted();
                    final int endLine = e.getEndLineNumber();
                    SwingUtilities.invokeLater(() -> {
                        DialogFactory.showSuccess(parentFrame, "Exécution terminée", 
                            "✓ Toutes les instructions ont été exécutées avec succès !\n\n" +
                            "• Instructions exécutées : " + totalInstructions + "\n" +
                            "• PC final : $" + String.format("%04X", finalPC) +
                            (endLine != -1 ? "\n• Fin du programme à la ligne : " + endLine : "") // Ajoute l'info si disponible
                        );
                        // ICI : L'UI devrait surligner 'endLine'
                    });
                    stop();
                    break;
                    
                } catch (RuntimeException e) {
                    // ===== VRAIE ERREUR: Message d'erreur =====
                    System.err.println("Erreur d'exécution: " + e.getMessage());
                    SwingUtilities.invokeLater(() -> 
                        DialogFactory.showError(parentFrame, "Erreur d'exécution", e.getMessage())
                    );
                    stop();
                    break;
                }
                
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    stop();
                    break;
                }
            }
        }).start();
    }

    /**
     * Exécute une seule instruction (pas-à-pas manuel depuis l'UI).
     * Affiche les messages appropriés.
     */
    public void stepWithFeedback() {
        try {
            step(); // Tente d'exécuter l'instruction
        } catch (ProgramTerminatedException e) {
            // ===== CORRECTION : Gérer explicitement ProgramTerminatedException comme un succès =====
            final int finalPC = e.getFinalPC();
            final int totalInstructions = e.getInstructionsExecuted();
            final int endLine = e.getEndLineNumber();
            SwingUtilities.invokeLater(() -> {
                DialogFactory.showSuccess(parentFrame, "Exécution terminée", 
                    "✓ Toutes les instructions ont été exécutées avec succès !\n\n" +
                    "• Instructions exécutées : " + totalInstructions + "\n" +
                    "• PC final : $" + String.format("%04X", finalPC) +
                    (endLine != -1 ? "\n• Fin du programme à la ligne : " + endLine : "")
                );
                // ICI : L'UI devrait surligner 'endLine'
            });
            // Stop l'exécution ici si nécessaire, bien que stepWithFeedback soit pour un pas unique.
            // Si le programme est terminé, il ne devrait plus y avoir d'autres steps possibles.
            stop(); 
        } catch (RuntimeException e) {
            // ===== VRAIE ERREUR: Message d'erreur (pour toutes les autres RuntimeException) =====
            System.err.println("Erreur d'exécution: " + e.getMessage());
            SwingUtilities.invokeLater(() -> 
                DialogFactory.showError(parentFrame, "Erreur d'exécution", e.getMessage())
            );
            stop(); // Arrête la simulation en cas de vraie erreur
        }
    }

    /**
     * Exécute jusqu'à une adresse cible.
     */
    public void runUntil(int targetAddress) {
        if (running && !paused) {
            System.out.println("L'exécution est déjà en cours.");
            return;
        }
        
        running = true;
        paused = false;
        
        new Thread(() -> {
            while (running && !paused && cpu.getPC() != targetAddress) {
                try {
                    if (breakpoints.contains(cpu.getPC())) {
                        System.out.printf("Breakpoint atteint à $%04X avant la cible $%04X%n", 
                            cpu.getPC(), targetAddress);
                        SwingUtilities.invokeLater(() -> 
                            DialogFactory.showInfo(parentFrame, "Breakpoint", 
                                String.format("Breakpoint atteint à $%04X\n(avant la cible $%04X)", 
                                    cpu.getPC(), targetAddress))
                        );
                        pause();
                        break;
                    }
                    
                    step();
                    
                } catch (ProgramTerminatedException e) {
                    // Fin normale avant d'atteindre la cible
                    final int finalPC = e.getFinalPC();
                    final int totalInstructions = e.getInstructionsExecuted();
                    final int endLine = e.getEndLineNumber();
                    SwingUtilities.invokeLater(() -> {
                        DialogFactory.showSuccess(parentFrame, "Exécution terminée", 
                            "Le programme s'est terminé avant d'atteindre l'adresse cible.\n\n" +
                            "• Cible : $" + String.format("%04X", targetAddress) + "\n" +
                            "• PC final : $" + String.format("%04X", finalPC) + "\n" +
                            "• Instructions exécutées : " + totalInstructions +
                            (endLine != -1 ? "\n• Fin du programme à la ligne : " + endLine : "")
                        );
                        // ICI : L'UI devrait surligner 'endLine'
                    });
                    stop();
                    break;
                    
                } catch (RuntimeException e) {
                    System.err.println("Erreur d'exécution: " + e.getMessage());
                    SwingUtilities.invokeLater(() -> 
                        DialogFactory.showError(parentFrame, "Erreur d'exécution", e.getMessage())
                    );
                    stop();
                    break;
                }
                
                try {
                    Thread.sleep(10); 
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    stop();
                    break;
                }
            }
            
            if (cpu.getPC() == targetAddress && running) {
                System.out.printf("Adresse cible atteinte: $%04X%n", targetAddress);
                SwingUtilities.invokeLater(() -> 
                    DialogFactory.showSuccess(parentFrame, "Cible atteinte", 
                        String.format("Adresse cible atteinte : $%04X\n\n" +
                            "• Instructions exécutées : %d", targetAddress, instructionsExecuted))
                );
                pause();
            }
        }).start();
    }

    public void stop() {
        running = false;
        paused = false; 
        System.out.println("Exécution arrêtée");
    }

    public void pause() {
        paused = true;
        System.out.println("Exécution en pause");
    }

    public void resume() {
        if (paused) {
            paused = false;
            System.out.println("Reprise de l'exécution...");
            run();
        } else {
            System.out.println("L'exécution n'est pas en pause.");
        }
    }

    public void reset() {
        instructionsExecuted = 0;
        running = false;
        paused = false;
        clearBreakpoints();
        this.endDirectiveLineNumber = -1; // Réinitialiser la ligne de END
        System.out.println("Exécuteur réinitialisé.");
    }

    // ==================== GESTION DES BREAKPOINTS ====================

    public void addBreakpoint(int address) {
        breakpoints.add(address & 0xFFFF);
        System.out.printf("Breakpoint ajouté à $%04X%n", address & 0xFFFF);
    }

    public void removeBreakpoint(int address) {
        if (breakpoints.remove(address & 0xFFFF)) {
            System.out.printf("Breakpoint retiré à $%04X%n", address & 0xFFFF);
        }
    }

    public void clearBreakpoints() {
        breakpoints.clear();
        System.out.println("Tous les breakpoints retirés");
    }

    public boolean hasBreakpoint(int address) {
        return breakpoints.contains(address & 0xFFFF);
    }

    public Set<Integer> getBreakpoints() {
        return new HashSet<>(breakpoints);
    }

    // ==================== GETTERS / SETTERS ====================

    public boolean isRunning() { return running; }
    public boolean isPaused() { return paused; }
    public int getInstructionsExecuted() { return instructionsExecuted; }
    
    public void setMaxInstructions(int max) {
        if (max > 0) this.maxInstructions = max;
    }
    
    public int getMaxInstructions() { return maxInstructions; }
    public void setAdresseDebut(int adresse) { this.adresseDebut = adresse & 0xFFFF; }
    public void setAdresseFin(int adresse) { this.adresseFin = adresse & 0xFFFF; }
    public int getAdresseDebut() { return adresseDebut; }
    public int getAdresseFin() { return adresseFin; }

    // --- CORRECTION : Setter pour la ligne de END ---
    public void setEndDirectiveLineNumber(int endDirectiveLineNumber) {
        this.endDirectiveLineNumber = endDirectiveLineNumber;
    }
}
