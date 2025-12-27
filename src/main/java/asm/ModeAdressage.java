package asm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum ModeAdressage {
    INHERENT,    // Pas d'opérande (NOP, RTS, etc.)
    IMMEDIAT,    // #$xx ou #$xxxx
    DIRECT,      // $xx (page directe)
    ETENDU,      // $xxxx (adresse 16 bits)
    INDEXE,      // offset,R ou [offset,R]
    RELATIF,     // Branches (offset 8 ou 16 bits) - pour usage interne de l'assembleur
    REGISTRE;    // EXG, TFR, PSH, PUL (le post-byte contient les registres)

    // Pattern pour identifier un label simple ou avec offset (avant résolution par l'assembleur)
    private static final Pattern LABEL_OR_REG_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*([+-]\\$?[0-9A-Fa-f]+|[0-9]+)?$");

    /**
     * Détecte le mode d'adressage d'un opérande donné, basé sur la syntaxe.
     * Cette détection est une première estimation pour l'assembleur.
     * @param operand La chaîne de l'opérande.
     * @return Le ModeAdressage détecté.
     */
    public static ModeAdressage detecter(String operand) {
        if (operand == null || operand.isEmpty()) {
            return INHERENT;
        }
        
        operand = operand.trim();
        
        // Mode immédiat
        if (operand.startsWith("#")) {
            return IMMEDIAT;
        }
        // Mode indexé (contient une virgule ou est entre crochets)
        if (operand.contains(",") || (operand.startsWith("[") && operand.endsWith("]"))) {
            return INDEXE;
        }
        // Mode direct ($xx, adresse 8 bits)
        if (operand.matches("\\$[0-9A-Fa-f]{1,2}")) { // $00 - $FF
            return DIRECT;
        }
        // Mode étendu ($xxxx, adresse 16 bits)
        if (operand.matches("\\$[0-9A-Fa-f]{3,4}")) { // $0000 - $FFFF
            return ETENDU;
        }
        
        // Si c'est un nom de label ou de registre qui n'est pas une valeur immédiate, indexée, directe ou étendue
        // et qu'il n'est pas un nombre direct (ex: "100" sans $).
        // On le considère comme RELATIF, l'assembleur décidera s'il s'agit d'une branche ou d'une adresse directe/étendue
        // APRÈS la résolution des symboles.
        if (LABEL_OR_REG_PATTERN.matcher(operand).matches() && !ParseurOperande.estNombre(operand)) {
            return RELATIF; // Ce mode est un "catch-all" pour les symboles avant résolution finale.
        }

        // Si aucun mode spécifique n'est détecté, on retourne INHERENT.
        // Cela peut indiquer une erreur ou une instruction sans opérande explicite.
        return INHERENT;
    }
}
