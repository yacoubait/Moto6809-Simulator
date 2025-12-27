package exec;

import cpu.CPU6809;
import mem.Memoire;

public class GestionPile {
    private final CPU6809 cpu;
    private final Memoire mem;

    public GestionPile(CPU6809 cpu, Memoire mem) {
        this.cpu = cpu;
        this.mem = mem;
    }

    // ==================== PUSH/PULL MOT (pour JSR/RTS) ====================
    // Ces méthodes sont pour les appels de sous-programmes et retour,
    // où le PC est empilé/dépilé. Elles doivent toujours empiler/dépiler 16 bits.

    public void pushS(int val) {
        // High-byte empilé en premier (adresse la plus basse de la zone de 2 octets)
        cpu.setS((cpu.getS() - 1) & 0xFFFF); 
        mem.ecrire(cpu.getS(), (val >> 8) & 0xFF, true); // <<< MODIFIÉ: isStackOperation = true
        
        // Low-byte empilé ensuite (adresse plus haute de la zone de 2 octets)
        cpu.setS((cpu.getS() - 1) & 0xFFFF); 
        mem.ecrire(cpu.getS(), val & 0xFF, true); // <<< MODIFIÉ: isStackOperation = true
    }

    public int pullS() {
        int hi = mem.lire(cpu.getS()); 
        cpu.setS((cpu.getS() + 1) & 0xFFFF); 
        int lo = mem.lire(cpu.getS()); 
        cpu.setS((cpu.getS() + 1) & 0xFFFF); 
        return (hi << 8) | lo; 
    }

    public void pushU(int val) {
        cpu.setU((cpu.getU() - 1) & 0xFFFF);
        mem.ecrire(cpu.getU(), (val >> 8) & 0xFF, true); // <<< MODIFIÉ: isStackOperation = true
        cpu.setU((cpu.getU() - 1) & 0xFFFF);
        mem.ecrire(cpu.getU(), val & 0xFF, true); // <<< MODIFIÉ: isStackOperation = true
    }

    public int pullU() {
        int hi = mem.lire(cpu.getU());
        cpu.setU((cpu.getU() + 1) & 0xFFFF);
        int lo = mem.lire(cpu.getU());
        cpu.setU((cpu.getU() + 1) & 0xFFFF);
        return (hi << 8) | lo;
    }

    // ==================== PSHS/PULS/PSHU/PULU (Empilement/Dépilement de registres multiples) ====================

    public void pshs(int postByte) {
        pushRegistres(postByte, true); 
    }

    public void puls(int postByte) {
        pullRegistres(postByte, true); 
    }

    public void pshu(int postByte) {
        pushRegistres(postByte, false); 
    }

    public void pulu(int postByte) {
        pullRegistres(postByte, false); 
    }

    private void pushRegistres(int postByte, boolean useS) {
        if ((postByte & 0x80) != 0) pushWordInternal(cpu.getPC(), useS);      
        if ((postByte & 0x40) != 0) { 
             if (useS) pushWordInternal(cpu.getU(), useS); 
             else pushWordInternal(cpu.getS(), useS);     
        }
        if ((postByte & 0x20) != 0) pushWordInternal(cpu.getY(), useS);      
        if ((postByte & 0x10) != 0) pushWordInternal(cpu.getX(), useS);      
        if ((postByte & 0x08) != 0) pushByteInternal(cpu.getDP(), useS);     
        if ((postByte & 0x04) != 0) pushByteInternal(cpu.getB(), useS);      
        if ((postByte & 0x02) != 0) pushByteInternal(cpu.getA(), useS);      
        if ((postByte & 0x01) != 0) pushByteInternal(cpu.getCC(), useS);     
    }

    private void pullRegistres(int postByte, boolean useS) {
        if ((postByte & 0x01) != 0) cpu.setCC(pullByteInternal(useS));     
        if ((postByte & 0x02) != 0) cpu.setA(pullByteInternal(useS));      
        if ((postByte & 0x04) != 0) cpu.setB(pullByteInternal(useS));      
        if ((postByte & 0x08) != 0) cpu.setDP(pullByteInternal(useS));     
        if ((postByte & 0x10) != 0) cpu.setX(pullWordInternal(useS));      
        if ((postByte & 0x20) != 0) cpu.setY(pullWordInternal(useS));      
        if ((postByte & 0x40) != 0) { 
            if (useS) cpu.setU(pullWordInternal(true));    
            else cpu.setS(pullWordInternal(false));   
        }
        if ((postByte & 0x80) != 0) cpu.setPC(pullWordInternal(useS));     
    }


    // --- Méthodes internes d'empilement/dépilement (utilisées par les méthodes ci-dessus) ---

    private void pushByteInternal(int val, boolean useS) {
        if (useS) {
            cpu.setS((cpu.getS() - 1) & 0xFFFF);
            mem.ecrire(cpu.getS(), val & 0xFF, true); // <<< MODIFIÉ: isStackOperation = true
        } else {
            cpu.setU((cpu.getU() - 1) & 0xFFFF);
            mem.ecrire(cpu.getU(), val & 0xFF, true); // <<< MODIFIÉ: isStackOperation = true
        }
    }

    private void pushWordInternal(int val, boolean useS) {
        pushByteInternal((val >> 8) & 0xFF, useS); 
        pushByteInternal(val & 0xFF, useS);       
    }

    private int pullByteInternal(boolean useS) {
        int val;
        if (useS) {
            val = mem.lire(cpu.getS());
            cpu.setS((cpu.getS() + 1) & 0xFFFF);
        } else {
            val = mem.lire(cpu.getU());
            cpu.setU((cpu.getU() + 1) & 0xFFFF);
        }
        return val;
    }

    private int pullWordInternal(boolean useS) {
        int hi = pullByteInternal(useS); 
        int lo = pullByteInternal(useS); 
        return (hi << 8) | lo; 
    }
}
