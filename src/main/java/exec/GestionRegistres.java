package exec;

import cpu.CPU6809;
// import asm.ModeAdressage; // Import supprimé car non utilisé directement ici

public class GestionRegistres {
    private final CPU6809 cpu;
    // La variable "mode" (asm.ModeAdressage) n'est pas nécessaire ici et a été supprimée.
    // private final ModeAdressage mode; 

    public GestionRegistres(CPU6809 cpu) { // Le constructeur ne prend plus ModeAdressage
        this.cpu = cpu;
        // this.mode = mode; 
    }

    /**
     * Implémente l'instruction TFR (Transfert de Registre).
     * Transfère le contenu d'un registre source vers un registre destination.
     * @param postByte Le post-byte qui encode les registres source et destination.
     */
    public void tfr(int postByte) {
        int src = (postByte >> 4) & 0x0F; // Registre source (4 bits de poids fort)
        int dst = postByte & 0x0F;        // Registre destination (4 bits de poids faible)
        int val = lireRegistre(src);      // Lit la valeur du registre source
        ecrireRegistre(dst, val);         // Écrit la valeur dans le registre destination
    }

    /**
     * Implémente l'instruction EXG (Échange de Registres).
     * Échange les contenus de deux registres.
     * @param postByte Le post-byte qui encode les deux registres à échanger.
     */
    public void exg(int postByte) {
        int r1 = (postByte >> 4) & 0x0F; // Premier registre
        int r2 = postByte & 0x0F;        // Deuxième registre
        int val1 = lireRegistre(r1);     // Lit la valeur du premier registre
        int val2 = lireRegistre(r2);     // Lit la valeur du deuxième registre
        ecrireRegistre(r1, val2);        // Écrit la valeur du deuxième dans le premier
        ecrireRegistre(r2, val1);        // Écrit la valeur du premier dans le deuxième
    }

    /**
     * Lit la valeur d'un registre du CPU à partir de son code d'encodage (pour EXG/TFR).
     * @param code Le code 4 bits du registre.
     * @return La valeur du registre.
     */
    private int lireRegistre(int code) {
        switch (code) {
            case 0x0: return cpu.getD();  // Registre D (A:B)
            case 0x1: return cpu.getX();  // Registre X
            case 0x2: return cpu.getY();  // Registre Y
            case 0x3: return cpu.getU();  // Registre U
            case 0x4: return cpu.getS();  // Registre S
            case 0x5: return cpu.getPC(); // Registre PC
            case 0x8: return cpu.getA();  // Registre A
            case 0x9: return cpu.getB();  // Registre B
            case 0xA: return cpu.getCC(); // Registre CC
            case 0xB: return cpu.getDP(); // Registre DP
            default: return 0; // Code inconnu, retourne 0
        }
    }

    /**
     * Écrit une valeur dans un registre du CPU à partir de son code d'encodage.
     * @param code Le code 4 bits du registre.
     * @param val La valeur à écrire.
     */
    private void ecrireRegistre(int code, int val) {
        switch (code) {
            case 0x0: cpu.setD(val); break;        // Registre D (utilise setD pour A et B)
            case 0x1: cpu.setX(val & 0xFFFF); break; // Registre X (16 bits)
            case 0x2: cpu.setY(val & 0xFFFF); break; // Registre Y (16 bits)
            case 0x3: cpu.setU(val & 0xFFFF); break; // Registre U (16 bits)
            case 0x4: cpu.setS(val & 0xFFFF); break; // Registre S (16 bits)
            case 0x5: cpu.setPC(val & 0xFFFF); break; // Registre PC (16 bits)
            case 0x8: cpu.setA(val & 0xFF); break;   // Registre A (8 bits)
            case 0x9: cpu.setB(val & 0xFF); break;   // Registre B (8 bits)
            case 0xA: cpu.setCC(val & 0xFF); break;  // Registre CC (8 bits)
            case 0xB: cpu.setDP(val & 0xFF); break;  // Registre DP (8 bits)
            default: /* Ne rien faire pour les codes inconnus */ break;
        }
    }

    /**
     * Implémente l'instruction SEX (Sign Extend B into A).
     * Étend le bit de signe du registre B dans le registre A.
     * Met à jour les flags N et Z en fonction du registre D (A:B).
     */
    public void sex() {
        if ((cpu.getB() & 0x80) != 0) { // Si le bit de signe de B est 1
            cpu.setA(0xFF); // Remplir A avec des 1
        } else {
            cpu.setA(0x00); // Remplir A avec des 0
        }
        
        // Mise à jour des flags N et Z basée sur le registre D (A:B)
        int d = cpu.getD();
        int cc = cpu.getCC(); 
        cc &= ~ (ALU.FLAG_N | ALU.FLAG_Z); // Efface les flags N et Z
        if (d == 0) cc |= ALU.FLAG_Z;      // Définit Z si D est zéro
        if ((d & 0x8000) != 0) cc |= ALU.FLAG_N; // Définit N si le bit 15 de D est défini
        cpu.setCC(cc);
    }

    /**
     * Implémente l'instruction ABX (Add B to X).
     * Ajoute le contenu non signé de B au registre X.
     * @note Cette instruction n'affecte pas les flags.
     */
    public void abx() {
        cpu.setX((cpu.getX() + (cpu.getB() & 0xFF)) & 0xFFFF); // Ajout et tronque à 16 bits
    }
}

