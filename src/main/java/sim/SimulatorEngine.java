package sim;

import asm.Assembleur;
import asm.Instruction; // <<< NOUVEL IMPORT
import asm.TableOpcodes; // <<< NOUVEL IMPORT
import cpu.CPU6809;
import exec.UniteExecution;
import exec.StepExecutor;
import mem.Memoire;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set; 
import java.util.regex.Matcher; 
import java.util.regex.Pattern; 
import java.util.stream.Collectors;

/**
 * Moteur principal de simulation du 6809.
 * Orchestre l'assembleur, le CPU, la mémoire et l'unité d'exécution.
 * Fournit une API pour l'interface graphique pour contrôler la simulation.
 */
public class SimulatorEngine {
    private final CPU6809 cpu;
    private final Memoire mem;
    private final Assembleur assembleur; 
    private final UniteExecution uniteExecution; 
    private final StepExecutor stepExecutor; 

    private List<String> currentSourceCodeLines; 
    private Map<Integer, Integer> addressToLineMap; 
    private Map<Integer, Integer> instructionSizesMap; // <<< NOUVEAU: Map pour stocker la taille des instructions

    // Pattern pour extraire les labels (pour les ignorer lors de la recherche d'opérandes)
    private static final Pattern LABEL_PATTERN = Pattern.compile("^\\s*([A-Za-z_][A-Za-z0-9_]*):.*"); 

    public SimulatorEngine() {
        this.cpu = new CPU6809();
        this.mem = new Memoire();
        this.cpu.setMemoire(mem); 
        this.assembleur = new Assembleur(mem); 
        this.uniteExecution = new UniteExecution(cpu, mem);
        this.stepExecutor = new StepExecutor(cpu, mem, uniteExecution);

        this.stepExecutor.setAdresseDebut(0x0000);
        this.stepExecutor.setAdresseFin(0xFFFF); 
        
        this.currentSourceCodeLines = new ArrayList<>();
        this.addressToLineMap = new HashMap<>();
        this.instructionSizesMap = new HashMap<>(); // <<< NOUVEAU: Initialisation
    }

    /**
     * Charge et assemble le code source fourni en mémoire.
     * @param sourceCode Le code assembleur sous forme de chaîne.
     * @return true si l'assemblage est réussi, false sinon.
     * @throws Exception En cas d'erreur d'assemblage.
     */
    public boolean loadAndAssemble(String sourceCode) throws Exception {
        resetSimulationState(); // Réinitialiser le CPU et la mémoire avant le nouvel assemblage

        // Convertir la chaîne en liste de lignes pour l'assembleur
        currentSourceCodeLines = new BufferedReader(new StringReader(sourceCode))
                                    .lines()
                                    .collect(Collectors.toList());

        // Assemblage en deux passes
        try {
            assembleur.firstPass(currentSourceCodeLines);
            addressToLineMap = assembleur.secondPass(currentSourceCodeLines); // Récupère la map adresse->ligne

            // <<< NOUVEAU: Après la deuxième passe, parcourir les adresses pour déterminer les tailles d'instructions
            generateInstructionSizesMap(currentSourceCodeLines, addressToLineMap);

            // Vérifier si la directive END a été trouvée
            if (!assembleur.isEndDirectiveFound()) {
                throw new Exception("Erreur d'assemblage: La directive 'END' est manquante à la fin du programme. Le programme ne peut pas être exécuté.");
            }

            // Mettre à jour l'adresse de départ du PC et la zone d'exécution
            int startPC = assembleur.getAdresseDepart();
            cpu.setPC(startPC);
            stepExecutor.setAdresseDebut(startPC);
            // L'adresse de fin est l'adresseCourante de l'assembleur après le break (donc juste après le code)
            stepExecutor.setAdresseFin(assembleur.getAdresseCourante()); 
            // Transférer le numéro de ligne de END au StepExecutor
            stepExecutor.setEndDirectiveLineNumber(assembleur.getEndDirectiveLineNumber());
            
            System.out.println("Assemblage réussi. PC initialisé à: $" + String.format("%04X", startPC));
            return true;
        } catch (Exception e) {
            System.err.println("Erreur d'assemblage: " + e.getMessage());
            // Relancer l'exception pour l'UI, potentiellement avec un message plus amical.
            throw new Exception("Échec de l'assemblage: " + e.getMessage(), e); 
        }
    }

