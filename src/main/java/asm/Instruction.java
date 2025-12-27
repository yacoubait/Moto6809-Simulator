package asm;

public class Instruction {
    private String mnemonic; 
    private final String operand;
    private ModeAdressage mode; // Le mode initial sera une estimation, l'assembleur le finalisera

    private int opcode;
    private int postByte = -1;
    private int operandValue;
    private int operandSize;  // 0, 1 ou 2 bytes

    public Instruction(String mnemonic, String operand) {
        this.mnemonic = mnemonic.toUpperCase();
        this.operand = operand != null ? operand.trim() : "";
        this.mode = ModeAdressage.detecter(this.operand); // Détection initiale purement syntaxique
        
        // Logique de correction pour REGISTRE/RELATIF basée sur le mnémonique
        if (TableOpcodes.estBranche(this.mnemonic)) {
            this.mode = ModeAdressage.RELATIF; // Force RELATIF pour les branches
        } else if (TableOpcodes.estRegistreOp(this.mnemonic)) {
            this.mode = ModeAdressage.REGISTRE; // Force REGISTRE pour EXG/TFR/PSH/PUL
        }
        // Pour les autres, le mode détecté (y compris un RELATIF pour un label non-branche)
        // sera finalisé dans Assembleur.traiterInstruction().
    }

    // Getters
    public String getMnemonic() { return mnemonic; }
    public String getOperand() { return operand; }
    public ModeAdressage getMode() { return mode; }
    public int getOpcode() { return opcode; }
    public int getPostByte() { return postByte; }
    public int getOperandValue() { return operandValue; }
    public int getOperandSize() { return operandSize; }
    public boolean hasPostByte() { return postByte != -1; }

    // Setters
    public void setMnemonic(String mnemonic) { this.mnemonic = mnemonic.toUpperCase(); } 
    public void setMode(ModeAdressage mode) { this.mode = mode; } 
    public void setOpcode(int opcode) { this.opcode = opcode; }
    public void setPostByte(int postByte) { this.postByte = postByte; }
    public void setOperandValue(int value) { this.operandValue = value; }
    public void setOperandSize(int size) { this.operandSize = size; }

    /**
     * Calcule la taille totale en bytes de l'instruction assemblée.
     * @return La taille de l'instruction en octets.
     */
    public int getTaille() {
        int taille = (opcode > 0xFF) ? 2 : 1; 
        if (hasPostByte()) taille++;          
        taille += operandSize;                 
        return taille;
    }

