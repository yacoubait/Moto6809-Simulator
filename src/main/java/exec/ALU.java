package exec;

import cpu.CPU6809;

public class ALU {
    private final CPU6809 cpu;

    // Masques des flags du registre CC (Condition Codes)
    public static final int FLAG_C = 0x01;  // Bit 0: Carry
    public static final int FLAG_V = 0x02;  // Bit 1: Overflow
    public static final int FLAG_Z = 0x04;  // Bit 2: Zero
    public static final int FLAG_N = 0x08;  // Bit 3: Negative
    public static final int FLAG_I = 0x10;  // Bit 4: IRQ Mask
    public static final int FLAG_H = 0x20;  // Bit 5: Half Carry
    public static final int FLAG_F = 0x40;  // Bit 6: FIRQ Mask
    public static final int FLAG_E = 0x80;  // Bit 7: Entire (Fast Interrupt Mask)

    public ALU(CPU6809 cpu) {
        this.cpu = cpu;
    }

    // ==================== GESTION DES FLAGS ====================
    
    /**
     * Définit ou efface des flags dans le registre CC.
     * @param mask Masque des flags à modifier.
     * @param condition true pour définir les flags, false pour les effacer.
     */
    private void setFlags(int mask, boolean condition) {
        if (condition) {
            cpu.setCC(cpu.getCC() | mask); // Définit les bits du masque
        } else {
            cpu.setCC(cpu.getCC() & ~mask); // Efface les bits du masque
        }
    }

    /**
     * Efface des flags spécifiques dans le registre CC.
     * @param mask Masque des flags à effacer.
     */
    private void clearFlags(int mask) {
        cpu.setCC(cpu.getCC() & ~mask);
    }

    /**
     * Vérifie l'état du flag Carry.
     * @return true si le flag Carry est défini, false sinon.
     */
    public boolean getCarry() {
        return (cpu.getCC() & FLAG_C) != 0;
    }

    // ==================== OPERATIONS ARITHMÉTIQUES ET LOGIQUES 8 BITS ====================

    /**
     * Effectue une addition de 8 bits (A + B + CarryIn) et met à jour les flags.
     * @param a Le premier opérande.
     * @param b Le deuxième opérande.
     * @param withCarry Indique si le flag Carry doit être inclus dans l'addition.
     * @return Le résultat tronqué à 8 bits.
     */
    public int add8(int a, int b, boolean withCarry) {
        int carry = (withCarry && getCarry()) ? 1 : 0;
        int result = a + b + carry;

        // Effacer les flags H, N, Z, V, C avant de les mettre à jour
        clearFlags(FLAG_H | FLAG_N | FLAG_Z | FLAG_V | FLAG_C);
        setFlags(FLAG_C, (result & 0x100) != 0); // Carry si résultat > 0xFF
        setFlags(FLAG_Z, (result & 0xFF) == 0);  // Zero si le résultat 8 bits est 0
        setFlags(FLAG_N, (result & 0x80) != 0);  // Negative si le bit 7 est défini
        setFlags(FLAG_V, ((a ^ result) & (b ^ result) & 0x80) != 0); // Overflow signé
        setFlags(FLAG_H, ((a & 0xF) + (b & 0xF) + carry) > 0xF); // Half Carry pour BCD

        return result & 0xFF;
    }

    /**
     * Effectue une soustraction de 8 bits (A - B - BorrowIn) et met à jour les flags.
     * @param a Le premier opérande.
     * @param b Le deuxième opérande.
     * @param withBorrow Indique si le flag Carry (utilisé comme Borrow) doit être inclus.
     * @return Le résultat tronqué à 8 bits.
     */
    public int sub8(int a, int b, boolean withBorrow) {
        int borrow = (withBorrow && getCarry()) ? 1 : 0;
        int result = a - b - borrow;

        // Effacer les flags N, Z, V, C avant de les mettre à jour
        clearFlags(FLAG_N | FLAG_Z | FLAG_V | FLAG_C);
        setFlags(FLAG_C, result < 0); // Carry/Borrow si résultat < 0
        setFlags(FLAG_Z, (result & 0xFF) == 0);  // Zero si le résultat 8 bits est 0
        setFlags(FLAG_N, (result & 0x80) != 0);  // Negative si le bit 7 est défini
        setFlags(FLAG_V, ((a ^ b) & (a ^ result) & 0x80) != 0); // Overflow signé

        return result & 0xFF;
    }