    /**
     * (NOUVEAU) Génère la map des tailles d'instructions en re-parcourant le code source.
     * C'est une passe légère qui utilise l'Instruction.estimerTaille qui est plus précise que
     * la logique dans MemoryPanel.
     */
    private void generateInstructionSizesMap(List<String> sourceLines, Map<Integer, Integer> addrToLineMap) throws Exception {
        instructionSizesMap.clear();
        int currentAddress = assembleur.getAdresseDepart(); // On commence à l'adresse de départ du programme

        // Tri des adresses mappées pour les parcourir dans l'ordre
        List<Integer> sortedAddresses = new ArrayList<>(addrToLineMap.keySet());
        sortedAddresses.sort(Integer::compare);

        // Une map temporaire pour les lignes sans adresse explicite (labels)
        Map<Integer, String> lineContent = new HashMap<>();
        for (int i = 0; i < sourceLines.size(); i++) {
            lineContent.put(i + 1, sourceLines.get(i));
        }

        for (int addr : sortedAddresses) {
            int lineNumber = addrToLineMap.get(addr);
            String line = lineContent.get(lineNumber);

            String originalLigne = line;
            line = line.trim();
            if (line.isEmpty() || line.startsWith(";")) {
                continue;
            }

            // Enlever les commentaires
            int posComment = line.indexOf(';');
            if (posComment > 0) {
                line = line.substring(0, posComment).trim();
            }

            // Enlever les labels
            Matcher labelMatcher = LABEL_PATTERN.matcher(originalLigne);
            if (labelMatcher.matches()) {
                line = line.substring(originalLigne.indexOf(':') + 1).trim();
                if (line.isEmpty()) {
                    continue;
                }
            }

            // Si c'est une directive, sa taille est déjà gérée par l'assembleur
            // Pour le highlight, nous voulons la taille de l'opcode/operande.
            // Les directives n'ont pas de "taille d'instruction" au sens où on l'entend ici.
            // On peut les ignorer ou leur assigner une taille 0 pour le highlight.
            if (line.toUpperCase().startsWith("ORG") || line.toUpperCase().startsWith("EQU") ||
                line.toUpperCase().startsWith("FCC") || line.toUpperCase().startsWith("FCB") ||
                line.toUpperCase().startsWith("FDB") || line.toUpperCase().startsWith("RMB") ||
                line.toUpperCase().startsWith("SETDP") || line.toUpperCase().startsWith("END")) {
                // Pour ces directives, on ne stocke pas de taille d'instruction "exécutable"
                // On peut juste ignorer ou mettre 0, mais elles n'ont pas d'opcode
                continue;
            }
            
            String[] parts = line.split("\\s+", 2);
            if (parts.length == 0 || parts[0].isEmpty()) continue;
            
            String mnemonic = parts[0].toUpperCase();
            String operand = (parts.length > 1) ? parts[1].trim() : "";

            int size = Instruction.estimerTaille(mnemonic, operand); // Utilise la méthode d'estimation de taille
            instructionSizesMap.put(addr, size);
        }
    }


    /**
     * Exécute une seule instruction.
     * @throws exec.StepExecutor.ProgramTerminatedException si une fin normale de programme survient.
     * @throws RuntimeException si une erreur d'exécution survient.
     */
    public void step() {
        stepExecutor.step();
    }

    /**
     * Lance l'exécution continue de la simulation.
     */
    public void run() {
        stepExecutor.run();
    }

    /**
     * Met en pause l'exécution de la simulation.
     */
    public void pause() {
        stepExecutor.pause();
    }

    /**
     * Reprend l'exécution de la simulation.
     */
    public void resume() {
        stepExecutor.resume();
    }

    /**
     * Arrête l'exécution de la simulation.
     */
    public void stop() {
        stepExecutor.stop();
    }

