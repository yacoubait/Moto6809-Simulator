package asm;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TableOpcodes {
    
    public static final Map<String, Map<ModeAdressage, Integer>> OPCODES = new HashMap<>();
    public static final Map<String, Integer> INHERENT_OPCODES = new HashMap<>();
    
    private static final Set<String> BRANCH_MNEMONICS;
    private static final Set<String> MNEMONICS_16_BITS;
    private static final Set<String> REGISTRE_OP_MNEMONICS; // Instructions de registre (EXG, TFR, PSH, PUL)
    
    static {
        initInherent();
        initOpcodes8bits();
        initOpcodes16bits();
        initOpcodesMemoire();
        initOpcodesBranches();
        initOpcodesSpeciaux();

        BRANCH_MNEMONICS = Set.of(
            "BCC", "BCS", "BEQ", "BGE", "BGT", "BHI", "BHS", "BLE", "BLS", "BLT", 
            "BMI", "BNE", "BPL", "BRA", "BRN", "BSR", "BVC", "BVS",
            "LBRA", "LBSR", 
            "LBEQ", "LBCS", "LBCC", "LBMI", "LBPL", "LBVS", "LBVC",
            "LBHI", "LBLS", "LBGE", "LBLT", "LBGT", "LBLE" 
        );
        MNEMONICS_16_BITS = Set.of(
            "ADDD", "SUBD", "CMPD", "CMPX", "CMPY", "CMPU", "CMPS", 
            "LDD", "LDX", "LDY", "LDU", "LDS", 
            "STD", "STX", "STY", "STU", "STS"
        );
        REGISTRE_OP_MNEMONICS = Set.of(
            "EXG", "TFR", "PSHS", "PULS", "PSHU", "PULU" // <<< Assurez-vous que ces mnémoniques sont ICI
        );
    }

    private static void initInherent() {
        INHERENT_OPCODES.put("ABX", 0x3A);
        INHERENT_OPCODES.put("ASLA", 0x48); INHERENT_OPCODES.put("ASLB", 0x58);
        INHERENT_OPCODES.put("ASRA", 0x47); INHERENT_OPCODES.put("ASRB", 0x57);
        INHERENT_OPCODES.put("CLRA", 0x4F); INHERENT_OPCODES.put("CLRB", 0x5F);
        INHERENT_OPCODES.put("COMA", 0x43); INHERENT_OPCODES.put("COMB", 0x53);
        INHERENT_OPCODES.put("DAA", 0x19);
        INHERENT_OPCODES.put("DECA", 0x4A); INHERENT_OPCODES.put("DECB", 0x5A);
        INHERENT_OPCODES.put("INCA", 0x4C); INHERENT_OPCODES.put("INCB", 0x5C);
        INHERENT_OPCODES.put("LSLA", 0x48); INHERENT_OPCODES.put("LSLB", 0x58); // ALIAS for ASLA/B
        INHERENT_OPCODES.put("LSRA", 0x44); INHERENT_OPCODES.put("LSRB", 0x54);
        INHERENT_OPCODES.put("MUL", 0x3D);
        INHERENT_OPCODES.put("NEGA", 0x40); INHERENT_OPCODES.put("NEGB", 0x50);
        INHERENT_OPCODES.put("NOP", 0x12);
        INHERENT_OPCODES.put("ROLA", 0x49); INHERENT_OPCODES.put("ROLB", 0x59);
        INHERENT_OPCODES.put("RORA", 0x46); INHERENT_OPCODES.put("RORB", 0x56);
        INHERENT_OPCODES.put("RTI", 0x3B);
        INHERENT_OPCODES.put("RTS", 0x39);
        INHERENT_OPCODES.put("SEX", 0x1D);
        INHERENT_OPCODES.put("SWI", 0x3F);
        INHERENT_OPCODES.put("SWI2", 0x103F);
        INHERENT_OPCODES.put("SWI3", 0x113F);
        INHERENT_OPCODES.put("SYNC", 0x13);
        INHERENT_OPCODES.put("TSTA", 0x4D); INHERENT_OPCODES.put("TSTB", 0x5D);
    }

    private static void initOpcodes8bits() {
        ajouterInstruction("ADCA", 0x89, 0x99, 0xA9, 0xB9);
        ajouterInstruction("ADCB", 0xC9, 0xD9, 0xE9, 0xF9);
        ajouterInstruction("ADDA", 0x8B, 0x9B, 0xAB, 0xBB);
        ajouterInstruction("ADDB", 0xCB, 0xDB, 0xEB, 0xFB);
        ajouterInstruction("ANDA", 0x84, 0x94, 0xA4, 0xB4);
        ajouterInstruction("ANDB", 0xC4, 0xD4, 0xE4, 0xF4);
        ajouterInstruction("BITA", 0x85, 0x95, 0xA5, 0xB5);
        ajouterInstruction("BITB", 0xC5, 0xD5, 0xE5, 0xF5);
        ajouterInstruction("CMPA", 0x81, 0x91, 0xA1, 0xB1);
        ajouterInstruction("CMPB", 0xC1, 0xD1, 0xE1, 0xF1);
        ajouterInstruction("EORA", 0x88, 0x98, 0xA8, 0xB8);
        ajouterInstruction("EORB", 0xC8, 0xD8, 0xE8, 0xF8);
        ajouterInstruction("LDA", 0x86, 0x96, 0xA6, 0xB6);
        ajouterInstruction("LDB", 0xC6, 0xD6, 0xE6, 0xF6);
        ajouterInstruction("ORA", 0x8A, 0x9A, 0xAA, 0xBA);
        ajouterInstruction("ORB", 0xCA, 0xDA, 0xEA, 0xFA);
        ajouterInstruction("SBCA", 0x82, 0x92, 0xA2, 0xB2);
        ajouterInstruction("SBCB", 0xC2, 0xD2, 0xE2, 0xF2);
        ajouterInstruction("SUBA", 0x80, 0x90, 0xA0, 0xB0);
        ajouterInstruction("SUBB", 0xC0, 0xD0, 0xE0, 0xF0);
        
        ajouterInstruction("STA", -1, 0x97, 0xA7, 0xB7);
        ajouterInstruction("STB", -1, 0xD7, 0xE7, 0xF7);
    }

    private static void initOpcodes16bits() {
        ajouterInstruction("ADDD", 0xC3, 0xD3, 0xE3, 0xF3);
        ajouterInstruction("CMPX", 0x8C, 0x9C, 0xAC, 0xBC);
        ajouterInstruction("LDD", 0xCC, 0xDC, 0xEC, 0xFC);
        ajouterInstruction("LDX", 0x8E, 0x9E, 0xAE, 0xBE);
        ajouterInstruction("LDU", 0xCE, 0xDE, 0xEE, 0xFE);
        ajouterInstruction("SUBD", 0x83, 0x93, 0xA3, 0xB3);
        
        ajouterInstruction("STD", -1, 0xDD, 0xED, 0xFD);
        ajouterInstruction("STX", -1, 0x9F, 0xAF, 0xBF);
        ajouterInstruction("STU", -1, 0xDF, 0xEF, 0xFF);
        
        ajouterInstruction("CMPD", 0x1083, 0x1093, 0x10A3, 0x10B3); 
        ajouterInstruction("CMPY", 0x108C, 0x109C, 0x10AC, 0x10BC);
        ajouterInstruction("LDY", 0x108E, 0x109E, 0x10AE, 0x10BE);
        ajouterInstruction("LDS", 0x10CE, 0x10DE, 0x10EE, 0x10FE);
        ajouterInstruction("STY", -1, 0x109F, 0x10AF, 0x10BF);
        ajouterInstruction("STS", -1, 0x10DF, 0x10EF, 0x10FF);
        
        ajouterInstruction("CMPU", 0x1183, 0x1193, 0x11A3, 0x11B3);
        ajouterInstruction("CMPS", 0x118C, 0x119C, 0x11AC, 0x11BC);
    }

    private static void initOpcodesMemoire() {
        ajouterInstructionMem("ASL", 0x08, 0x68, 0x78);
        ajouterInstructionMem("ASR", 0x07, 0x67, 0x77);
        ajouterInstructionMem("CLR", 0x0F, 0x6F, 0x7F);
        ajouterInstructionMem("COM", 0x03, 0x63, 0x73);
        ajouterInstructionMem("DEC", 0x0A, 0x6A, 0x7A);
        ajouterInstructionMem("INC", 0x0C, 0x6C, 0x7C);
        ajouterInstructionMem("JMP", 0x0E, 0x6E, 0x7E);
        ajouterInstructionMem("JSR", 0x9D, 0xAD, 0xBD);
        ajouterInstructionMem("LSL", 0x08, 0x68, 0x78); 
        ajouterInstructionMem("LSR", 0x04, 0x64, 0x74);
        ajouterInstructionMem("NEG", 0x00, 0x60, 0x70);
        ajouterInstructionMem("ROL", 0x09, 0x69, 0x79);
        ajouterInstructionMem("ROR", 0x06, 0x66, 0x76);
        ajouterInstructionMem("TST", 0x0D, 0x6D, 0x7D);
    }

    private static void initOpcodesBranches() {
        ajouterInstructionRelatif("BCC", 0x24); ajouterInstructionRelatif("BHS", 0x24);
        ajouterInstructionRelatif("BCS", 0x25); ajouterInstructionRelatif("BLO", 0x25);
        ajouterInstructionRelatif("BEQ", 0x27);
        ajouterInstructionRelatif("BGE", 0x2C);
        ajouterInstructionRelatif("BGT", 0x2E);
        ajouterInstructionRelatif("BHI", 0x22);
        ajouterInstructionRelatif("BLE", 0x2F);
        ajouterInstructionRelatif("BLS", 0x23);
        ajouterInstructionRelatif("BLT", 0x2D);
        ajouterInstructionRelatif("BMI", 0x2B);
        ajouterInstructionRelatif("BNE", 0x26);
        ajouterInstructionRelatif("BPL", 0x2A);
        ajouterInstructionRelatif("BRA", 0x20);
        ajouterInstructionRelatif("BRN", 0x21);
        ajouterInstructionRelatif("BSR", 0x8D); 
        ajouterInstructionRelatif("BVC", 0x28);
        ajouterInstructionRelatif("BVS", 0x29);

        ajouterInstructionRelatif("LBCC", 0x1024); ajouterInstructionRelatif("LBHS", 0x1024);
        ajouterInstructionRelatif("LBCS", 0x1025); ajouterInstructionRelatif("LBLO", 0x1025);
        ajouterInstructionRelatif("LBEQ", 0x1027);
        ajouterInstructionRelatif("LBGE", 0x102C);
        ajouterInstructionRelatif("LBGT", 0x102E);
        ajouterInstructionRelatif("LBHI", 0x1022);
        ajouterInstructionRelatif("LBLE", 0x102F);
        ajouterInstructionRelatif("LBLS", 0x1023);
        ajouterInstructionRelatif("LBLT", 0x102D);
        ajouterInstructionRelatif("LBMI", 0x102B);
        ajouterInstructionRelatif("LBNE", 0x1026);
        ajouterInstructionRelatif("LBRA", 0x16); 
        ajouterInstructionRelatif("LBRN", 0x1021);
        ajouterInstructionRelatif("LBSR", 0x17); 
        ajouterInstructionRelatif("LBVC", 0x1028);
        ajouterInstructionRelatif("LBVS", 0x1029);
    }

    private static void initOpcodesSpeciaux() {
        Map<ModeAdressage, Integer> andcc = new HashMap<>();
        andcc.put(ModeAdressage.IMMEDIAT, 0x1C);
        OPCODES.put("ANDCC", andcc);

        Map<ModeAdressage, Integer> orcc = new HashMap<>();
        orcc.put(ModeAdressage.IMMEDIAT, 0x1A);
        OPCODES.put("ORCC", orcc);

        Map<ModeAdressage, Integer> cwai = new HashMap<>();
        cwai.put(ModeAdressage.IMMEDIAT, 0x3C);
        OPCODES.put("CWAI", cwai);

        Map<ModeAdressage, Integer> exg = new HashMap<>();
        exg.put(ModeAdressage.IMMEDIAT, 0x1E); 
        exg.put(ModeAdressage.REGISTRE, 0x1E); 
        OPCODES.put("EXG", exg);

        Map<ModeAdressage, Integer> tfr = new HashMap<>();
        tfr.put(ModeAdressage.IMMEDIAT, 0x1F); 
        tfr.put(ModeAdressage.REGISTRE, 0x1F); 
        OPCODES.put("TFR", tfr);

        // <<< MODIFICATION ICI pour PSH/PUL
        Map<ModeAdressage, Integer> pshs = new HashMap<>();
        pshs.put(ModeAdressage.REGISTRE, 0x34); // Le mode REGISTRE est le plus approprié
        OPCODES.put("PSHS", pshs);

        Map<ModeAdressage, Integer> puls = new HashMap<>();
        puls.put(ModeAdressage.REGISTRE, 0x35);
        OPCODES.put("PULS", puls);

        Map<ModeAdressage, Integer> pshu = new HashMap<>();
        pshu.put(ModeAdressage.REGISTRE, 0x36);
        OPCODES.put("PSHU", pshu);

        Map<ModeAdressage, Integer> pulu = new HashMap<>();
        pulu.put(ModeAdressage.REGISTRE, 0x37);
        OPCODES.put("PULU", pulu);

        Map<ModeAdressage, Integer> leax = new HashMap<>();
        leax.put(ModeAdressage.INDEXE, 0x30);
        OPCODES.put("LEAX", leax);

        Map<ModeAdressage, Integer> leay = new HashMap<>();
        leay.put(ModeAdressage.INDEXE, 0x31);
        OPCODES.put("LEAY", leay);

        Map<ModeAdressage, Integer> leas = new HashMap<>();
        leas.put(ModeAdressage.INDEXE, 0x32);
        OPCODES.put("LEAS", leas);

        Map<ModeAdressage, Integer> leau = new HashMap<>();
        leau.put(ModeAdressage.INDEXE, 0x33);
        OPCODES.put("LEAU", leau);
    }

    private static void ajouterInstruction(String mnemonic, int imm, int dir, int idx, int ext) {
        Map<ModeAdressage, Integer> modes = OPCODES.computeIfAbsent(mnemonic, k -> new HashMap<>());
        if (imm >= 0) modes.put(ModeAdressage.IMMEDIAT, imm);
        if (dir >= 0) modes.put(ModeAdressage.DIRECT, dir);
        if (idx >= 0) modes.put(ModeAdressage.INDEXE, idx);
        if (ext >= 0) modes.put(ModeAdressage.ETENDU, ext);
    }

    private static void ajouterInstructionMem(String mnemonic, int dir, int idx, int ext) {
        ajouterInstruction(mnemonic, -1, dir, idx, ext);
    }

    private static void ajouterInstructionRelatif(String mnemonic, int rel) {
        Map<ModeAdressage, Integer> modes = OPCODES.computeIfAbsent(mnemonic, k -> new HashMap<>());
        if (rel >= 0) modes.put(ModeAdressage.RELATIF, rel);
    }

    // ==================== API PUBLIQUE ====================

    public static int getOpcode(String mnemonic, ModeAdressage mode) {
        if (mode == ModeAdressage.INHERENT && INHERENT_OPCODES.containsKey(mnemonic)) {
            return INHERENT_OPCODES.get(mnemonic);
        }
        
        Map<ModeAdressage, Integer> modes = OPCODES.get(mnemonic);
        if (modes != null && modes.containsKey(mode)) {
            return modes.get(mode);
        }
        
        return -1;
    }

    public static boolean existe(String mnemonic) {
        return INHERENT_OPCODES.containsKey(mnemonic) || OPCODES.containsKey(mnemonic);
    }

    public static boolean estInherent(String mnemonic) {
        return INHERENT_OPCODES.containsKey(mnemonic);
    }

    public static boolean estBranche(String mnemonic) {
        return BRANCH_MNEMONICS.contains(mnemonic.toUpperCase());
    }

    public static boolean est16bits(String mnemonic) {
        return MNEMONICS_16_BITS.contains(mnemonic.toUpperCase());
    }

    public static boolean estRegistreOp(String mnemonic) {
        return REGISTRE_OP_MNEMONICS.contains(mnemonic.toUpperCase());
    }

    public static Set<String> getAllMnemonics() {
        Set<String> allMnemonics = new HashSet<>(INHERENT_OPCODES.keySet());
        allMnemonics.addAll(OPCODES.keySet());
        return Collections.unmodifiableSet(allMnemonics);
    }
}
