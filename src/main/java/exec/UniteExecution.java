package exec;

import cpu.CPU6809;
import mem.Memoire;
import java.util.function.IntUnaryOperator; // Import ajouté pour le type de fonction op dans memOp

/**
 * Unité d'exécution principale du CPU 6809.
 * Elle est responsable de décoder et d'exécuter chaque instruction machine.
 * Utilise une table de dispatch pour mapper les opcodes aux actions correspondantes.
 */
public class UniteExecution {
    private final CPU6809 cpu;
    private final Memoire mem;
    
    // Modules fonctionnels
    private final ModeAdressage mode;       // Pour le calcul des adresses et la lecture des opérandes
    private final ALU alu;                  // Pour les opérations arithmétiques et logiques
    private final GestionPile pile;         // Pour la manipulation de la pile (PUSH/PULL)
    private final GestionRegistres registres; // Pour les opérations directes sur les registres (EXG/TFR)
    
    // Tables de dispatch pour les opcodes (gestion des pages d'opcodes)
    private final Runnable[] instructions = new Runnable[256]; // Page 1 (opcodes 0x00-0xFF)
    private final Runnable[] page2 = new Runnable[256];        // Page 2 (opcodes 0x10xx)
    private final Runnable[] page3 = new Runnable[256];        // Page 3 (opcodes 0x11xx)

    public UniteExecution(CPU6809 cpu, Memoire mem) {
        this.cpu = cpu;
        this.mem = mem;
        this.mode = new ModeAdressage(cpu, mem); // ModeAdressage du package exec
        this.alu = new ALU(cpu);
        // Correction pour les constructeurs de GestionPile et GestionRegistres
        // La variable 'mode' (ModeAdressage de asm) n'est pas nécessaire pour eux et était source d'erreur/confusion.
        this.pile = new GestionPile(cpu, mem);
        this.registres = new GestionRegistres(cpu);
        
        initialiserInstructions(); // Initialise les opcodes de la page principale
        initialiserPage2();        // Initialise les opcodes de la page 2
        initialiserPage3();        // Initialise les opcodes de la page 3
    }

    /**
     * Exécute la prochaine instruction pointée par le Program Counter (PC) du CPU.
     * Lit l'opcode, le dispatche vers la bonne routine d'exécution.
     * @throws IllegalStateException si un opcode inconnu est rencontré.
     * @throws RuntimeException si une erreur se produit pendant l'exécution d'une instruction.
     */
    public void executerInstruction() {
        int opcode = mode.lireOctet(); // Lit le premier octet qui est l'opcode, et incrémente le PC
        
        if (instructions[opcode] != null) {
            instructions[opcode].run();
        } else {
            // Lève une exception pour arrêter la simulation proprement en cas d'opcode inconnu
            throw new IllegalStateException(String.format("Opcode inconnu à l'adresse $%04X: %02X", (cpu.getPC() - 1) & 0xFFFF, opcode));
        }
    }

    // ==================== INITIALISATION DES INSTRUCTIONS (PAGE 1) ====================
    
