package asm;

public class ParseurOperande {

    /**
     * Parse une valeur numérique (décimale, hexadécimale, binaire)
     * et gère les préfixes comme #, $, 0x, % et suffixes h.
     * @param s La chaîne à parser.
     * @return La valeur entière, ou 0 si la chaîne est vide ou null.
     */
    public static int parseValeur(String s) {
        if (s == null || s.isEmpty()) return 0;
        s = s.trim();

        // Enlever le # si présent pour les immédiats
        if (s.startsWith("#")) {
            s = s.substring(1).trim();
        }

        Integer val = tryParseNumber(s);
        return val != null ? val : 0;
    }

    /**
     * Tente de parser un nombre (décimal, hexadécimal, binaire) à partir d'une chaîne.
     * Gère les préfixes $, 0x, % et suffixes h.
     * Retourne null si la chaîne n'est pas un nombre valide.
     * @param s La chaîne à parser.
     * @return L'entier parsé, ou null si la conversion échoue.
     */
    public static Integer tryParseNumber(String s) {
        if (s == null || s.isEmpty()) return null;
        s = s.trim();

        // Enlever le # si présent (pour les immédiats)
        if (s.startsWith("#")) {
            s = s.substring(1).trim();
        }

        try {
            // Hexadécimal avec $
            if (s.startsWith("$")) {
                return Integer.parseInt(s.substring(1), 16);
            }
            
            // Hexadécimal négatif
            if (s.startsWith("-$")) {
                return -Integer.parseInt(s.substring(2), 16);
            }
            
            // Hexadécimal avec 0x ou 0X
            if (s.toLowerCase().startsWith("0x")) {
                return Integer.parseInt(s.substring(2), 16);
            }
            
            // Hexadécimal avec suffixe h ou H
            if (s.toLowerCase().endsWith("h")) {
                return Integer.parseInt(s.substring(0, s.length() - 1), 16);
            }
            
            // Binaire avec préfixe % ou 0b
            if (s.startsWith("%")) {
                return Integer.parseInt(s.substring(1), 2);
            }
            if (s.toLowerCase().startsWith("0b")) {
                return Integer.parseInt(s.substring(2), 2);
            }
            
            // Si contient des lettres A-F (et pas de préfixe connu), c'est probablement de l'hexa
            // Attention: cela peut être ambigu si un label non résolu contient de telles lettres.
            // Le parseur d'assembleur doit d'abord résoudre les symboles.
            if (s.matches(".*[A-Fa-f].*") && s.length() <= 4) { // Heuristique : Max 4 chiffres hex
                return Integer.parseInt(s, 16);
            }
            
            // Décimal par défaut
            return Integer.parseInt(s);
            
        } catch (NumberFormatException e) {
            return null; // Retourne null si la conversion échoue
        }
    }

    /**
     * Extrait l'adresse d'un opérande en mode direct/étendu.
     * @param operand L'opérande.
     * @return L'adresse parsée.
     */
    public static int parseAdresse(String operand) {
        if (operand == null || operand.isEmpty()) return 0;
        
        operand = operand.trim();
        
        if (operand.startsWith("$")) {
            return Integer.parseInt(operand.substring(1), 16);
        }
        
        return parseValeur(operand);
    }

    /**
     * Vérifie si une chaîne est un nombre valide (hex, dec, bin).
     * @param s La chaîne à vérifier.
     * @return true si la chaîne représente un nombre valide, false sinon.
     */
    public static boolean estNombre(String s) {
        return tryParseNumber(s) != null;
    }
    
    /**
     * Extrait la partie numérique d'une expression de label avec offset.
     * Exemple: "LOOP+5" -> 5, "VAR-2" -> -2.
     * @param s L'expression (ex: "LABEL+OFFSET").
     * @return L'offset numérique, ou 0 si pas d'offset.
     */
    public static int extraireOffset(String s) {
        if (s == null) return 0;
        
        // Chercher le dernier '+' ou '-' qui n'est pas un préfixe de nombre négatif
        int idxPlus = s.lastIndexOf('+');
        int idxMinus = s.lastIndexOf('-');

        // Trouver l'index du séparateur d'offset valide
        int offsetSeparatorIndex = -1;
        if (idxPlus > 0) { // Ne pas considérer le '+' initial comme un séparateur d'offset
            offsetSeparatorIndex = idxPlus;
        }
        if (idxMinus > 0 && idxMinus > offsetSeparatorIndex) { // Ne pas considérer '-' initial comme séparateur
            offsetSeparatorIndex = idxMinus;
        }
        
        if (offsetSeparatorIndex > 0) {
            String offsetStr = s.substring(offsetSeparatorIndex);
            Integer val = tryParseNumber(offsetStr);
            return val != null ? val : 0;
        }
        
        return 0;
    }
    
    /**
     * Extrait le nom du label d'une expression avec ou sans offset.
     * Exemple: "LOOP+5" -> "LOOP", "#LABEL" -> "LABEL".
     * @param s L'expression.
     * @return Le nom du label, ou une chaîne vide si pas de label.
     */
    public static String extraireLabel(String s) {
        if (s == null || s.isEmpty()) return "";
        
        s = s.trim();
        
        // Supprimer le préfixe immédiat '#' si présent
        if (s.startsWith("#")) {
            s = s.substring(1).trim();
        }

        // Si c'est un nombre après avoir retiré le #, ce n'est pas un label
        if (estNombre(s)) return "";
        
        // Chercher le dernier '+' ou '-' pour trouver la fin du label
        int idxPlus = s.lastIndexOf('+');
        int idxMinus = s.lastIndexOf('-');
        
        int offsetSeparatorIndex = -1;
        if (idxPlus > 0) {
            offsetSeparatorIndex = idxPlus;
        }
        if (idxMinus > 0 && idxMinus > offsetSeparatorIndex) {
            offsetSeparatorIndex = idxMinus;
        }

        if (offsetSeparatorIndex > 0) {
            return s.substring(0, offsetSeparatorIndex).trim();
        }
        
        return s; // Si pas d'offset, toute la chaîne est le label
    }
}
