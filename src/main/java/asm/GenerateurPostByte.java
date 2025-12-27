package asm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenerateurPostByte {
    
    // Pattern pour parser le mode indexé
    // Capture les préfixes (-, --), le registre de base (PC, X, Y, U, S) et les suffixes (+, ++)
    private static final Pattern PATTERN_BASE = Pattern.compile(
        "^(-{0,2})?(PC|[XYUS])(\\+{0,2})?$", 
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Classe interne pour encapsuler le résultat de la génération du post-byte.
     */
    public static class ResultatIndexe {
        public int postByte;    // L'octet du post-byte
        public int offset;      // La valeur de l'offset (peut être 0 si pas d'offset explicite)
        public int offsetSize;  // La taille de l'offset en octets (0, 1 ou 2)

        public ResultatIndexe(int pb, int off, int size) {
            this.postByte = pb;
            this.offset = off;
            this.offsetSize = size;
        }
    }

    /**
     * Génère le post-byte et l'offset associé pour un opérande en mode indexé.
     * @param operand La chaîne de l'opérande en mode indexé (ex: "10,X", "[B,Y++]").
     * @return Un objet ResultatIndexe contenant le post-byte, l'offset et sa taille, ou null si la syntaxe est invalide.
     */
    public static ResultatIndexe generer(String operand) {
        // Vérifie si c'est un adressage indirect (entre crochets)
        boolean indirect = operand.startsWith("[") && operand.endsWith("]");
        // Retire les crochets si indirect, sinon utilise l'opérande tel quel
        String contenu = indirect ? operand.substring(1, operand.length() - 1).trim() : operand.trim();

        // Si l'opérande ne contient pas de virgule, cela signifie un offset de 0
        // (ex: "X" est interprété comme "0,X")
        if (!contenu.contains(",")) {
            contenu = "0," + contenu;
        }

        // Sépare l'opérande en deux parties: l'offset et le registre de base
        // (ex: "10,X" -> "10" et "X")
        String[] parties = contenu.split(",", 2);
        String partieOffset = parties[0].trim();
        String partieBase = parties[1].trim();

        // Utilise le pattern pour analyser la partie du registre de base et les auto-incréments/décréments
        Matcher m = PATTERN_BASE.matcher(partieBase);
        if (!m.matches()) {
            return null; // Syntaxe du registre de base invalide
        }

        // Extrait les groupes du pattern
        String prefixe = m.group(1) != null ? m.group(1) : ""; // -, --
        String registre = m.group(2).toUpperCase();             // PC, X, Y, U, S
        String suffixe = m.group(3) != null ? m.group(3) : "";  // +, ++

        // Récupère le code 2 bits du registre de base (RR) pour le post-byte
        int rr = RegistreHelper.getCodeIndexe(registre);
        int postByte = 0;
        int offset = 0;
        int offsetSize = 0;

        // CAS 1: Auto-incrément/décrément (-, --, +, ++)
        if (!prefixe.isEmpty() || !suffixe.isEmpty()) {
            int modeNibble = getAutoIncDecMode(prefixe, suffixe);
            if (modeNibble >= 0) {
                // Le bit 7 (I) est à 1 pour les modes indexés complexes
                postByte = 0x80 | (rr << 5) | modeNibble;
            } else {
                return null; // Mode auto-incrément/décrément invalide
            }
        }
        // CAS 2: Offset par registre accumulateur (A, B, D comme offset)
        else if (partieOffset.matches("[ABDabd]")) {
            int accCode = getAccumulatorCode(partieOffset.toUpperCase());
            postByte = 0x80 | (rr << 5) | accCode;
        }
        // CAS 3: Offset relatif à PC (n,PC)
        else if (registre.equals("PC")) {
            offset = parseOffset(partieOffset);
            if (offset >= -128 && offset <= 127) {
                postByte = 0x8C;  // ,PCR avec offset 8 bits
                offsetSize = 1;
            } else {
                postByte = 0x8D;  // ,PCR avec offset 16 bits
                offsetSize = 2;
            }
        }
        // CAS 4: Offset numérique (n,R)
        else {
            offset = parseOffset(partieOffset);
            
            // Offset 5 bits (-16 à +15) - ce mode n'est pas utilisé avec l'adressage indirect
            if (!indirect && offset >= -16 && offset <= 15) {
                // Le bit 7 est 0 pour les offsets 5 bits
                postByte = (rr << 5) | (offset & 0x1F); // (offset & 0x1F) assure la représentation sur 5 bits
            }
            // Offset 8 bits (-128 à +127)
            else if (offset >= -128 && offset <= 127) {
                // Le bit 7 est 1 pour les modes indexés complexes avec offset
                postByte = 0x80 | (rr << 5) | 0x08; // 0x08 indique offset 8 bits
                offsetSize = 1;
            }
            // Offset 16 bits
            else {
                postByte = 0x80 | (rr << 5) | 0x09; // 0x09 indique offset 16 bits
                offsetSize = 2;
            }
        }

        // Appliquer le bit indirect (bit 4 du post-byte) si nécessaire.
        // Ce bit est applicable aux modes indexés complexes (ceux où le bit 7 est à 1).
        if (indirect && (postByte & 0x80) != 0) {
            postByte |= 0x10;
        }

        // Retourne le résultat, l'offset est masqué à 16 bits pour rester cohérent
        return new ResultatIndexe(postByte, offset & 0xFFFF, offsetSize);
    }

    /**
     * Retourne le nibble (4 bits) du mode auto-incrément/décrément pour le post-byte.
     * @param pre Le préfixe (-, --).
     * @param post Le suffixe (+, ++).
     * @return Le code du mode, ou -1 si invalide.
     */
    private static int getAutoIncDecMode(String pre, String post) {
        if (post.equals("+")) return 0x00;   // ,R+
        if (post.equals("++")) return 0x01;  // ,R++
        if (pre.equals("-")) return 0x02;    // ,-R
        if (pre.equals("--")) return 0x03;   // ,--R
        return -1; // Mode non reconnu
    }

    /**
     * Retourne le code 4 bits pour un registre accumulateur utilisé comme offset.
     * @param acc Le nom de l'accumulateur (A, B, D).
     * @return Le code correspondant.
     */
    private static int getAccumulatorCode(String acc) {
        switch (acc) {
            case "A": return 0x06; // A,R
            case "B": return 0x05; // B,R
            case "D": return 0x0B; // D,R
            default: return 0x04;  // Offset 0,R (ce cas ne devrait pas être atteint si la regex est correcte)
        }
    }

    /**
     * Parse une valeur numérique (décimale ou hexadécimale) pour l'offset.
     * @param s La chaîne de l'offset.
     * @return La valeur entière de l'offset.
     */
    private static int parseOffset(String s) {
        if (s == null || s.isEmpty()) return 0;
        s = s.trim();
        
        try {
            // Hexadécimal avec $
            if (s.startsWith("$")) {
                return Integer.parseInt(s.substring(1), 16);
            }
            // Hexadécimal négatif (ex: -$10)
            if (s.startsWith("-$")) {
                return -Integer.parseInt(s.substring(2), 16);
            }
            // Décimal par défaut
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            // En cas d'erreur de format, retourner 0 ou un message d'erreur approprié
            // Pour le générateur de post-byte, retourner 0 est une gestion simple.
            // Une vraie gestion d'erreur serait de propager l'exception.
            return 0; 
        }
    }
}