    private void initialiserInstructions() {
        // --- NEG (Négation / Complément à deux) ---
        instructions[0x00] = () -> memOp(mode.direct(), alu::neg); // Direct
        instructions[0x40] = () -> cpu.setA(alu.neg(cpu.getA()));   // Accumulateur A
        instructions[0x50] = () -> cpu.setB(alu.neg(cpu.getB()));   // Accumulateur B
        instructions[0x60] = () -> memOp(mode.indexe(), alu::neg);  // Indexé
        instructions[0x70] = () -> memOp(mode.etendu(), alu::neg);  // Étendu

        // --- COM (Complément à un / NOT logique) ---
        instructions[0x03] = () -> memOp(mode.direct(), alu::com);
        instructions[0x43] = () -> cpu.setA(alu.com(cpu.getA()));
        instructions[0x53] = () -> cpu.setB(alu.com(cpu.getB()));
        instructions[0x63] = () -> memOp(mode.indexe(), alu::com);
        instructions[0x73] = () -> memOp(mode.etendu(), alu::com);

        // --- LSR (Logical Shift Right) ---
        instructions[0x04] = () -> memOp(mode.direct(), alu::lsr);
        instructions[0x44] = () -> cpu.setA(alu.lsr(cpu.getA()));
        instructions[0x54] = () -> cpu.setB(alu.lsr(cpu.getB()));
        instructions[0x64] = () -> memOp(mode.indexe(), alu::lsr);
        instructions[0x74] = () -> memOp(mode.etendu(), alu::lsr);

        // --- ROR (Rotate Right) ---
        instructions[0x06] = () -> memOp(mode.direct(), alu::ror);
        instructions[0x46] = () -> cpu.setA(alu.ror(cpu.getA()));
        instructions[0x56] = () -> cpu.setB(alu.ror(cpu.getB()));
        instructions[0x66] = () -> memOp(mode.indexe(), alu::ror);
        instructions[0x76] = () -> memOp(mode.etendu(), alu::ror);

        // --- ASR (Arithmetic Shift Right) ---
        instructions[0x07] = () -> memOp(mode.direct(), alu::asr);
        instructions[0x47] = () -> cpu.setA(alu.asr(cpu.getA()));
        instructions[0x57] = () -> cpu.setB(alu.asr(cpu.getB()));
        instructions[0x67] = () -> memOp(mode.indexe(), alu::asr);
        instructions[0x77] = () -> memOp(mode.etendu(), alu::asr);

        // --- ASL / LSL (Arithmetic/Logical Shift Left) ---
        instructions[0x08] = () -> memOp(mode.direct(), alu::asl);
        instructions[0x48] = () -> cpu.setA(alu.asl(cpu.getA()));
        instructions[0x58] = () -> cpu.setB(alu.asl(cpu.getB()));
        instructions[0x68] = () -> memOp(mode.indexe(), alu::asl);
        instructions[0x78] = () -> memOp(mode.etendu(), alu::asl);

        // --- ROL (Rotate Left) ---
        instructions[0x09] = () -> memOp(mode.direct(), alu::rol);
        instructions[0x49] = () -> cpu.setA(alu.rol(cpu.getA()));
        instructions[0x59] = () -> cpu.setB(alu.rol(cpu.getB()));
        instructions[0x69] = () -> memOp(mode.indexe(), alu::rol);
        instructions[0x79] = () -> memOp(mode.etendu(), alu::rol);

        // --- DEC (Décrémenter) ---
        instructions[0x0A] = () -> memOp(mode.direct(), alu::dec);
        instructions[0x4A] = () -> cpu.setA(alu.dec(cpu.getA()));
        instructions[0x5A] = () -> cpu.setB(alu.dec(cpu.getB()));
        instructions[0x6A] = () -> memOp(mode.indexe(), alu::dec);
        instructions[0x7A] = () -> memOp(mode.etendu(), alu::dec);

        // --- INC (Incrémenter) ---
        instructions[0x0C] = () -> memOp(mode.direct(), alu::inc);
        instructions[0x4C] = () -> cpu.setA(alu.inc(cpu.getA()));
        instructions[0x5C] = () -> cpu.setB(alu.inc(cpu.getB()));
        instructions[0x6C] = () -> memOp(mode.indexe(), alu::inc);
        instructions[0x7C] = () -> memOp(mode.etendu(), alu::inc);

        // --- TST (Tester une valeur) ---
        instructions[0x0D] = () -> alu.tst(mem.lire(mode.direct()));
        instructions[0x4D] = () -> alu.tst(cpu.getA());
        instructions[0x5D] = () -> alu.tst(cpu.getB());
        instructions[0x6D] = () -> alu.tst(mem.lire(mode.indexe()));
        instructions[0x7D] = () -> alu.tst(mem.lire(mode.etendu()));

        // --- JMP (Saut inconditionnel) ---
        instructions[0x0E] = () -> cpu.setPC(mode.direct());
        instructions[0x6E] = () -> cpu.setPC(mode.indexe());
        instructions[0x7E] = () -> cpu.setPC(mode.etendu());

        // --- CLR (Mettre à zéro) ---
        instructions[0x0F] = () -> { mem.ecrire(mode.direct(), alu.clr()); };
        instructions[0x4F] = () -> cpu.setA(alu.clr());
        instructions[0x5F] = () -> cpu.setB(alu.clr());
        instructions[0x6F] = () -> { mem.ecrire(mode.indexe(), alu.clr()); };
        instructions[0x7F] = () -> { mem.ecrire(mode.etendu(), alu.clr()); };

        // --- Préfixes des pages 2 et 3 d'opcodes ---
        instructions[0x10] = this::executerPage2;
        instructions[0x11] = this::executerPage3;

        // --- NOP (No Operation) ---
        instructions[0x12] = () -> {};

        // --- DAA (Decimal Adjust Accumulator A) ---
        instructions[0x19] = this::daa;

        // --- ORCC (OR avec le registre CC) ---
        instructions[0x1A] = () -> cpu.setCC(cpu.getCC() | mode.lireOctet());

        // --- ANDCC (AND avec le registre CC) ---
        instructions[0x1C] = () -> cpu.setCC(cpu.getCC() & mode.lireOctet());

        // --- SEX (Sign Extend B into A) ---
        instructions[0x1D] = registres::sex;

        // --- EXG (Exchange Registers) ---
        instructions[0x1E] = () -> registres.exg(mode.lireOctet());

        // --- TFR (Transfer Register) ---
        instructions[0x1F] = () -> registres.tfr(mode.lireOctet());

        // --- LEA (Load Effective Address) ---
        instructions[0x30] = () -> { cpu.setX(mode.indexe()); alu.updateFlags16(cpu.getX()); }; // LEAX affecte les flags N,Z,V
        instructions[0x31] = () -> { cpu.setY(mode.indexe()); alu.updateFlags16(cpu.getY()); }; // LEAY affecte les flags N,Z,V
        instructions[0x32] = () -> cpu.setS(mode.indexe()); // LEAS n'affecte pas les flags
        instructions[0x33] = () -> cpu.setU(mode.indexe()); // LEAU n'affecte pas les flags

        // --- PSH/PUL (Push/Pull Registers) ---
        instructions[0x34] = () -> pile.pshs(mode.lireOctet());
        instructions[0x35] = () -> pile.puls(mode.lireOctet());
        instructions[0x36] = () -> pile.pshu(mode.lireOctet());
        instructions[0x37] = () -> pile.pulu(mode.lireOctet());

        // --- RTS (Return from Subroutine) ---
        instructions[0x39] = () -> cpu.setPC(pile.pullS());

        // --- ABX (Add B to X) ---
        instructions[0x3A] = registres::abx;

        // --- MUL (Multiply A by B) ---
        instructions[0x3D] = () -> cpu.setD(alu.mul(cpu.getA(), cpu.getB())); // Correction: Utilisez cpu.setD()

        // --- SWI (Software Interrupt) ---
        instructions[0x3F] = () -> System.out.println("SWI"); // Placeholder, pourrait implémenter une gestion d'interruption

        // --- Initialisation des groupes d'instructions avec modes d'adressage ---
        initSUBA();
        initSUBB();
        initSBCA();
        initSBCB();
        initANDA();
        initANDB();
        initBITA();
        initBITB();
        initLDA();
        initLDB();
        initSTA();
        initSTB();
        initEORA();
        initEORB();
        initADCA();
        initADCB();
        initORA();
        initORB();
        initADDA();
        initADDB();
        initADCA();
        initCMPB();

        initSUBD();
        initADDD();
        initCMPX();
        initLDD();
        initSTD();
        initLDX();
        initSTX();
        initLDU();
        
        initJSR();
        initBranches(); // Initialiser les branches 8 bits
    }