    /**
     * Effectue une opération AND logique de 8 bits et met à jour les flags N, Z, V.
     * @param a Le premier opérande.
     * @param b Le deuxième opérande.
     * @return Le résultat tronqué à 8 bits.
     */
    public int and(int a, int b) {
        return logique(a & b);
    }

    /**
     * Effectue une opération OR logique de 8 bits et met à jour les flags N, Z, V.
     * @param a Le premier opérande.
     * @param b Le deuxième opérande.
     * @return Le résultat tronqué à 8 bits.
     */
    public int or(int a, int b) {
        return logique(a | b);
    }

    /**
     * Effectue une opération XOR logique de 8 bits et met à jour les flags N, Z, V.
     * @param a Le premier opérande.
     * @param b Le deuxième opérande.
     * @return Le résultat tronqué à 8 bits.
     */
    public int xor(int a, int b) {
        return logique(a ^ b);
    }

    /**
     * Méthode générique pour les opérations logiques, met à jour les flags N, Z, V.
     * Le flag V est toujours 0 pour les opérations logiques.
     * @param result Le résultat de l'opération logique.
     * @return Le résultat tronqué à 8 bits.
     */
    private int logique(int result) {
        clearFlags(FLAG_N | FLAG_Z | FLAG_V); // V est toujours 0 pour les logiques
        setFlags(FLAG_Z, (result & 0xFF) == 0);
        setFlags(FLAG_N, (result & 0x80) != 0);
        return result & 0xFF;
    }

    /**
     * Effectue une négation (complément à deux) de 8 bits et met à jour les flags.
     * @param val La valeur à négater.
     * @return Le résultat tronqué à 8 bits.
     */
    public int neg(int val) {
        // -val est équivalent à (~val + 1)
        int result = (-val) & 0xFF; 
        
        clearFlags(FLAG_N | FLAG_Z | FLAG_V | FLAG_C);
        setFlags(FLAG_C, val != 0); // Carry est défini si val n'était pas 0
        setFlags(FLAG_Z, result == 0);
        setFlags(FLAG_N, (result & 0x80) != 0);
        setFlags(FLAG_V, val == 0x80); // Overflow si -128 (0x80) est négaté

        return result;
    }

    /**
     * Effectue un complément à un (NOT) de 8 bits et met à jour les flags.
     * @param val La valeur à complémenter.
     * @return Le résultat tronqué à 8 bits.
     */
    public int com(int val) {
        int result = (~val) & 0xFF;
        
        clearFlags(FLAG_N | FLAG_Z | FLAG_V); // V est toujours 0
        setFlags(FLAG_C, true);  // Carry est toujours 1
        setFlags(FLAG_Z, result == 0);
        setFlags(FLAG_N, (result & 0x80) != 0);

        return result;
    }

    /**
     * Incrémente une valeur de 8 bits et met à jour les flags.
     * @param val La valeur à incrémenter.
     * @return Le résultat tronqué à 8 bits.
     */
    public int inc(int val) {
        int result = (val + 1) & 0xFF;
        
        clearFlags(FLAG_N | FLAG_Z | FLAG_V); // C n'est pas affecté
        setFlags(FLAG_Z, result == 0);
        setFlags(FLAG_N, (result & 0x80) != 0);
        setFlags(FLAG_V, val == 0x7F); // Overflow si 127 devient -128

        return result;
    }

    /**
     * Décrémente une valeur de 8 bits et met à jour les flags.
     * @param val La valeur à décrémenter.
     * @return Le résultat tronqué à 8 bits.
     */
    public int dec(int val) {
        int result = (val - 1) & 0xFF;
        
        clearFlags(FLAG_N | FLAG_Z | FLAG_V); // C n'est pas affecté
        setFlags(FLAG_Z, result == 0);
        setFlags(FLAG_N, (result & 0x80) != 0);
        setFlags(FLAG_V, val == 0x80); // Overflow si -128 devient 127

        return result;
    }

