package asm;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RegistreHelper {
    
    // Codes pour EXG/TFR (4 bits)
    // Mappe les noms de registres aux codes 4 bits utilisés dans le post-byte pour EXG/TFR.
    private static final Map<String, Integer> CODES_EXG_TFR = new HashMap<>();
    static {
        CODES_EXG_TFR.put("D", 0x0);   // Accumulateur D (A:B)
        CODES_EXG_TFR.put("X", 0x1);   // Registre d'index X
        CODES_EXG_TFR.put("Y", 0x2);   // Registre d'index Y
        CODES_EXG_TFR.put("U", 0x3);   // Pointeurs de pile U
        CODES_EXG_TFR.put("S", 0x4);   // Pointeurs de pile S
        CODES_EXG_TFR.put("PC", 0x5);  // Program Counter
        CODES_EXG_TFR.put("A", 0x8);   // Accumulateur A
        CODES_EXG_TFR.put("B", 0x9);   // Accumulateur B
        CODES_EXG_TFR.put("CC", 0xA);  // Condition Codes Register
        CODES_EXG_TFR.put("DP", 0xB);  // Direct Page Register
    }

    // Masques pour PSH/PUL (chaque bit représente un registre à empiler/dépiler)
    // Mappe les noms de registres aux masques binaires utilisés dans le post-byte pour PSH/PUL.
    private static final Map<String, Integer> MASQUES_PILE = new HashMap<>();
    static {
        MASQUES_PILE.put("CC", 0x01); // Bit 0: Condition Codes Register
        MASQUES_PILE.put("A", 0x02);  // Bit 1: Accumulateur A
        MASQUES_PILE.put("B", 0x04);  // Bit 2: Accumulateur B
        MASQUES_PILE.put("DP", 0x08); // Bit 3: Direct Page Register
        MASQUES_PILE.put("X", 0x10);  // Bit 4: Registre d'index X
        MASQUES_PILE.put("Y", 0x20);  // Bit 5: Registre d'index Y
        MASQUES_PILE.put("U", 0x40);  // Bit 6: Pointeurs de pile U (pour PSHS/PULS)
        MASQUES_PILE.put("S", 0x40);  // Bit 6: Pointeurs de pile S (pour PSHU/PULU) - le même bit est utilisé
        MASQUES_PILE.put("PC", 0x80); // Bit 7: Program Counter
    }

    // Codes registres pour mode indexé (2 bits)
    // Mappe les noms de registres aux codes 2 bits utilisés dans le post-byte pour le mode indexé (bits RR).
    private static final Map<String, Integer> CODES_INDEXE = new HashMap<>();
    static {
        CODES_INDEXE.put("X", 0); // 00 bin
        CODES_INDEXE.put("Y", 1); // 01 bin
        CODES_INDEXE.put("U", 2); // 10 bin
        CODES_INDEXE.put("S", 3); // 11 bin
    }

    /**
     * Génère le post-byte pour les instructions EXG ou TFR.
     * Le post-byte est formé de deux codes de registre de 4 bits (source << 4 | destination).
     * @param operand La chaîne d'opérande (ex: "A,B", "X,Y").
     * @return Le post-byte calculé, ou 0 si la syntaxe est invalide ou les registres inconnus.
     */
    public static int genererPostByteEXG_TFR(String operand) {
        String[] regs = operand.split(","); // Sépare les deux registres par la virgule
        if (regs.length != 2) return 0; // Doit y avoir exactement deux registres

        // Récupère les codes 4 bits pour le registre source et destination
        // getOrDefault retourne 0 si le registre n'est pas trouvé, ce qui peut masquer des erreurs.
        // Une meilleure gestion d'erreur pourrait lever une exception.
        int r1 = CODES_EXG_TFR.getOrDefault(regs[0].trim().toUpperCase(), 0);
        int r2 = CODES_EXG_TFR.getOrDefault(regs[1].trim().toUpperCase(), 0);
        
        return (r1 << 4) | r2; // Combine les deux codes en un octet
    }

    /**
     * Génère le post-byte pour les instructions PSHS/PULS/PSHU/PULU.
     * Le post-byte est un masque de bits, où chaque bit représente un registre à empiler/dépiler.
     * @param operand La chaîne d'opérande (ex: "A,B,X", "CC,PC").
     * @return Le masque de bits calculé.
     */
    public static int genererPostBytePile(String operand) {
        int masque = 0;
        String[] regs = operand.split(","); // Sépare les registres par la virgule
        
        for (String reg : regs) {
            String r = reg.trim().toUpperCase();
            // Applique le masque du registre trouvé, ou 0 si inconnu
            masque |= MASQUES_PILE.getOrDefault(r, 0); 
        }
        
        return masque;
    }

    /**
     * Retourne le code 2 bits du registre d'index pour le mode indexé.
     * @param registre Le nom du registre (X, Y, U, S).
     * @return Le code 2 bits, ou 0 si le registre est inconnu.
     */
    public static int getCodeIndexe(String registre) {
        return CODES_INDEXE.getOrDefault(registre.toUpperCase(), 0);
    }

    /**
     * Vérifie si une chaîne représente un registre d'index valide ou le PC pour l'adressage indexé.
     * Les préfixes/suffixes d'auto-incrément/décrément sont retirés avant la vérification.
     * @param registre La chaîne à vérifier (ex: "X", "Y++", "-U", "PC").
     * @return true si c'est un registre d'index valide ou PC, false sinon.
     */
    public static boolean estRegistreIndex(String registre) {
        // Supprime les caractères + et - pour n'avoir que le nom du registre
        String r = registre.toUpperCase().replaceAll("[+-]", "");
        return CODES_INDEXE.containsKey(r) || r.equals("PC");
    }
}