    // ==================== HELPERS D'INITIALISATION D'INSTRUCTIONS ====================

    /**
     * Helper générique pour les opérations 8 bits qui lisent une opérande et écrivent dans un registre.
     * @param opcodes Tableau des opcodes [IMMEDIAT, DIRECT, INDEXE, ETENDU].
     * @param reg Getter du registre accumulateur (ex: cpu::getA).
     * @param set Setter du registre accumulateur (ex: cpu::setA).
     * @param op Opération arithmétique/logique 8 bits.
     */
    private void initInstr8(int[] opcodes, RegGetter reg, RegSetter set, Op8 op) {
        if (opcodes[0] >= 0) instructions[opcodes[0]] = () -> set.set(op.apply(reg.get(), mode.lireOctet()));
        if (opcodes[1] >= 0) instructions[opcodes[1]] = () -> set.set(op.apply(reg.get(), mem.lire(mode.direct())));
        if (opcodes[2] >= 0) instructions[opcodes[2]] = () -> set.set(op.apply(reg.get(), mem.lire(mode.indexe())));
        if (opcodes[3] >= 0) instructions[opcodes[3]] = () -> set.set(op.apply(reg.get(), mem.lire(mode.etendu())));
    }

    /**
     * Helper pour les comparaisons 8 bits (CMPA, CMPB). Elles n'écrivent pas de résultat, seulement les flags.
     * @param opcodes Tableau des opcodes.
     * @param reg Getter du registre accumulateur.
     */
    private void initCmp8(int[] opcodes, RegGetter reg) {
        if (opcodes[0] >= 0) instructions[opcodes[0]] = () -> alu.sub8(reg.get(), mode.lireOctet(), false);
        if (opcodes[1] >= 0) instructions[opcodes[1]] = () -> alu.sub8(reg.get(), mem.lire(mode.direct()), false);
        if (opcodes[2] >= 0) instructions[opcodes[2]] = () -> alu.sub8(reg.get(), mem.lire(mode.indexe()), false);
        if (opcodes[3] >= 0) instructions[opcodes[3]] = () -> alu.sub8(reg.get(), mem.lire(mode.etendu()), false);
    }