    /**
     * Teste une valeur de 8 bits et met à jour les flags N, Z, V.
     * @param val La valeur à tester.
     */
    public void tst(int val) {
        clearFlags(FLAG_N | FLAG_Z | FLAG_V); // C n'est pas affecté, V est toujours 0
        setFlags(FLAG_Z, (val & 0xFF) == 0);
        setFlags(FLAG_N, (val & 0x80) != 0);
    }

    /**
     * Met à zéro une valeur de 8 bits et met à jour les flags.
     * @return Toujours 0.
     */
    public int clr() {
        clearFlags(FLAG_N | FLAG_V | FLAG_C); // N, V, C sont 0
        setFlags(FLAG_Z, true); // Z est 1
        return 0;
    }

    // ==================== SHIFTS & ROTATES 8 BITS ====================

    /**
     * Décale arithmétiquement vers la gauche (ASL/LSL) de 8 bits et met à jour les flags.
     * @param val La valeur à décaler.
     * @return Le résultat tronqué à 8 bits.
     */
    public int asl(int val) {
        int result = (val << 1) & 0xFF;
        boolean carryOut = (val & 0x80) != 0;

        clearFlags(FLAG_N | FLAG_Z | FLAG_V | FLAG_C);
        setFlags(FLAG_C, carryOut);
        setFlags(FLAG_Z, result == 0);
        setFlags(FLAG_N, (result & 0x80) != 0);
        setFlags(FLAG_V, ((result & 0x80) != 0) ^ carryOut);

        return result;
    }

    /**
     * Décale arithmétiquement vers la droite (ASR) de 8 bits et met à jour les flags.
     * @param val La valeur à décaler.
     * @return Le résultat tronqué à 8 bits (le bit de signe est préservé).
     */
    public int asr(int val) {
        int result = (val >> 1) | (val & 0x80);  // Garde le bit de signe (bit 7)
        
        clearFlags(FLAG_N | FLAG_Z | FLAG_C); // V est toujours 0
        setFlags(FLAG_C, (val & 1) == 1); // Le bit 0 est décalé dans Carry
        setFlags(FLAG_Z, result == 0);
        setFlags(FLAG_N, (result & 0x80) != 0);

        return result;
    }

    /**
     * Décale logiquement vers la droite (LSR) de 8 bits et met à jour les flags.
     * @param val La valeur à décaler.
     * @return Le résultat tronqué à 8 bits (le bit de signe est toujours 0 après).
     */
    public int lsr(int val) {
        int result = val >> 1; // Le bit 7 devient 0
        
        clearFlags(FLAG_N | FLAG_Z | FLAG_C); // N est toujours 0, V est toujours 0
        setFlags(FLAG_C, (val & 1) == 1); // Le bit 0 est décalé dans Carry
        setFlags(FLAG_Z, result == 0);
        // N est toujours 0 après LSR, donc pas besoin de le définir

        return result;
    }

    /**
     * Rotate Left (ROL) de 8 bits (inclut le flag Carry) et met à jour les flags.
     * @param val La valeur à faire pivoter.
     * @return Le résultat tronqué à 8 bits.
     */
    public int rol(int val) {
        int carryIn = getCarry() ? 1 : 0;
        boolean carryOut = (val & 0x80) != 0;
        int result = ((val << 1) | carryIn) & 0xFF;

        clearFlags(FLAG_N | FLAG_Z | FLAG_V | FLAG_C);
        setFlags(FLAG_C, carryOut);
        setFlags(FLAG_Z, result == 0);
        setFlags(FLAG_N, (result & 0x80) != 0);
        setFlags(FLAG_V, ((result & 0x80) != 0) ^ carryOut);

        return result;
    }


    /**
     * Rotate Right (ROR) de 8 bits (inclut le flag Carry) et met à jour les flags.
     * @param val La valeur à faire pivoter.
     * @return Le résultat tronqué à 8 bits.
     */
    public int ror(int val) {
        int carryIn = getCarry() ? 0x80 : 0; // Le Carry entre par le bit 7
        int result = (val >> 1) | carryIn;
        
        clearFlags(FLAG_N | FLAG_Z | FLAG_C); // V est toujours 0
        setFlags(FLAG_C, (val & 1) == 1); // Le bit 0 est décalé dans Carry
        setFlags(FLAG_Z, result == 0);
        setFlags(FLAG_N, (result & 0x80) != 0);

        return result;
    }