    /**
     * Réinitialise le CPU, la mémoire et l'état de l'exécuteur.
     */
    public void resetSimulationState() {
        cpu.reset();
        mem.reset(); 
        assembleur.getTableSymboles().effacer(); 
        stepExecutor.reset(); 
        currentSourceCodeLines = new ArrayList<>(); 
        addressToLineMap = new HashMap<>(); 
        instructionSizesMap = new HashMap<>(); // <<< NOUVEAU: Réinitialisation
        System.out.println("Simulateur réinitialisé.");
    }

    // ==================== Gestion des Breakpoints ====================

    /**
     * Ajoute ou supprime un breakpoint à une adresse donnée.
     * @param address L'adresse mémoire du breakpoint.
     */
    public void toggleBreakpoint(int address) {
        if (stepExecutor.hasBreakpoint(address)) {
            stepExecutor.removeBreakpoint(address);
        } else {
            stepExecutor.addBreakpoint(address);
        }
    }

    /**
     * Vérifie si un breakpoint existe à une adresse donnée.
     * @param address L'adresse mémoire à vérifier.
     * @return true si un breakpoint existe, false sinon.
     */
    public boolean hasBreakpoint(int address) {
        return stepExecutor.hasBreakpoint(address);
    }

    /**
     * Efface tous les breakpoints définis.
     */
    public void clearBreakpoints() {
        stepExecutor.clearBreakpoints();
    }

    /**
     * Retourne la partie opérande de l'instruction à l'adresse mémoire donnée.
     * Cette méthode est utilisée pour afficher l'opérande dans l'interface graphique.
     * @param address L'adresse mémoire de l'instruction.
     * @return La chaîne représentant l'opérande, ou "----" si non trouvée ou sans opérande.
     */
    public String getOperandForAddress(int address) {
        // Trouver le numéro de ligne de source correspondant à l'adresse
        Integer lineNumber = addressToLineMap.get(address);
        if (lineNumber == null || lineNumber < 1 || lineNumber > currentSourceCodeLines.size()) {
            return "----"; // Adresse non mappée à une ligne de code
        }

        // Récupérer la ligne de code (lineNumber est 1-indexé)
        String line = currentSourceCodeLines.get(lineNumber - 1);
        
        // Nettoyer la ligne pour extraire l'opérande
        String cleanedLine = line;

        // 1. Enlever les commentaires
        int commentPos = cleanedLine.indexOf(';');
        if (commentPos != -1) {
            cleanedLine = cleanedLine.substring(0, commentPos);
        }
        
        // 2. Enlever les labels (s'il y en a un au début de la ligne)
        Matcher labelMatcher = LABEL_PATTERN.matcher(cleanedLine);
        if (labelMatcher.matches()) {
            cleanedLine = cleanedLine.substring(labelMatcher.end()).trim();
        } else {
            cleanedLine = cleanedLine.trim(); // Juste trimmer s'il n'y a pas de label
        }

        // 3. Séparer mnémonique et opérande
        String[] parts = cleanedLine.split("\\s+", 2);
        if (parts.length > 1 && !parts[1].trim().isEmpty()) {
            return parts[1].trim(); // Retourne l'opérande
        } else {
            return ""; // Pas d'opérande ou opérande vide
        }
    }

    /**
     * (NOUVEAU) Retourne la taille de l'instruction à une adresse donnée.
     * @param address L'adresse de l'instruction.
     * @return La taille de l'instruction en octets, ou 1 par défaut si non trouvée.
     */
    public int getInstructionSize(int address) {
        return instructionSizesMap.getOrDefault(address, 1);
    }

    // ==================== Getters pour les composants internes ====================
    
    public CPU6809 getCPU() {
        return cpu;
    }

    public Memoire getMemoire() {
        return mem;
    }

    public Assembleur getAssembleur() {
        return assembleur;
    }

    public StepExecutor getStepExecutor() {
        return stepExecutor;
    }

    public Map<Integer, Integer> getAddressToLineMap() {
        return addressToLineMap;
    }

    public List<String> getCurrentSourceCodeLines() {
        return currentSourceCodeLines;
    }
}
