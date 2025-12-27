package cpu;

import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import mem.Memoire; 

public class CPU6809 {
    // Registres de 8 bits
    private int A;
    private int B;
    private int DP;  // Direct Page Register
    private int CC;  // Condition Codes Register

    // Registres 16 bits
    private int X;   // Index Register X
    private int Y;   // Index Register Y
    private int U;   // User Stack Pointer
    private int S;   // Hardware Stack Pointer
    private int PC;  // Program Counter

    private Memoire mem; 

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public static final int FLAG_CARRY      = 0x01; 
    public static final int FLAG_OVERFLOW   = 0x02; 
    public static final int FLAG_ZERO       = 0x04; 
    public static final int FLAG_NEGATIVE   = 0x08; 
    public static final int FLAG_IRQ_MASK   = 0x10; 
    public static final int FLAG_HALF_CARRY = 0x20; 
    public static final int FLAG_FIRQ_MASK  = 0x40; 
    public static final int FLAG_ENTIRE_FLAG= 0x80; 


    public CPU6809() {
        this.mem = null; 
        reset(); 
    }

    public void setMemoire(Memoire mem) {
        this.mem = mem;
        reset(); 
    }

    public void reset() {
        A = 0; B = 0; DP = 0; CC = 0;
        X = 0; Y = 0; 
        U = Memoire.RAM_END - 0x100; // <<< MODIFICATION ICI: U commence 256 octets plus bas que S
        S = Memoire.RAM_END;         // S commence à la toute fin de la RAM
        PC = 0; 

        if (mem != null) {
            PC = mem.lireMot(0xFFFE); 
        } else {
            PC = 0x0000; 
        }

        fireStateChanged(); 
        System.out.println("CPU 6809 réinitialisé. PC = $" + String.format("%04X", PC));
    }

    // ... Reste du code inchangé ... (Accesseurs/Modificateurs, Flags, PropertyChangeSupport)
    
    public int getA() { return A; }
    public void setA(int a) {
        int old = this.A;
        this.A = a & 0xFF; 
        pcs.firePropertyChange("A", old, this.A);
        fireStateChanged(); 
    }

    public int getB() { return B; }
    public void setB(int b) {
        int old = this.B;
        this.B = b & 0xFF; 
        pcs.firePropertyChange("B", old, this.B);
        fireStateChanged(); 
    }

    public int getDP() { return DP; }
    public void setDP(int dP) {
        int old = this.DP;
        this.DP = dP & 0xFF; 
        pcs.firePropertyChange("DP", old, this.DP);
        fireStateChanged(); 
    }

    public int getCC() { return CC; }
    public void setCC(int cC) {
        int old = this.CC;
        this.CC = cC & 0xFF; 
        pcs.firePropertyChange("CC", old, this.CC);
        fireStateChanged(); 
    }

    public int getX() { return X; }
    public void setX(int x) {
        int old = this.X;
        this.X = x & 0xFFFF; 
        pcs.firePropertyChange("X", old, this.X);
        fireStateChanged(); 
    }

    public int getY() { return Y; }
    public void setY(int y) {
        int old = this.Y;
        this.Y = y & 0xFFFF; 
        pcs.firePropertyChange("Y", old, this.Y);
        fireStateChanged(); 
    }

    public int getU() { return U; }
    public void setU(int u) {
        int old = this.U;
        this.U = u & 0xFFFF; 
        pcs.firePropertyChange("U", old, this.U); 
        fireStateChanged(); 
    }

    public int getS() { return S; }
    public void setS(int s) {
        int old = this.S;
        this.S = s & 0xFFFF; 
        pcs.firePropertyChange("S", old, this.S); 
        fireStateChanged(); 
    }

    public int getPC() { return PC; }
    public void setPC(int pC) {
        int old = this.PC;
        this.PC = pC & 0xFFFF; 
        pcs.firePropertyChange("PC", old, this.PC);
        fireStateChanged(); 
    }
    
    public void incrementPC() { setPC((PC + 1) & 0xFFFF); } 
    public void incrementPC(int value) { setPC((PC + value) & 0xFFFF); }
    public void decrementPC() { setPC((PC - 1) & 0xFFFF); }
    public void decrementPC(int value) { setPC((PC - value) & 0xFFFF); }


    public int getD() {
        return (A << 8) | B;
    }

    public void setD(int d) {
        setA((d >> 8) & 0xFF); 
        setB(d & 0xFF);       
    }

    public boolean getFlag(int flagMask) {
        return (CC & flagMask) != 0;
    }

    public void setFlag(int flagMask, boolean value) {
        int oldCC = this.CC;
        if (value) {
            this.CC |= flagMask;
        } else {
            this.CC &= ~flagMask;
        }
        if (oldCC != this.CC) {
            pcs.firePropertyChange("CC", oldCC, this.CC);
            fireStateChanged();
        }
    }

    public void setCarry(boolean value) { setFlag(FLAG_CARRY, value); }
    public void setOverflow(boolean value) { setFlag(FLAG_OVERFLOW, value); }
    public void setZero(boolean value) { setFlag(FLAG_ZERO, value); }
    public void setNegative(boolean value) { setFlag(FLAG_NEGATIVE, value); }
    public void setIRQMask(boolean value) { setFlag(FLAG_IRQ_MASK, value); }
    public void setHalfCarry(boolean value) { setFlag(FLAG_HALF_CARRY, value); }
    public void setFIRQMask(boolean value) { setFlag(FLAG_FIRQ_MASK, value); }
    public void setEntireFlag(boolean value) { setFlag(FLAG_ENTIRE_FLAG, value); }


    private void fireStateChanged() {
        pcs.firePropertyChange("cpuState", null, this);
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener l) {
        pcs.addPropertyChangeListener(propertyName, l);
    }
    
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener l) {
        pcs.removePropertyChangeListener(propertyName, l);
    }


    @Override
    public String toString() {
        return String.format(
            "PC=$%04X S=$%04X U=$%04X Y=$%04X X=$%04X A=$%02X B=$%02X D=$%04X DP=$%02X CC=$%02X [%s]",
            PC, S, U, Y, X, A, B, getD(), DP, CC, getFlagsString()
        );
    }

    public String getFlagsString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getFlag(FLAG_ENTIRE_FLAG)    ? 'E' : '-');
        sb.append(getFlag(FLAG_FIRQ_MASK)     ? 'F' : '-');
        sb.append(getFlag(FLAG_HALF_CARRY)    ? 'H' : '-');
        sb.append(getFlag(FLAG_IRQ_MASK)      ? 'I' : '-');
        sb.append(getFlag(FLAG_NEGATIVE)      ? 'N' : '-');
        sb.append(getFlag(FLAG_ZERO)          ? 'Z' : '-');
        sb.append(getFlag(FLAG_OVERFLOW)      ? 'V' : '-');
        sb.append(getFlag(FLAG_CARRY)         ? 'C' : '-');
        return sb.toString();
    }
}