    // ==================== OPERATIONS 16 BITS ====================

    /**
     * Effectue une addition de 16 bits (A + B) et met à jour les flags N, Z, V, C.
     * @param a Le premier opérande.
     * @param b Le deuxième opérande.
     * @return Le résultat tronqué à 16 bits.
     */
    public int add16(int a, int b) {
        int result = a + b;

        clearFlags(FLAG_N | FLAG_Z | FLAG_V | FLAG_C);
        setFlags(FLAG_C, (result & 0x10000) != 0); // Carry si résultat > 0xFFFF
        setFlags(FLAG_Z, (result & 0xFFFF) == 0);  // Zero si le résultat 16 bits est 0
        setFlags(FLAG_N, (result & 0x8000) != 0);  // Negative si le bit 15 est défini
        setFlags(FLAG_V, ((a ^ result) & (b ^ result) & 0x8000) != 0); // Overflow signé

        return result & 0xFFFF;
    }

    /**
     * Effectue une soustraction de 16 bits (A - B) et met à jour les flags N, Z, V, C.
     * @param a Le premier opérande.
     * @param b Le deuxième opérande.
     * @return Le résultat tronqué à 16 bits.
     */
    public int sub16(int a, int b) {
        int result = a - b;

        clearFlags(FLAG_N | FLAG_Z | FLAG_V | FLAG_C);
        setFlags(FLAG_C, result < 0); // Carry/Borrow si résultat < 0
        setFlags(FLAG_Z, (result & 0xFFFF) == 0);  // Zero si le résultat 16 bits est 0
        setFlags(FLAG_N, (result & 0x8000) != 0);  // Negative si le bit 15 est défini
        setFlags(FLAG_V, ((a ^ b) & (a ^ result) & 0x8000) != 0); // Overflow signé

        return result & 0xFFFF;
    }

    /**
     * Compare deux valeurs de 16 bits. Met à jour les flags N, Z, V, C (comme une soustraction).
     * @param a Le premier opérande.
     * @param b Le deuxième opérande.
     */
    public void cmp16(int a, int b) {
        sub16(a, b);  // Juste pour les flags, le résultat n'est pas stocké
    }

    /**
     * Met à jour les flags N, Z, V pour une valeur de 8 bits.
     * (V est toujours 0 si non affecté par l'opération réelle)
     * @param val La valeur.
     */
    public void updateFlags8(int val) {
        clearFlags(FLAG_N | FLAG_Z | FLAG_V);
        setFlags(FLAG_Z, (val & 0xFF) == 0);
        setFlags(FLAG_N, (val & 0x80) != 0);
    }

    /**
     * Met à jour les flags N, Z, V pour une valeur de 16 bits.
     * (V est toujours 0 si non affecté par l'opération réelle)
     * @param val La valeur.
     */
    public void updateFlags16(int val) {
        clearFlags(FLAG_N | FLAG_Z | FLAG_V);
        setFlags(FLAG_Z, (val & 0xFFFF) == 0);
        setFlags(FLAG_N, (val & 0x8000) != 0);
    }

    // ==================== MUL (Multiplication) ====================

    /**
     * Effectue une multiplication non signée de 8 bits (A * B).
     * Le résultat est de 16 bits, stocké dans D (A:B).
     * Met à jour les flags Z et C. V et N sont toujours 0.
     * @param a Le premier opérande (accumulateur A).
     * @param b Le deuxième opérande (accumulateur B).
     * @return Le résultat de 16 bits.
     */
    public int mul(int a, int b) {
        int result = (a & 0xFF) * (b & 0xFF); // Multiplication non signée
        
        clearFlags(FLAG_N | FLAG_V | FLAG_Z | FLAG_C); // N et V sont toujours 0 pour MUL
        setFlags(FLAG_Z, result == 0);
        setFlags(FLAG_C, (result & 0x80) != 0); // Carry est le bit 7 du résultat (spécifique au 6809)

        return result & 0xFFFF;
    }
}
