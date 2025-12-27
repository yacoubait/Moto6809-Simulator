package exec;

import cpu.CPU6809;
import mem.Memoire;

/**
 * Gère le calcul des adresses effectives et la lecture des opérandes
 * pour les différents modes d'adressage du CPU 6809 lors de l'exécution.
 */
public class ModeAdressage {
    private final CPU6809 cpu;
    private final Memoire mem;

    public ModeAdressage(CPU6809 cpu, Memoire mem) {
        this.cpu = cpu;
        this.mem = mem;
    }

    // --- Lecture immédiate (incrémente le PC) ---

    /**
     * Lit l'octet pointé par le PC et incrémente le PC.
     * @return L'octet lu.
     */
    public int lireOctet() {
        int v = mem.lire(cpu.getPC());
        cpu.setPC((cpu.getPC() + 1) & 0xFFFF); // Incrémente PC et masque à 16 bits
        return v;
    }

    /**
     * Lit le mot (2 octets) pointé par le PC et incrémente le PC de 2.
     * @return Le mot lu.
     */
    public int lireMot() {
        int v = mem.lireMot(cpu.getPC());
        cpu.setPC((cpu.getPC() + 2) & 0xFFFF); // Incrémente PC de 2 et masque à 16 bits
        return v;
    }

    // --- Calcul d'adresses effectives ---

    /**
     * Calcule l'adresse effective pour le mode direct.
     * L'adresse est formée par le registre DP (Direct Page) et un octet lu de la mémoire.
     * @return L'adresse effective 16 bits.
     */
    public int direct() {
        return ((cpu.getDP() & 0xFF) << 8) | lireOctet();
    }

    /**
     * Calcule l'adresse effective pour le mode étendu.
     * L'adresse est directement lue comme un mot de 16 bits de la mémoire.
     * @return L'adresse effective 16 bits.
     */
    public int etendu() {
        return lireMot();
    }

    /**
     * Calcule l'adresse effective pour le mode indexé.
     * Implémente la logique complexe du post-byte du 6809.
     * @return L'adresse effective 16 bits.
     * @throws RuntimeException en cas de mode indexé invalide.
     */
    public int indexe() {
        int postByte = lireOctet(); // Le post-byte est lu immédiatement après l'opcode

        int registreCode = (postByte >> 5) & 0x03; // Bits 6 et 5 pour le registre (X, Y, U, S)
        int baseRegistreValue = getRegistreIndexe(registreCode);
        
        boolean indirect = (postByte & 0x10) != 0; // Bit 4 indique l'adressage indirect (si complexe)

        int adresse;

        // Mode simple: offset 5 bits signé (bit 7 est 0)
        if ((postByte & 0x80) == 0) {
            int offset = postByte & 0x1F;
            if ((offset & 0x10) != 0) { // Test du bit 4 pour l'extension de signe (valeurs négatives)
                offset |= 0xFFFFFFE0; // Étend le signe pour 5 bits (-16 à +15)
            }
            adresse = (baseRegistreValue + offset) & 0xFFFF;
        } else {
            // Modes complexes (bit 7 est 1)
            adresse = calculerIndexeComplexe(postByte, baseRegistreValue);
        }

        // Si l'adressage est indirect, l'adresse calculée est elle-même l'adresse
        // où se trouve l'adresse effective finale.
        return indirect ? mem.lireMot(adresse) : adresse;
    }

    /**
     * Retourne la valeur du registre indexé (X, Y, U, S) à partir de son code 2 bits.
     * @param code Le code 2 bits du registre (0=X, 1=Y, 2=U, 3=S).
     * @return La valeur 16 bits du registre.
     */
    private int getRegistreIndexe(int code) {
        switch (code) {
            case 0: return cpu.getX();
            case 1: return cpu.getY();
            case 2: return cpu.getU();
            case 3: return cpu.getS();
            default: throw new RuntimeException("Code registre indexé invalide: " + code); // Ne devrait pas arriver
        }
    }