    /**
     * Helper pour les BIT tests 8 bits (BITA, BITB). Elles n'écrivent pas de résultat, seulement les flags.
     * @param opcodes Tableau des opcodes.
     * @param reg Getter du registre accumulateur.
     */
    private void initBit8(int[] opcodes, RegGetter reg) {
        if (opcodes[0] >= 0) instructions[opcodes[0]] = () -> alu.and(reg.get(), mode.lireOctet());
        if (opcodes[1] >= 0) instructions[opcodes[1]] = () -> alu.and(reg.get(), mem.lire(mode.direct()));
        if (opcodes[2] >= 0) instructions[opcodes[2]] = () -> alu.and(reg.get(), mem.lire(mode.indexe()));
        if (opcodes[3] >= 0) instructions[opcodes[3]] = () -> alu.and(reg.get(), mem.lire(mode.etendu()));
    }

    /**
     * Helper pour les opérations de Store 8 bits (STA, STB). Écrit en mémoire et met à jour les flags N/Z.
     * @param opcodes Tableau des opcodes [DIRECT, INDEXE, ETENDU] (pas d'IMM).
     * @param reg Getter du registre à stocker.
     */
    private void initStore8(int[] opcodes, RegGetter reg) {
        if (opcodes[0] >= 0) instructions[opcodes[0]] = () -> { mem.ecrire(mode.direct(), reg.get()); alu.updateFlags8(reg.get()); };
        if (opcodes[1] >= 0) instructions[opcodes[1]] = () -> { mem.ecrire(mode.indexe(), reg.get()); alu.updateFlags8(reg.get()); };
        if (opcodes[2] >= 0) instructions[opcodes[2]] = () -> { mem.ecrire(mode.etendu(), reg.get()); alu.updateFlags8(reg.get()); };
    }

    // --- Instructions individuelles 8 bits (utilisant les helpers ou implémentation directe) ---
    private void initSUBA() { initInstr8(new int[]{0x80,0x90,0xA0,0xB0}, cpu::getA, cpu::setA, (a,b)->alu.sub8(a,b,false)); }
    private void initSUBB() { initInstr8(new int[]{0xC0,0xD0,0xE0,0xF0}, cpu::getB, cpu::setB, (a,b)->alu.sub8(a,b,false)); }
    private void initSBCA() { initInstr8(new int[]{0x82,0x92,0xA2,0xB2}, cpu::getA, cpu::setA, (a,b)->alu.sub8(a,b,true)); }
    private void initSBCB() { initInstr8(new int[]{0xC2,0xD2,0xE2,0xF2}, cpu::getB, cpu::setB, (a,b)->alu.sub8(a,b,true)); }
    private void initANDA() { initInstr8(new int[]{0x84,0x94,0xA4,0xB4}, cpu::getA, cpu::setA, alu::and); }
    private void initANDB() { initInstr8(new int[]{0xC4,0xD4,0xE4,0xF4}, cpu::getB, cpu::setB, alu::and); }
    private void initBITA() { initBit8(new int[]{0x85,0x95,0xA5,0xB5}, cpu::getA); }
    private void initBITB() { initBit8(new int[]{0xC5,0xD5,0xE5,0xF5}, cpu::getB); }
    private void initEORA() { initInstr8(new int[]{0x88,0x98,0xA8,0xB8}, cpu::getA, cpu::setA, alu::xor); }
    private void initEORB() { initInstr8(new int[]{0xC8,0xD8,0xE8,0xF8}, cpu::getB, cpu::setB, alu::xor); }
    private void initADCA() { initInstr8(new int[]{0x89,0x99,0xA9,0xB9}, cpu::getA, cpu::setA, (a,b)->alu.add8(a,b,true)); }
    private void initADCB() { initInstr8(new int[]{0xC9,0xD9,0xE9,0xF9}, cpu::getB, cpu::setB, (a,b)->alu.add8(a,b,true)); }
    private void initORA()  { initInstr8(new int[]{0x8A,0x9A,0xAA,0xBA}, cpu::getA, cpu::setA, alu::or); }
    private void initORB()  { initInstr8(new int[]{0xCA,0xDA,0xEA,0xFA}, cpu::getB, cpu::setB, alu::or); }
    private void initADDA() { initInstr8(new int[]{0x8B,0x9B,0xAB,0xBB}, cpu::getA, cpu::setA, (a,b)->alu.add8(a,b,false)); }
    private void initADDB() { initInstr8(new int[]{0xCB,0xDB,0xEB,0xFB}, cpu::getB, cpu::setB, (a,b)->alu.add8(a,b,false)); }
    private void initPA() { initCmp8(new int[]{0x81,0x91,0xA1,0xB1}, cpu::getA); }
    private void initCMPB() { initCmp8(new int[]{0xC1,0xD1,0xE1,0xF1}, cpu::getB); }
    