    /**
     * Estime la taille d'une instruction en première passe, sans résoudre les symboles.
     * C'est une estimation conservative.
     * @param mnemonic Le mnémonique de l'instruction.
     * @param operand L'opérande de l'instruction.
     * @return La taille estimée en octets.
     * @throws IllegalArgumentException si le mnémonique est inconnu.
     */
    public static int estimerTaille(String mnemonic, String operand) {
        String upperMnemonic = mnemonic.toUpperCase();
        
        // Si mnémonique inconnu, lever une exception
        if (!TableOpcodes.existe(upperMnemonic) && !TableOpcodes.estBranche(upperMnemonic)) {
            throw new IllegalArgumentException("Mnémonique inconnue: '" + mnemonic + "'");
        }

        ModeAdressage mode = ModeAdressage.detecter(operand); // Utilise la détection de mode simplifiée

        // Instructions inhérentes
        if (mode == ModeAdressage.INHERENT) {
            int opcode = TableOpcodes.getOpcode(upperMnemonic, ModeAdressage.INHERENT);
            if (opcode > 0) return (opcode > 0xFF) ? 2 : 1;
        }
        
        // Instructions de registres (EXG, TFR, PSH, PUL)
        if (TableOpcodes.estRegistreOp(upperMnemonic)) {
            int opcode = TableOpcodes.getOpcode(upperMnemonic, ModeAdressage.REGISTRE);
            if (opcode < 0) opcode = TableOpcodes.getOpcode(upperMnemonic, ModeAdressage.IMMEDIAT);
            if (opcode > 0) return ((opcode > 0xFF) ? 2 : 1) + 1; // Opcode + Post-byte
        }

        // Pour les branches (elles ont déjà forcé le mode à RELATIF dans le constructeur Instruction)
        if (TableOpcodes.estBranche(upperMnemonic)) {
            int opcode8bit = TableOpcodes.getOpcode(upperMnemonic, ModeAdressage.RELATIF);
            int opcode16bit = TableOpcodes.getOpcode("L" + upperMnemonic, ModeAdressage.RELATIF); // Ex: LBRA
            
            if (opcode16bit > 0) { // Si une version 16-bit existe
                return ((opcode16bit > 0xFF) ? 2 : 1) + 2; // Opcode (1 ou 2 bytes) + offset 2 bytes
            } else if (opcode8bit > 0) { // Sinon, si seulement 8-bit
                 return ((opcode8bit > 0xFF) ? 2 : 1) + 1; // Opcode (1 ou 2 bytes) + offset 1 byte
            } else {
                throw new IllegalArgumentException("Mnémonique de branche sans mode relatif valide: '" + mnemonic + "'");
            }
        }
        
        // Estimer la taille en fonction du mode d'adressage détecté (asm.ModeAdressage)
        // C'est une heuristique pour la 1ère passe
        int opcodePrefixBytes = 0; // Pour les opcodes de page 2/3 (0x10, 0x11)
        int opcodeBaseBytes = 1;   // L'opcode lui-même
        int operandBytes = 0;      // Taille de l'opérande
        int postByteBytes = 0;     // Taille du post-byte (pour l'adressage indexé)

        // Déterminer si l'opcode principal est de page 2 (0x10) ou page 3 (0x11)
        if (upperMnemonic.matches("CMPY|LDY|STY|LDS|STS")) { 
            opcodePrefixBytes = 1;
        } else if (upperMnemonic.matches("CMPU|CMPS")) { 
            opcodePrefixBytes = 1;
        }
        
        switch (mode) {
            case INHERENT:
                break;
            case IMMEDIAT:
                // Assume 2 bytes pour l'opérande immédiate pour les mnémoniques 16-bits, 1 byte pour les 8-bits
                operandBytes = TableOpcodes.est16bits(upperMnemonic) ? 2 : 1;
                break;
            case DIRECT:
                operandBytes = 1; // 1 octet pour l'adresse directe
                break;
            case ETENDU:
                operandBytes = 2; // 2 octets pour l'adresse étendue
                break;
            case INDEXE:
                postByteBytes = 1; // Toujours un post-byte
                // L'offset peut être 0, 1 ou 2 octets. Pour la première passe, le plus sûr est 2.
                if (operand.matches(".*,-{1,2}[XYUSPC].*") || operand.matches(".*,[XYUSPC]\\+{1,2}.*") || operand.matches("^[XYUS]$")) {
                    operandBytes = 0; // Auto-inc/dec n'ont pas d'offset en plus
                } else if (operand.matches(".*,[ABDd],[XYUSPC].*")) {
                    operandBytes = 0; // Registre comme offset
                } else {
                    operandBytes = 2; // Par défaut, offset 16 bits pour une estimation conservative
                }
                break;
            case RELATIF:
                // Si le mode est RELATIF pour une instruction non-branche (ex: LDA VALEUR_1),
                // c'est un label. On estime la taille comme une adresse étendue pour la 1ère passe.
                operandBytes = 2; 
                break;
            case REGISTRE:
                // Géré plus haut
                break;
        }
        
        return opcodePrefixBytes + opcodeBaseBytes + postByteBytes + operandBytes;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-6s %-12s", mnemonic, operand));
        sb.append(String.format(" [Mode: %-8s, Op: %04X", mode, opcode));
        if (hasPostByte()) sb.append(String.format(", PB: %02X", postByte));
        if (operandSize > 0) sb.append(String.format(", Val: %04X", operandValue));
        sb.append("]");
        return sb.toString();
    }
}