    /**
     * Calcule l'adresse effective pour les modes indexés complexes (post-byte bit 7 = 1).
     * @param postByte Le post-byte complet.
     * @param baseRegistreValue La valeur du registre de base (X, Y, U, S).
     * @return L'adresse effective 16 bits.
     * @throws RuntimeException en cas de mode indexé complexe invalide.
     */
    private int calculerIndexeComplexe(int postByte, int baseRegistreValue) {
        int modeBits = postByte & 0x0F; // Bits 3-0 du post-byte
        int adresse = baseRegistreValue; // Adresse de base pour les calculs

        switch (modeBits) {
            case 0x00: // ,R+ (post-incrément par 1)
                incrementerRegistre((postByte >> 5) & 3, 1); // R est incrémenté APRES utilisation
                break;
            case 0x01: // ,R++ (post-incrément par 2)
                incrementerRegistre((postByte >> 5) & 3, 2); // R est incrémenté APRES utilisation
                break;
            case 0x02: // ,-R (pré-décrément par 1)
                decrementRegistre((postByte >> 5) & 3, 1); // R est décrémenté AVANT utilisation
                adresse = getRegistreIndexe((postByte >> 5) & 3); // L'adresse est celle APRES décrémentation
                break;
            case 0x03: // ,--R (pré-décrément par 2)
                decrementRegistre((postByte >> 5) & 3, 2); // R est décrémenté AVANT utilisation
                adresse = getRegistreIndexe((postByte >> 5) & 3); // L'adresse est celle APRES décrémentation
                break;
            case 0x04: // 0,R ou ,R (offset de 0)
                // L'adresse reste la valeur du registre de base
                break;
            case 0x05: // B,R (offset par registre B)
                adresse = (baseRegistreValue + signExtend8(cpu.getB())) & 0xFFFF;
                break;
            case 0x06: // A,R (offset par registre A)
                adresse = (baseRegistreValue + signExtend8(cpu.getA())) & 0xFFFF;
                break;
            case 0x08: // n,R (offset 8 bits signé)
                adresse = (baseRegistreValue + signExtend8(lireOctet())) & 0xFFFF; // Lit un octet d'offset
                break;
            case 0x09: // n,R (offset 16 bits signé)
                adresse = (baseRegistreValue + lireMot()) & 0xFFFF; // Lit un mot d'offset
                break;
            case 0x0B: // D,R (offset par registre D)
                adresse = (baseRegistreValue + cpu.getD()) & 0xFFFF; // Utilise cpu.getD()
                break;
            case 0x0C: // n,PC (offset 8 bits signé)
                // L'offset PC-relative est calculé par rapport à l'adresse du prochain byte après l'offset lui-même.
                // Le PC a déjà été incrémenté pour le post-byte.
                adresse = (cpu.getPC() + signExtend8(lireOctet())) & 0xFFFF; // Lit un octet d'offset
                break;
            case 0x0D: // n,PC (offset 16 bits signé)
                // Similaire à 0x0C
                adresse = (cpu.getPC() + lireMot()) & 0xFFFF; // Lit un mot d'offset
                break;
            case 0x0F: // Ce cas (Extended Indirect) est pour les opcodes comme 0x9F (LDX [addr])
                       // Il ne fait pas partie des modes indexés avec post-byte 0x0F pour le 6809.
                       // Si `indirect` est aussi activé (postByte & 0x10), la dernière ligne de `indexe()` gère [effective address].
                       // Une implémentation ici pourrait être incorrecte. L'hypothèse est qu'il n'y a pas de mode indexé 0x0F.
                throw new RuntimeException("Mode indexé complexe invalide (0x0F) à l'adresse PC: $" + String.format("%04X", cpu.getPC() -1));
            default: 
                 throw new RuntimeException("Mode indexé complexe non reconnu: " + String.format("%02X", modeBits) + " à l'adresse PC: $" + String.format("%04X", cpu.getPC() - 1));
        }

        return adresse;
    }

    /**
     * Incrémente un registre indexé (X, Y, U, S).
     * @param code Le code 2 bits du registre.
     * @param delta La valeur d'incrémentation.
     */
    private void incrementerRegistre(int code, int delta) {
        switch (code) {
            case 0: cpu.setX((cpu.getX() + delta) & 0xFFFF); break;
            case 1: cpu.setY((cpu.getY() + delta) & 0xFFFF); break;
            case 2: cpu.setU((cpu.getU() + delta) & 0xFFFF); break;
            case 3: cpu.setS((cpu.getS() + delta) & 0xFFFF); break;
        }
    }

    /**
     * Décrémente un registre indexé (X, Y, U, S).
     * @param code Le code 2 bits du registre.
     * @param delta La valeur de décrémentation.
     */
    private void decrementRegistre(int code, int delta) {
        switch (code) {
            case 0: cpu.setX((cpu.getX() - delta) & 0xFFFF); break;
            case 1: cpu.setY((cpu.getY() - delta) & 0xFFFF); break;
            case 2: cpu.setU((cpu.getU() - delta) & 0xFFFF); break;
            case 3: cpu.setS((cpu.getS() - delta) & 0xFFFF); break;
        }
    }

    /**
     * Étend le signe d'une valeur de 8 bits à 16 bits.
     * @param val La valeur 8 bits.
     * @return La valeur 16 bits avec le signe étendu.
     */
    private int signExtend8(int val) {
        return (val & 0x80) != 0 ? val | 0xFFFFFF00 : val; // Si bit 7 est 1, remplir les bits supérieurs avec des 1
    }
}