    // Instructions de Load 8 bits spécifiques avec mise à jour des flags
    private void initLDA() {
        instructions[0x86] = () -> { cpu.setA(mode.lireOctet()); alu.updateFlags8(cpu.getA()); };
        instructions[0x96] = () -> { cpu.setA(mem.lire(mode.direct())); alu.updateFlags8(cpu.getA()); };
        instructions[0xA6] = () -> { cpu.setA(mem.lire(mode.indexe())); alu.updateFlags8(cpu.getA()); };
        instructions[0xB6] = () -> { cpu.setA(mem.lire(mode.etendu())); alu.updateFlags8(cpu.getA()); };
    }
    
    private void initLDB() {
        instructions[0xC6] = () -> { cpu.setB(mode.lireOctet()); alu.updateFlags8(cpu.getB()); };
        instructions[0xD6] = () -> { cpu.setB(mem.lire(mode.direct())); alu.updateFlags8(cpu.getB()); };
        instructions[0xE6] = () -> { cpu.setB(mem.lire(mode.indexe())); alu.updateFlags8(cpu.getB()); };
        instructions[0xF6] = () -> { cpu.setB(mem.lire(mode.etendu())); alu.updateFlags8(cpu.getB()); };
    }
    
    // Instructions de Store 8 bits spécifiques avec mise à jour des flags (N/Z)
    private void initSTA() { initStore8(new int[]{0x97,0xA7,0xB7}, cpu::getA); }
    private void initSTB() { initStore8(new int[]{0xD7,0xE7,0xF7}, cpu::getB); }

    // --- Instructions 16 bits ---
    
    /**
     * Helper générique pour les opérations 16 bits qui lisent une opérande et écrivent dans le registre D ou un registre indexé.
     * @param opcodes Tableau des opcodes [IMMEDIAT, DIRECT, INDEXE, ETENDU].
     * @param regGetter Getter du registre (ex: cpu::getD).
     * @param regSetter Setter du registre (ex: cpu::setD).
     * @param op Opération arithmétique/logique 16 bits.
     */
    private void initInstr16(int[] opcodes, RegGetter regGetter, RegSetter regSetter, Op16 op) {
        if (opcodes[0] >= 0) instructions[opcodes[0]] = () -> regSetter.set(op.apply(regGetter.get(), mode.lireMot()));
        if (opcodes[1] >= 0) instructions[opcodes[1]] = () -> regSetter.set(op.apply(regGetter.get(), mem.lireMot(mode.direct())));
        if (opcodes[2] >= 0) instructions[opcodes[2]] = () -> regSetter.set(op.apply(regGetter.get(), mem.lireMot(mode.indexe())));
        if (opcodes[3] >= 0) instructions[opcodes[3]] = () -> regSetter.set(op.apply(regGetter.get(), mem.lireMot(mode.etendu())));
    }

    /**
     * Helper pour les comparaisons 16 bits.
     * @param opcodes Tableau des opcodes.
     * @param regGetter Getter du registre 16 bits à comparer.
     */
    private void initCmp16(int[] opcodes, RegGetter regGetter) {
        if (opcodes[0] >= 0) instructions[opcodes[0]] = () -> alu.cmp16(regGetter.get(), mode.lireMot());
        if (opcodes[1] >= 0) instructions[opcodes[1]] = () -> alu.cmp16(regGetter.get(), mem.lireMot(mode.direct()));
        if (opcodes[2] >= 0) instructions[opcodes[2]] = () -> alu.cmp16(regGetter.get(), mem.lireMot(mode.indexe()));
        if (opcodes[3] >= 0) instructions[opcodes[3]] = () -> alu.cmp16(regGetter.get(), mem.lireMot(mode.etendu()));
    }

    /**
     * Helper pour les chargements 16 bits (LDD, LDX, LDY, LDU, LDS).
     * @param opcodes Tableau des opcodes.
     * @param regSetter Setter du registre 16 bits.
     * @param regGetter Getter du registre 16 bits (pour updateFlags).
     */
    private void initLoad16(int[] opcodes, RegSetter regSetter, RegGetter regGetter) {
        if (opcodes[0] >= 0) instructions[opcodes[0]] = () -> { regSetter.set(mode.lireMot()); alu.updateFlags16(regGetter.get()); };
        if (opcodes[1] >= 0) instructions[opcodes[1]] = () -> { regSetter.set(mem.lireMot(mode.direct())); alu.updateFlags16(regGetter.get()); };
        if (opcodes[2] >= 0) instructions[opcodes[2]] = () -> { regSetter.set(mem.lireMot(mode.indexe())); alu.updateFlags16(regGetter.get()); };
        if (opcodes[3] >= 0) instructions[opcodes[3]] = () -> { regSetter.set(mem.lireMot(mode.etendu())); alu.updateFlags16(regGetter.get()); };
    }

