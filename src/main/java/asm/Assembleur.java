package asm;

import mem.Memoire;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Assembleur {
    private final Memoire mem;
    private int adresseCourante;
    private int adresseDepart; 
    private final TableSymboles symboles;
    private Map<Integer, Integer> adresseToLigneSource; 

    private boolean endDirectiveFound;
    private int endDirectiveLineNumber; 

    private static final Pattern LABEL_PATTERN = Pattern.compile("^\\s*([A-Za-z_][A-Za-z0-9_]*):.*");

    public Assembleur(Memoire mem) {
        this.mem = mem;
        this.adresseDepart = Memoire.ROM_START; 
        this.adresseCourante = this.adresseDepart;
        this.symboles = new TableSymboles();
        this.adresseToLigneSource = new HashMap<>();
        this.endDirectiveFound = false; 
        this.endDirectiveLineNumber = -1; 
    }

    public void firstPass(List<String> sourceLines) throws Exception {
        symboles.effacer(); 
        this.adresseDepart = Memoire.ROM_START; 
        this.adresseCourante = Memoire.ROM_START; 
        this.endDirectiveFound = false; 
        this.endDirectiveLineNumber = -1; 

        for (int i = 0; i < sourceLines.size(); i++) {
            String ligne = sourceLines.get(i);
            int numeroLigne = i + 1; 

            String originalLigne = ligne; 

            ligne = ligne.trim();
            if (ligne.isEmpty() || ligne.startsWith(";")) {
                continue; 
            }

            int posComment = ligne.indexOf(';');
            if (posComment > 0) {
                ligne = ligne.substring(0, posComment).trim();
            }

            Matcher labelMatcher = LABEL_PATTERN.matcher(originalLigne);
            if (labelMatcher.matches()) {
                String label = labelMatcher.group(1);
                if (!label.isEmpty()) {
                    symboles.definirLabel(label, adresseCourante);
                }
                ligne = ligne.substring(originalLigne.indexOf(':') + 1).trim();
                if (ligne.isEmpty()) {
                    continue; 
                }
            }

            if (traiterDirective(ligne, true, numeroLigne)) { 
                if (ligne.toUpperCase().trim().equals("END")) {
                    this.endDirectiveFound = true; 
                    this.endDirectiveLineNumber = numeroLigne; 
                    break; 
                }
                continue; 
            }

            String[] parties = ligne.split("\\s+", 2);
            if (parties.length == 0 || parties[0].isEmpty()) continue;
            
            String mnemonic = parties[0].toUpperCase();
            String operand = (parties.length > 1) ? parties[1].trim() : "";

            int tailleInstr;
            try {
                tailleInstr = Instruction.estimerTaille(mnemonic, operand);
            } catch (IllegalArgumentException e) {
                throw new Exception("Erreur d'assemblage (Passe 1): " + e.getMessage() + " à la ligne " + numeroLigne + " (" + originalLigne + ")");
            }

            if (tailleInstr > 0) {
                adresseCourante += tailleInstr;
            } else {
                 throw new Exception("Erreur d'assemblage (Passe 1): Mnémonique inconnue ou mode non géré pour estimer la taille: '" + mnemonic + "' à la ligne " + numeroLigne + " (" + originalLigne + ")");
            }
        }
    }

    public Map<Integer, Integer> secondPass(List<String> sourceLines) throws Exception {
        this.adresseCourante = this.adresseDepart; 
        this.adresseToLigneSource.clear(); 
        
        for (int i = 0; i < sourceLines.size(); i++) {
            String ligne = sourceLines.get(i);
            int numeroLigne = i + 1; 

            int adresseDebutInstruction = adresseCourante; 

            String originalLigne = ligne; 
            ligne = ligne.trim();
            if (ligne.isEmpty() || ligne.startsWith(";")) {
                continue; 
            }

            int posComment = ligne.indexOf(';');
            if (posComment > 0) {
                ligne = ligne.substring(0, posComment).trim();
            }

            Matcher labelMatcher = LABEL_PATTERN.matcher(originalLigne);
            if (labelMatcher.matches()) {
                ligne = ligne.substring(originalLigne.indexOf(':') + 1).trim();
                if (ligne.isEmpty()) {
                    continue;
                }
            }
            
            boolean isDirective = traiterDirective(ligne, false, numeroLigne); 
            if (isDirective) {
                if (ligne.toUpperCase().trim().equals("END")) {
                    break; 
                }
                continue; 
            }

            String[] parties = ligne.split("\\s+", 2);
            if (parties.length == 0 || parties[0].isEmpty()) {
                throw new Exception("Erreur d'assemblage (Passe 2): Ligne vide ou format invalide à la ligne " + numeroLigne + " (" + originalLigne + ")");
            }
            
            String mnemonic = parties[0].toUpperCase();
            String operand = (parties.length > 1) ? parties[1].trim() : "";

            operand = resoudreSymboles(operand);

            if (!TableOpcodes.existe(mnemonic)) {
                throw new Exception("Erreur d'assemblage (Passe 2): Mnémonique inconnue: '" + mnemonic + "' à la ligne " + numeroLigne + " (" + originalLigne + ")");
            }

            Instruction instr = new Instruction(mnemonic, operand);
            
            if (!traiterInstruction(instr, false, numeroLigne)) { 
                throw new Exception("Erreur d'assemblage (Passe 2): Impossible d'assembler l'instruction: '" + ligne + "' à la ligne " + numeroLigne + " (" + originalLigne + ")");
            }

            ecrireEnMemoireRom(instr); 
            
            adresseToLigneSource.put(adresseDebutInstruction, numeroLigne);
        }
        
        afficherSymboles(); 
        return adresseToLigneSource;
    }

    private boolean traiterDirective(String ligne, boolean isFirstPass, int numeroLigne) throws Exception {
        String upper = ligne.toUpperCase().trim();

        if (upper.startsWith("ORG")) {
            String operand = ligne.substring(3).trim();
            Integer addr = symboles.resoudre(operand); 
            if (addr != null) {
                adresseCourante = addr;
                if (isFirstPass) { 
                    this.adresseDepart = addr;
                }
                System.out.printf("ORG $%04X (Ligne %d)%n", addr, numeroLigne);
            } else {
                throw new Exception("Erreur d'assemblage (Passe " + (isFirstPass ? "1" : "2") + "): Adresse ORG invalide ou symbole non résolu: '" + operand + "' à la ligne " + numeroLigne);
            }
            return true;
        }

        if (upper.contains(" EQU ")) {
            String[] parts = ligne.split("\\s+");
            if (parts.length >= 3) {
                String nom = parts[0].trim();
                String valeurStr = parts[2].trim();
                Integer valeur = symboles.resoudre(valeurStr); 
                if (valeur != null) {
                    if (isFirstPass) { 
                        if (symboles.existe(nom)) {
                            throw new Exception("Erreur d'assemblage (Passe 1): Symbole EQU dupliqué '" + nom + "' à la ligne " + numeroLigne);
                        }
                        symboles.definirSymbole(nom, valeur);
                        System.out.printf("%s EQU $%04X (%d) (Ligne %d)%n", nom, valeur, valeur, numeroLigne);
                    }
                } else {
                    throw new Exception("Erreur d'assemblage (Passe " + (isFirstPass ? "1" : "2") + "): Valeur EQU invalide ou symbole non résolu: '" + valeurStr + "' à la ligne " + numeroLigne);
                }
            }
            return true;
        }

        if (upper.startsWith("FCC")) {
            String str = extraireChaine(ligne.substring(3).trim());
            if (isFirstPass) {
                adresseCourante += str.length();
            } else {
                for (char c : str.toCharArray()) {
                    mem.ecrireToRom(adresseCourante++, c); 
                }
            }
            return true;
        }

        if (upper.startsWith("FCB")) {
            String operandPart = ligne.substring(3).trim();
            String[] values = operandPart.split(",");
            if (isFirstPass) {
                adresseCourante += values.length;
            } else {
                for (String val : values) {
                    Integer v = symboles.resoudre(val.trim());
                    if (v != null) {
                        mem.ecrireToRom(adresseCourante++, v & 0xFF); 
                    } else {
                        throw new Exception("Erreur d'assemblage (Passe 2): Valeur FCB invalide ou symbole non résolu: '" + val.trim() + "' à la ligne " + numeroLigne);
                    }
                }
            }
            return true;
        }

        if (upper.startsWith("FDB")) {
            String operandPart = ligne.substring(3).trim();
            String[] values = operandPart.split(",");
            if (isFirstPass) {
                adresseCourante += values.length * 2;
            } else {
                for (String val : values) {
                    Integer v = symboles.resoudre(val.trim());
                    if (v != null) {
                        mem.ecrireMotToRom(adresseCourante, v & 0xFFFF); 
                        adresseCourante += 2;
                    } else {
                        throw new Exception("Erreur d'assemblage (Passe 2): Valeur FDB invalide ou symbole non résolu: '" + val.trim() + "' à la ligne " + numeroLigne);
                    }
                }
            }
            return true;
        }

        if (upper.startsWith("RMB")) {
            String operand = ligne.substring(3).trim();
            Integer count = symboles.resoudre(operand);
            if (count != null) {
                adresseCourante += count;
            } else {
                throw new Exception("Erreur d'assemblage (Passe " + (isFirstPass ? "1" : "2") + "): Valeur RMB invalide ou symbole non résolu: '" + operand + "' à la ligne " + numeroLigne);
            }
            return true;
        }

        if (upper.startsWith("SETDP")) {
            String operand = ligne.substring(5).trim();
            Integer dp = symboles.resoudre(operand);
            if (dp != null) {
                System.out.printf("SETDP $%02X (Ligne %d)%n", dp, numeroLigne);
            } else {
                throw new Exception("Erreur d'assemblage (Passe " + (isFirstPass ? "1" : "2") + "): Valeur SETDP invalide ou symbole non résolu: '" + operand + "' à la ligne " + numeroLigne);
            }
            return true;
        }

        if (upper.equals("END")) {
            System.out.println("END directive rencontrée (Ligne " + numeroLigne + ")");
            return true; 
        }

        return false;
    }

    private String resoudreSymboles(String operand) {
        if (operand == null || operand.isEmpty()) {
            return operand;
        }

        if (ParseurOperande.estNombre(operand)) {
            return operand;
        }
        
        String labelPart = ParseurOperande.extraireLabel(operand);
        int offsetPart = ParseurOperande.extraireOffset(operand); 

        if (!labelPart.isEmpty()) {
            Integer addr = symboles.obtenir(labelPart);
            if (addr != null) {
                int valeur = addr + offsetPart; 
                String resolvedValue = "$" + Integer.toHexString(valeur & 0xFFFF).toUpperCase();
                
                if (operand.startsWith("#")) {
                    return "#" + resolvedValue;
                }
                return resolvedValue; 
            }
        }
        
        Integer symboleValue = symboles.obtenir(operand);
        if (symboleValue != null) {
            return "$" + Integer.toHexString(symboleValue & 0xFFFF).toUpperCase();
        }

        return operand;
    }

    private String extraireChaine(String s) {
        s = s.trim();
        if (s.length() >= 2) {
            char delim = s.charAt(0);
            if ((delim == '"' || delim == '\'') && s.charAt(s.length() - 1) == delim) {
                return s.substring(1, s.length() - 1);
            }
        }
        return s;
    }

    private boolean traiterInstruction(Instruction instr, boolean isFirstPass, int numeroLigne) throws Exception {
        String mnemonic = instr.getMnemonic();
        String operand = instr.getOperand();
        asm.ModeAdressage detectedMode = instr.getMode(); 

        // Cas spécial: instructions de registres (EXG, TFR, PSH, PUL)
        if (TableOpcodes.estRegistreOp(mnemonic)) {
            int opcode = TableOpcodes.getOpcode(mnemonic, asm.ModeAdressage.REGISTRE);
            if (opcode < 0) { 
                opcode = TableOpcodes.getOpcode(mnemonic, asm.ModeAdressage.IMMEDIAT); // Fallback, si REGISTRE n'est pas défini
            }
            if (opcode < 0) {
                throw new Exception("Erreur d'assemblage: Mode registre/immédiat non supporté pour '" + mnemonic + "' à la ligne " + numeroLigne);
            }
            instr.setOpcode(opcode);

            int postByte;
            if (mnemonic.equals("EXG") || mnemonic.equals("TFR")) {
                postByte = RegistreHelper.genererPostByteEXG_TFR(operand);
            } else { // PSHS, PULS, PSHU, PULU
                postByte = RegistreHelper.genererPostBytePile(operand);
            }
            instr.setPostByte(postByte);
            instr.setOperandValue(postByte); // La valeur de l'opérande est le post-byte lui-même
            instr.setOperandSize(0); // <<< CORRECTION CRUCIALE ICI: DOIT ÊTRE 0 pour PSHS/PULS/EXG/TFR
            return true;
        }

        // Traitement spécial pour les branches (mode RELATIF)
        if (TableOpcodes.estBranche(mnemonic)) {
            if (isFirstPass) {
                int opcode8bit = TableOpcodes.getOpcode(mnemonic, asm.ModeAdressage.RELATIF);
                if (opcode8bit > 0) { 
                    instr.setOpcode(opcode8bit);
                    instr.setOperandSize(1); 
                    adresseCourante += 2; 
                    return true;
                } else { 
                    String longMnemonic = "L" + mnemonic;
                    int opcode16bit = TableOpcodes.getOpcode(longMnemonic, asm.ModeAdressage.RELATIF);
                    if (opcode16bit < 0) {
                        throw new Exception("Erreur d'assemblage (Passe 1): Mnémonique de branche inconnue ou sans mode relatif: '" + mnemonic + "' à la ligne " + numeroLigne);
                    }
                    instr.setOpcode(opcode16bit);
                    instr.setOperandSize(2); 
                    adresseCourante += (opcode16bit > 0xFF ? 2 : 1) + 2;
                    return true;
                }
            } else { 
                Integer targetAddress = symboles.obtenir(operand);
                if (targetAddress == null) {
                    throw new Exception("Erreur d'assemblage (Passe 2): Cible de branche relative '" + operand + "' non résolue à la ligne " + numeroLigne);
                }
                
                int pcAfterOpcode8bit = adresseCourante + 1; 
                int offset8bit = targetAddress - (pcAfterOpcode8bit + 1); 
                
                if (offset8bit >= -128 && offset8bit <= 127) {
                    int opcode = TableOpcodes.getOpcode(mnemonic, asm.ModeAdressage.RELATIF);
                    if (opcode < 0) { 
                    } else {
                        instr.setOpcode(opcode);
                        instr.setOperandValue(offset8bit & 0xFF);
                        instr.setOperandSize(1);
                        return true;
                    }
                }
                
                String longMnemonic = "L" + mnemonic;
                int opcode16bit = TableOpcodes.getOpcode(longMnemonic, asm.ModeAdressage.RELATIF);
                if (opcode16bit < 0) {
                    throw new Exception("Erreur d'assemblage (Passe 2): Branche relative '" + mnemonic + "' vers $" + Integer.toHexString(targetAddress) + "' est trop longue pour un offset de 8 bits et n'a pas de version 16 bits (comme L" + mnemonic + ") à la ligne " + numeroLigne);
                }
                instr.setMnemonic(longMnemonic); 
                instr.setOpcode(opcode16bit);

                int opcodeBytes = (opcode16bit > 0xFF) ? 2 : 1;
                int pcAfterLongOpcode = adresseCourante + opcodeBytes;
                int offset16bit = targetAddress - (pcAfterLongOpcode + 2); 

                instr.setOperandValue(offset16bit & 0xFFFF);
                instr.setOperandSize(2);
                return true;
            }
        }

        // Pour les autres instructions (non-branches, non-registres)
        int opcode = TableOpcodes.getOpcode(mnemonic, detectedMode);
        if (opcode < 0) {
            throw new Exception("Erreur d'assemblage: Mode d'adressage '" + detectedMode + "' non supporté pour le mnémonique '" + mnemonic + "' à la ligne " + numeroLigne);
        }
        instr.setOpcode(opcode);

        switch (detectedMode) {
            case INHERENT:
                instr.setOperandSize(0);
                break;

            case IMMEDIAT:
                int valImm = ParseurOperande.parseValeur(operand);
                instr.setOperandValue(valImm);
                instr.setOperandSize(TableOpcodes.est16bits(mnemonic) ? 2 : 1);
                break;

            case DIRECT:
                int adrDir = ParseurOperande.parseAdresse(operand);
                instr.setOperandValue(adrDir & 0xFF);
                instr.setOperandSize(1);
                break;

            case ETENDU:
                int adrExt = ParseurOperande.parseAdresse(operand);
                instr.setOperandValue(adrExt & 0xFFFF);
                instr.setOperandSize(2);
                break;

            case INDEXE:
                GenerateurPostByte.ResultatIndexe res = GenerateurPostByte.generer(operand);
                if (res == null) {
                    throw new Exception("Erreur d'assemblage: Syntaxe invalide pour le mode indexé: '" + operand + "' à la ligne " + numeroLigne);
                }
                instr.setPostByte(res.postByte);
                instr.setOperandValue(res.offset);
                instr.setOperandSize(res.offsetSize);
                break;
            
            case RELATIF:
                throw new Exception("Erreur d'assemblage: Le mode RELATIF ne devrait pas être directement détecté pour les instructions non-branches via ModeAdressage.detecter() à la ligne " + numeroLigne);

            default:
                throw new Exception("Erreur d'assemblage: Mode d'adressage non géré: '" + detectedMode + "' pour '" + mnemonic + "' à la ligne " + numeroLigne);
        }

        return true;
    }

    private void ecrireEnMemoireRom(Instruction instr) { 
        int opcode = instr.getOpcode();

        if (opcode > 0xFF) { 
            mem.ecrireToRom(adresseCourante++, (opcode >> 8) & 0xFF); 
        }
        mem.ecrireToRom(adresseCourante++, opcode & 0xFF); 

        if (instr.hasPostByte()) {
            mem.ecrireToRom(adresseCourante++, instr.getPostByte() & 0xFF);
        }

        int valeur = instr.getOperandValue();
        int taille = instr.getOperandSize();

        if (taille == 2) {
            mem.ecrireToRom(adresseCourante++, (valeur >> 8) & 0xFF); 
            mem.ecrireToRom(adresseCourante++, valeur & 0xFF);      
        } else if (taille == 1) { // Ne devrait plus être déclenché pour PSHS/PULS/EXG/TFR
            mem.ecrireToRom(adresseCourante++, valeur & 0xFF);
        }
    }

    public int getAdresseCourante() {
        return adresseCourante;
    }

    public int getAdresseDepart() {
        return adresseDepart;
    }

    public void setAdresseDepart(int adresse) {
        this.adresseDepart = adresse & 0xFFFF;
        this.adresseCourante = this.adresseDepart;
    }

    public TableSymboles getTableSymboles() {
        return symboles;
    }

    public void afficherSymboles() {
        System.out.println("--- Table de Symboles ---");
        System.out.println(symboles);
    }

    public boolean isEndDirectiveFound() {
        return endDirectiveFound;
    }

    public int getEndDirectiveLineNumber() {
        return endDirectiveLineNumber;
    }
}
