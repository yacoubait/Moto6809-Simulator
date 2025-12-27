package mem;

import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Memoire {
    public static final int RAM_START = 0x0000;
    public static final int RAM_SIZE = 0x8000; // 32 KB
    public static final int RAM_END = RAM_START + RAM_SIZE - 1;

    public static final int ROM_START = 0x8000;
    public static final int ROM_SIZE = 0x8000; // 32 KB
    public static final int ROM_END = ROM_START + ROM_SIZE - 1;

    private final byte[] ram; 
    private final byte[] rom; 
    
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private final Set<Integer> stackMemoryUsage = new HashSet<>();

    public Memoire() {
        ram = new byte[RAM_SIZE]; 
        rom = new byte[ROM_SIZE]; 
        reset(); 
    }
    
    public void reset() {
        Arrays.fill(ram, (byte) 0); 
        Arrays.fill(rom, (byte) 0); 
        stackMemoryUsage.clear();
        // Pour un reset général, pas de old/new value spécifiques.
        pcs.firePropertyChange("memoryChange", null, null); 
    }

    public int lire(int adresse) {
        int addr = adresse & 0xFFFF; 

        if (addr >= RAM_START && addr <= RAM_END) {
            return ram[addr - RAM_START] & 0xFF;
        } else if (addr >= ROM_START && addr <= ROM_END) {
            return rom[addr - ROM_START] & 0xFF;
        } else {
            System.err.println("Lecture à une adresse mémoire non mappée: $" + String.format("%04X", addr));
            return 0; 
        }
    }
    
    /**
     * Écrit un octet à une adresse donnée.
     * @param adresse L'adresse (16 bits) où écrire.
     * @param valeur La valeur de l'octet à écrire.
     * @param isStackOperation true si cette écriture est le fait d'une opération de pile.
     * @throws RuntimeException si une tentative d'écriture a lieu sur la ROM.
     */
    public void ecrire(int adresse, int valeur, boolean isStackOperation) {
        int addr = adresse & 0xFFFF;
        byte newVal = (byte) (valeur & 0xFF);

        if (addr >= RAM_START && addr <= RAM_END) {
            int offset = addr - RAM_START;
            byte oldVal = ram[offset];
            if (oldVal != newVal) {
                ram[offset] = newVal;
                if (isStackOperation) { 
                    stackMemoryUsage.add(addr);
                }
                // CORRECTION: Passer l'adresse comme oldValue de l'événement, et la nouvelle valeur comme newValue.
                pcs.firePropertyChange("memoryChange", Integer.valueOf(addr), Integer.valueOf(newVal & 0xFF)); 
            }
        } else if (addr >= ROM_START && addr <= ROM_END) {
            throw new RuntimeException("Tentative d'écriture protégée sur la ROM à l'adresse $" + String.format("%04X", addr));
        } else {
            System.err.println("Écriture à une adresse mémoire non mappée: $" + String.format("%04X", addr));
        }
    }

    // Surcharge de ecrire pour les opérations non-pile
    public void ecrire(int adresse, int valeur) {
        ecrire(adresse, valeur, false); 
    }

    public void ecrireToRom(int adresse, int valeur) {
        int addr = adresse & 0xFFFF;
        byte newVal = (byte) (valeur & 0xFF);

        if (addr >= ROM_START && addr <= ROM_END) {
            int offset = addr - ROM_START;
            byte oldVal = rom[offset];
            if (oldVal != newVal) {
                rom[offset] = newVal;
                // CORRECTION: Passer l'adresse comme oldValue de l'événement, et la nouvelle valeur comme newValue.
                pcs.firePropertyChange("memoryChange", Integer.valueOf(addr), Integer.valueOf(newVal & 0xFF)); 
            }
        } else {
            System.err.println("Assembleur: Tentative d'écriture dans la ROM à une adresse hors plage ROM: $" + String.format("%04X", addr));
        }
    }
    
    public int lireMot(int adresse) {
        int haut = lire(adresse);
        int bas = lire((adresse + 1) & 0xFFFF); 
        return (haut << 8) | bas; 
    }
    
    /**
     * Écrit un mot (16 bits) à partir d'une adresse donnée (Big-Endian).
     * @param adresse L'adresse de début où écrire le high-byte.
     * @param valeur La valeur du mot à écrire.
     * @param isStackOperation true si cette écriture est le fait d'une opération de pile.
     * @throws RuntimeException si une tentative d'écriture a lieu sur la ROM.
     */
    public void ecrireMot(int adresse, int valeur, boolean isStackOperation) {
        // Ces appels à ecrire() passeront l'indicateur isStackOperation
        ecrire(adresse, (valeur >> 8) & 0xFF, isStackOperation);   
        ecrire((adresse + 1) & 0xFFFF, valeur & 0xFF, isStackOperation); 
    }

    // Surcharge de ecrireMot pour les opérations non-pile
    public void ecrireMot(int adresse, int valeur) {
        ecrireMot(adresse, valeur, false);
    }

    public void ecrireMotToRom(int adresse, int valeur) {
        ecrireToRom(adresse, (valeur >> 8) & 0xFF); 
        ecrireToRom((adresse + 1) & 0xFFFF, valeur & 0xFF); 
    }

    public int concat(int ad1, int ad2) {
        return ((ad1 & 0xFF) << 8) | (ad2 & 0xFF);
    }

    public Set<Integer> getStackMemoryUsage() {
        return new HashSet<>(stackMemoryUsage); 
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }
}