    /**
     * Helper pour les stockages 16 bits (STD, STX, STY, STU, STS).
     * @param opcodes Tableau des opcodes.
     * @param regGetter Getter du registre 16 bits à stocker (pour la valeur et les flags).
     */
    private void initStore16(int[] opcodes, RegGetter regGetter) {
        if (opcodes[0] >= 0) instructions[opcodes[0]] = () -> { mem.ecrireMot(mode.direct(), regGetter.get()); alu.updateFlags16(regGetter.get()); };
        if (opcodes[1] >= 0) instructions[opcodes[1]] = () -> { mem.ecrireMot(mode.indexe(), regGetter.get()); alu.updateFlags16(regGetter.get()); };
        if (opcodes[2] >= 0) instructions[opcodes[2]] = () -> { mem.ecrireMot(mode.etendu(), regGetter.get()); alu.updateFlags16(regGetter.get()); };
    }

    private void initSUBD() { initInstr16(new int[]{0x83,0x93,0xA3,0xB3}, cpu::getD, cpu::setD, alu::sub16); }
    private void initADDD() { initInstr16(new int[]{0xC3,0xD3,0xE3,0xF3}, cpu::getD, cpu::setD, alu::add16); }
    private void initCMPX() { initCmp16(new int[]{0x8C,0x9C,0xAC,0xBC}, cpu::getX); }
    private void initLDD()  { initLoad16(new int[]{0xCC,0xDC,0xEC,0xFC}, cpu::setD, cpu::getD); }
    private void initSTD()  { initStore16(new int[]{-1,0xDD,0xED,0xFD}, cpu::getD); } // Pas de mode immédiat pour ST_
    private void initLDX()  { initLoad16(new int[]{0x8E,0x9E,0xAE,0xBE}, cpu::setX, cpu::getX); }
    private void initSTX()  { initStore16(new int[]{-1,0x9F,0xAF,0xBF}, cpu::getX); }
    private void initLDU()  { initLoad16(new int[]{0xCE,0xDE,0xEE,0xFE}, cpu::setU, cpu::getU); }

    private void initJSR() {
        // JSR empile l'adresse de retour (PC courant) sur la pile système (S)
        instructions[0x9D] = () -> { int adr = mode.direct(); pile.pushS(cpu.getPC()); cpu.setPC(adr); };
        instructions[0xAD] = () -> { int adr = mode.indexe(); pile.pushS(cpu.getPC()); cpu.setPC(adr); };
        instructions[0xBD] = () -> { int adr = mode.etendu(); pile.pushS(cpu.getPC()); cpu.setPC(adr); };
    }

    // --- Branches 8-bit ---
    private void initBranches() {
        // Le PC est déjà incrémenté après avoir lu l'opcode. L'opérande est l'offset.
        // L'adresse de l'offset est PC actuel. La cible est PC + offset.
        // L'opcode de BSR est 0x8D, l'offset est lu par mode.lireOctet().
        // Toutes les branches relatives utilisent le même pattern : lire l'offset, calculer la nouvelle PC.
        Runnable branchHandler = () -> {
            int offset = mode.lireOctet(); // Offset 8 bits
            if ((offset & 0x80) != 0) { // Extension de signe
                offset |= 0xFFFFFF00;
            }
            cpu.setPC((cpu.getPC() + offset) & 0xFFFF);
        };
        
        // Les opcodes 0x20-0x2F sont les branches conditionnelles/inconditionnelles 8 bits
        for (int i = 0x20; i <= 0x2F; i++) {
            instructions[i] = branchHandler;
        }
        instructions[0x8D] = () -> { // BSR (Branch to Subroutine)
            int offset = mode.lireOctet();
            if ((offset & 0x80) != 0) {
                offset |= 0xFFFFFF00;
            }
            pile.pushS(cpu.getPC()); // Empile l'adresse de retour
            cpu.setPC((cpu.getPC() + offset) & 0xFFFF);
        };
        // Pour les branches longues (LBRA, LBSR, etc.), elles sont gérées dans executerPage2/3 ou des opcodes spécifiques de page 1.
        // LBRA (0x16) et LBSR (0x17) ont un offset 16 bits.
        instructions[0x16] = () -> { // LBRA (Long Branch Always)
            int offset = mode.lireMot(); // Offset 16 bits
            cpu.setPC((cpu.getPC() + offset) & 0xFFFF);
        };
        instructions[0x17] = () -> { // LBSR (Long Branch to Subroutine)
            int offset = mode.lireMot();
            pile.pushS(cpu.getPC()); // Empile l'adresse de retour
            cpu.setPC((cpu.getPC() + offset) & 0xFFFF);
        };
    }


    // ==================== PAGE 2 (Opcode préfixé par 0x10) ====================

