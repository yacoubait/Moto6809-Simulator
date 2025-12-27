package asm;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Table de symboles pour gérer les labels et les constantes EQU.
 */
public class TableSymboles {
    
    private final Map<String, Integer> symboles = new HashMap<>(); // Pour les constantes EQU
    private final Map<String, Integer> labels = new HashMap<>();   // Pour les labels d'adresses
    
    /**
     * Définir un symbole (EQU).
     * @param nom Le nom du symbole.
     * @param valeur La valeur associée au symbole.
     * @throws IllegalArgumentException si le nom du symbole est vide.
     */
    public void definirSymbole(String nom, int valeur) {
        String key = nom.toUpperCase().trim();
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Nom de symbole vide");
        }
        symboles.put(key, valeur);
    }
    
    /**
     * Définir un label avec son adresse.
     * @param nom Le nom du label.
     * @param adresse L'adresse associée au label.
     * @throws IllegalArgumentException si le nom du label est vide.
     */
    public void definirLabel(String nom, int adresse) {
        String key = nom.toUpperCase().trim().replace(":", ""); // Supprimer le ':' si présent
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Nom de label vide");
        }
        labels.put(key, adresse);
    }
    
    /**
     * Obtenir la valeur d'un symbole ou label.
     * Cherche d'abord dans les symboles (EQU), puis dans les labels.
     * @param nom Le nom du symbole ou label à obtenir.
     * @return La valeur associée, ou null si non trouvé.
     */
    public Integer obtenir(String nom) {
        if (nom == null) return null;
        String key = nom.toUpperCase().trim().replace(":", "");
        
        // Chercher d'abord dans les symboles (EQU)
        if (symboles.containsKey(key)) {
            return symboles.get(key);
        }
        
        // Puis dans les labels
        return labels.get(key);
    }
    
    /**
     * Vérifier si un symbole ou label existe.
     * @param nom Le nom à vérifier.
     * @return true si le nom existe en tant que symbole ou label, false sinon.
     */
    public boolean existe(String nom) {
        if (nom == null) return false;
        String key = nom.toUpperCase().trim().replace(":", "");
        return symboles.containsKey(key) || labels.containsKey(key);
    }
    
    /**
     * Obtenir tous les noms de symboles (EQU).
     * @return Un Set non modifiable des noms de symboles.
     */
    public Set<String> getSymboles() {
        return Collections.unmodifiableSet(symboles.keySet());
    }
    
    /**
     * Obtenir tous les noms de labels.
     * @return Un Set non modifiable des noms de labels.
     */
    public Set<String> getLabels() {
        return Collections.unmodifiableSet(labels.keySet());
    }
    
    /**
     * Effacer tous les symboles et labels de la table.
     */
    public void effacer() {
        symboles.clear();
        labels.clear();
    }
    
    /**
     * Résoudre une référence qui peut être un nombre (hex, dec, bin) ou un symbole/label.
     * @param reference La chaîne de référence.
     * @return La valeur numérique résolue, ou null si la référence ne peut pas être résolue.
     */
    public Integer resoudre(String reference) {
        if (reference == null) return null;
        
        // Si c'est un nombre, le parser directement
        Integer valeur = ParseurOperande.tryParseNumber(reference);
        if (valeur != null) {
            return valeur;
        }
        
        // Sinon, chercher dans les symboles/labels
        return obtenir(reference);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Table de Symboles ===\n");
        
        if (!symboles.isEmpty()) {
            sb.append("Symboles (EQU):\n");
            symboles.forEach((k, v) -> 
                sb.append(String.format("  %-12s = $%04X (%d)\n", k, v, v))
            );
        }
        
        if (!labels.isEmpty()) {
            sb.append("Labels:\n");
            labels.forEach((k, v) -> 
                sb.append(String.format("  %-12s = $%04X (%d)\n", k, v, v))
            );
        }
        
        if (symboles.isEmpty() && labels.isEmpty()) {
            sb.append("  (vide)\n");
        }
        
        return sb.toString();
    }
}