    private void initialiserPage2() {
        // CMPD (Page 2)
        initCmp16Page2(new int[]{0x83,0x93,0xA3,0xB3}, cpu::getD); // Correction: utilisez cpu::getD
        
        // CMPY (Page 2)
        initCmp16Page2(new int[]{0x8C,0x9C,0xAC,0xBC}, cpu::getY);

        // LDY (Page 2)
        initLoad16Page2(new int[]{0x8E,0x9E,0xAE,0xBE}, cpu::setY, cpu::getY);

        // STY (Page 2)
        initStore16Page2(new int[]{-1,0x9F,0xAF,0xBF}, cpu::getY);

        // LDS (Page 2)
        initLoad16Page2(new int[]{0xCE,0xDE,0xEE,0xFE}, cpu::setS, cpu::getS);

        // STS (Page 2)
        initStore16Page2(new int[]{-1,0xDF,0xEF,0xFF}, cpu::getS);

        // Branches longues conditionnelles (Page 2, préfixe 0x10)
        // Les opcodes 0x1020-0x102F sont les branches longues conditionnelles 16 bits
        Runnable longBranchHandler = () -> {
            int offset = mode.lireMot(); // Offset 16 bits
            cpu.setPC((cpu.getPC() + offset) & 0xFFFF);
        };
        for (int i = 0x20; i <= 0x2F; i++) {
            // Seuls les opcodes 0x102x sont des branches conditionnelles longues.
            // On s'assure d'appeler le handler de branchement long.
            page2[i] = longBranchHandler;
        }
    }

    private void executerPage2() {
        int op = mode.lireOctet(); // Lit le deuxième octet (l'opcode réel)
        if (page2[op] != null) {
            page2[op].run();
        } else {
            throw new IllegalStateException(String.format("Opcode inconnu Page 2 à l'adresse $%04X: 10 %02X", (cpu.getPC() - 2) & 0xFFFF, op));
        }
    }

    // Helpers spécifiques pour la Page 2 (avec préfixe 0x10)
    private void initCmp16Page2(int[] opcodes, RegGetter regGetter) {
        if (opcodes[0] >= 0) page2[opcodes[0] & 0xFF] = () -> alu.cmp16(regGetter.get(), mode.lireMot());
        if (opcodes[1] >= 0) page2[opcodes[1] & 0xFF] = () -> alu.cmp16(regGetter.get(), mem.lireMot(mode.direct()));
        if (opcodes[2] >= 0) page2[opcodes[2] & 0xFF] = () -> alu.cmp16(regGetter.get(), mem.lireMot(mode.indexe()));
        if (opcodes[3] >= 0) page2[opcodes[3] & 0xFF] = () -> alu.cmp16(regGetter.get(), mem.lireMot(mode.etendu()));
    }
    private void initLoad16Page2(int[] opcodes, RegSetter regSetter, RegGetter regGetter) {
        if (opcodes[0] >= 0) page2[opcodes[0] & 0xFF] = () -> { regSetter.set(mode.lireMot()); alu.updateFlags16(regGetter.get()); };
        if (opcodes[1] >= 0) page2[opcodes[1] & 0xFF] = () -> { regSetter.set(mem.lireMot(mode.direct())); alu.updateFlags16(regGetter.get()); };
        if (opcodes[2] >= 0) page2[opcodes[2] & 0xFF] = () -> { regSetter.set(mem.lireMot(mode.indexe())); alu.updateFlags16(regGetter.get()); };
        if (opcodes[3] >= 0) page2[opcodes[3] & 0xFF] = () -> { regSetter.set(mem.lireMot(mode.etendu())); alu.updateFlags16(regGetter.get()); };
    }
    private void initStore16Page2(int[] opcodes, RegGetter regGetter) {
        if (opcodes[0] >= 0) page2[opcodes[0] & 0xFF] = () -> { mem.ecrireMot(mode.direct(), regGetter.get()); alu.updateFlags16(regGetter.get()); };
        if (opcodes[1] >= 0) page2[opcodes[1] & 0xFF] = () -> { mem.ecrireMot(mode.indexe(), regGetter.get()); alu.updateFlags16(regGetter.get()); };
        if (opcodes[2] >= 0) page2[opcodes[2] & 0xFF] = () -> { mem.ecrireMot(mode.etendu(), regGetter.get()); alu.updateFlags16(regGetter.get()); };
    }

    // ==================== PAGE 3 (Opcode préfixé par 0x11) ====================

    private void initialiserPage3() {
        // CMPU (Page 3)
        initCmp16Page3(new int[]{0x83,0x93,0xA3,0xB3}, cpu::getU);

        // CMPS (Page 3)
        initCmp16Page3(new int[]{0x8C,0x9C,0xAC,0xBC}, cpu::getS);
    }

    private void executerPage3() {
        int op = mode.lireOctet(); // Lit le deuxième octet (l'opcode réel)
        if (page3[op] != null) {
            page3[op].run();
        } else {
            throw new IllegalStateException(String.format("Opcode inconnu Page 3 à l'adresse $%04X: 11 %02X", (cpu.getPC() - 2) & 0xFFFF, op));
        }
    }

    // Helpers spécifiques pour la Page 3 (avec préfixe 0x11)
    private void initCmp16Page3(int[] opcodes, RegGetter regGetter) {
        if (opcodes[0] >= 0) page3[opcodes[0] & 0xFF] = () -> alu.cmp16(regGetter.get(), mode.lireMot());
        if (opcodes[1] >= 0) page3[opcodes[1] & 0xFF] = () -> alu.cmp16(regGetter.get(), mem.lireMot(mode.direct()));
        if (opcodes[2] >= 0) page3[opcodes[2] & 0xFF] = () -> alu.cmp16(regGetter.get(), mem.lireMot(mode.indexe()));
        if (opcodes[3] >= 0) page3[opcodes[3] & 0xFF] = () -> alu.cmp16(regGetter.get(), mem.lireMot(mode.etendu()));
    }


    // ==================== UTILITAIRES INTERNES ====================

    /**
     * Effectue une opération Read-Modify-Write sur une adresse mémoire.
     * @param adr L'adresse mémoire.
     * @param op L'opérateur unaire à appliquer à l'octet lu.
     */
    private void memOp(int adr, IntUnaryOperator op) {
        mem.ecrire(adr, op.applyAsInt(mem.lire(adr)));
    }

    /**
     * Met à jour les flags Zero et Negative pour une valeur 16 bits.
     * (Principalement utilisé par LEA X, Y, qui affecte N et Z, mais pas V ou C)
     * @param val La valeur 16 bits.
     */
    private void updateZ16(int val) {
        if ((val & 0xFFFF) == 0) cpu.setCC(cpu.getCC() | ALU.FLAG_Z);
        else cpu.setCC(cpu.getCC() & ~ALU.FLAG_Z);
        // Note: LEA X/Y met aussi à jour N
        if ((val & 0x8000) != 0) cpu.setCC(cpu.getCC() | ALU.FLAG_N);
        else cpu.setCC(cpu.getCC() & ~ALU.FLAG_N);
        // Les flags V et C ne sont pas affectés par LEA
        // ALU.updateFlags16 gère déjà N,Z,V, donc cette méthode est un peu redondante si ALU.updateFlags16 est utilisé partout.
        // Dans le cas de LEA, seuls N et Z sont affectés. Il faudrait une méthode `alu.updateNZ16(val)`.
    }

    /**
     * Implémentation de l'instruction DAA (Decimal Adjust Accumulator A).
     * Ajuste le contenu de l'accumulateur A après une addition BCD.
     * Met à jour les flags N, Z, C. Le flag V n'est pas affecté.
     */
    private void daa() {
        int a = cpu.getA();
        int cc = cpu.getCC();
        int correction = 0;

        // Si le nibble bas est > 9 ou le Half-Carry est set
        if ((a & 0x0F) > 9 || (cc & ALU.FLAG_H) != 0) {
            correction += 0x06;
        }
        // Si le nibble haut (ou tout l'octet) est > 0x99 ou le Carry est set
        if (a > 0x99 || (cc & ALU.FLAG_C) != 0) {
            correction += 0x60;
            cc |= ALU.FLAG_C; // Le Carry est set si la correction 0x60 est appliquée
        }

        a = (a + correction) & 0xFF; // Applique la correction
        cpu.setA(a); // Met à jour l'accumulateur A
        
        // Les flags V (Overflow) et H (Half-Carry) ne sont pas affectés par DAA
        // Met à jour N et Z en fonction du nouveau contenu de A
        cc = (cc & ~(ALU.FLAG_N | ALU.FLAG_Z)); // Efface N et Z
        if (a == 0) cc |= ALU.FLAG_Z;
        if ((a & 0x80) != 0) cc |= ALU.FLAG_N;
        cpu.setCC(cc); // Met à jour le registre CC
    }

    // ==================== INTERFACES FONCTIONNELLES UTILITAIRES ====================

    @FunctionalInterface
    private interface RegGetter { int get(); } // Pour obtenir la valeur d'un registre

    @FunctionalInterface
    private interface RegSetter { void set(int val); } // Pour définir la valeur d'un registre

    @FunctionalInterface
    private interface Op8 { int apply(int a, int b); } // Pour les opérations 8 bits binaires (a, b)
    
    @FunctionalInterface
    private interface Op16 { int apply(int a, int b); } // Pour les opérations 16 bits binaires (a, b)
}

