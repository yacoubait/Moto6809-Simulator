package gui.panels;

import gui.theme.*;
import gui.components.ComponentFactory;
import gui.components.StyledTextField;
import cpu.CPU6809;
import sim.SimulatorEngine; // Import nécessaire

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

// Le type générique <cpu> a été supprimé comme suggéré précédemment
public class CPUPanel extends JPanel implements PropertyChangeListener {

	private final Map<String, StyledTextField> registres = new LinkedHashMap<>();
	private final FlagsPanel flagsPanel;

	private CPU6809 cpu;
	private SimulatorEngine simulatorEngine; // Nouvelle référence

	public CPUPanel() {
		setLayout(new GridBagLayout());
		setBackground(Theme.FOND);
		setBorder(new RoundedBorder(Theme.BORDURE, 1, 20, new Insets(10, 10, 10, 10)));

		flagsPanel = new FlagsPanel();

		buildLayout();
	}

	/**
	 * Définit l'instance du CPU à observer et s'abonne à ses PropertyChangeEvents.
	 * 
	 * @param cpu L'instance de CPU6809.
	 */
	public void setCPU(CPU6809 cpu) {
		if (this.cpu != null) {
			this.cpu.removePropertyChangeListener(this);
			this.cpu.removePropertyChangeListener(flagsPanel);
		}
		this.cpu = cpu;
		if (this.cpu != null) {
			this.cpu.addPropertyChangeListener(this);
			// Le PC est déjà écouté via l'abonnement général 'this.cpu.addPropertyChangeListener(this);'
			flagsPanel.setCPU(cpu);
			updateAllRegisters(); // Mise à jour initiale des registres
		}
	}

	/**
	 * Définit l'instance du SimulatorEngine pour accéder aux informations du programme.
	 * @param simulatorEngine L'instance de SimulatorEngine.
	 */
	public void setSimulatorEngine(SimulatorEngine simulatorEngine) { // <-- CETTE MÉTHODE MANQUAIT OU ÉTAIT MAL PLACÉE
        this.simulatorEngine = simulatorEngine;
    }

	public FlagsPanel getFlagsPanel() {
		return flagsPanel;
	}

	private void buildLayout() {
		GridBagConstraints gbc = new GridBagConstraints();
		int ecartV = 5;

		// === COLONNE GAUCHE (Registres A, U, X, DP) ===
		gbc.gridx = 0;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.insets = new Insets(ecartV, 10, ecartV, 0);

		addRegisterField("A", gbc, 0, 4);
		addRegisterField("U", gbc, 1, 8);
		addRegisterField("X", gbc, 2, 8);
		addRegisterField("DP", gbc, 3, 4);

		// === BUS GAUCHE (Horizontal) ===
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(20, 0, 0, 0);
		add(ComponentFactory.createHorizontalBus(), gbc);

		// === CPU CENTRAL (Représentation graphique) ===
		gbc.gridx = 2;
		gbc.gridy = 0;
		gbc.gridheight = 3;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(ecartV, 0, -10, 0);

		JPanel cpuWrapper = new JPanel(new BorderLayout(0, 0));
		cpuWrapper.setOpaque(false);
		cpuWrapper.add(new JLabel(" "), BorderLayout.NORTH);
		cpuWrapper.add(createCPURectangle(), BorderLayout.CENTER);
		add(cpuWrapper, gbc);

		// === ZONE LIAISON BAS (Bus vertical et champ 'Résultat') ===
		gbc.gridy = 3;
		gbc.gridheight = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(-10, 0, -30, 0);
		add(createZoneLiaisonBas(), gbc);

		// === BUS DROIT (Horizontal) ===
		gbc.gridx = 3;
		gbc.gridy = 0;
		gbc.gridheight = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(20, -1, 0, 0);
		add(ComponentFactory.createHorizontalBus(), gbc);

		// === COLONNE DROITE (Registres B, S, Y, PC) ===
		gbc.gridx = 4;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(ecartV, 0, ecartV, 10);

		addRegisterField("B", gbc, 0, 4);
		addRegisterField("S", gbc, 1, 8);
		addRegisterField("Y", gbc, 2, 8);
		addRegisterField("PC", gbc, 3, 8);

		// === FLAGS CCR (Panneau des flags) ===
		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.gridwidth = 5;
		gbc.insets = new Insets(30, 0, 0, 0);
		gbc.anchor = GridBagConstraints.CENTER;
		add(flagsPanel, gbc);
	}

	private void addRegisterField(String name, GridBagConstraints gbc, int row, int charLength) {
		gbc.gridy = row;
		StyledTextField field = new StyledTextField(charLength);
		field.setEditable(false);
		field.setHorizontalAlignment(JTextField.CENTER);
		field.setText(String.format("%0" + (charLength / 2) + "X", 0)); 

		registres.put(name, field);
		add(createRegisterPanel(name, field), gbc);
	}

	private JPanel createRegisterPanel(String name, JTextField field) {
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setOpaque(false);

		JLabel label = ComponentFactory.createLabel(name);
		label.setHorizontalAlignment(JLabel.CENTER);

		panel.add(label, BorderLayout.NORTH);
		panel.add(field, BorderLayout.CENTER);

		return panel;
	}

	private JPanel createCPURectangle() {
		return new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				Graphics2D g2d = (Graphics2D) g;
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				int w = getWidth();
				int h = getHeight();

				GradientPaint gradient = new GradientPaint(0, 0, Theme.CPU_GRADIENT_TOP, 0, h,
						Theme.CPU_GRADIENT_BOTTOM);
				g2d.setPaint(gradient);
				g2d.fillRoundRect(0, 0, w - 1, h - 1, 20, 20);

				g2d.setColor(Theme.CPU_BORDER);
				g2d.setStroke(new BasicStroke(1));
				g2d.drawRoundRect(2, 2, w - 5, h - 5, 20, 20);

				g2d.setColor(Color.WHITE);
				g2d.setFont(Theme.FONT_CPU);
				FontMetrics fm = g2d.getFontMetrics();
				String text = "UAL";
				int x = (w - fm.stringWidth(text)) / 2;
				int y = (h + fm.getAscent() - fm.getDescent()) / 2;
				g2d.drawString(text, x, y - 10);

				g2d.setFont(Theme.fontUI(Font.PLAIN, 12));
				fm = g2d.getFontMetrics();
				String subText = "Motorola 6809";
				x = (w - fm.stringWidth(subText)) / 2;
				g2d.drawString(subText, x, y + 15);
			}

			@Override
			public Dimension getPreferredSize() {
				return new Dimension(Math.round(200 * Theme.UI_SCALE), 0);
			}
		};
	}

	private JPanel createZoneLiaisonBas() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setOpaque(false);

		GridBagConstraints gbc = new GridBagConstraints();

		// Bus gauche (vertical)
		gbc.gridx = 0;
		gbc.fill = GridBagConstraints.VERTICAL;
		gbc.weighty = 1.0;
		gbc.insets = new Insets(0, 30, 0, 0);
		panel.add(ComponentFactory.createVerticalBus(60), gbc);

		// Registre résultat (RES) - Maintenant pour afficher l'opérande
		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 1.0;
		gbc.weighty = 0;
		gbc.insets = new Insets(0, 0, 0, 0);
		StyledTextField regRes = new StyledTextField(10); 
		regRes.setEditable(false);
		regRes.setHorizontalAlignment(JTextField.CENTER);
		regRes.setText("----");
		registres.put("RES", regRes);
		// Le label est maintenant un espace vide
		JPanel resPanel = new JPanel(new BorderLayout(5, 5));
		resPanel.setOpaque(false);
		resPanel.add(ComponentFactory.createLabel(" "), BorderLayout.NORTH); 
		resPanel.add(regRes, BorderLayout.CENTER);
		panel.add(resPanel, gbc);

		// Bus droit (vertical)
		gbc.gridx = 2;
		gbc.fill = GridBagConstraints.VERTICAL;
		gbc.weightx = 0;
		gbc.weighty = 1.0;
		gbc.insets = new Insets(0, 0, 0, 30);
		panel.add(ComponentFactory.createVerticalBus(60), gbc);

		return panel;
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g.create();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setColor(getBackground());
		g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
		g2d.dispose();
		super.paintComponent(g);
	}

	/**
	 * Définit la valeur d'un registre spécifique.
	 * 
	 * @param name  Le nom du registre (ex: "A", "PC").
	 * @param value La valeur du registre sous forme de chaîne hexadécimale.
	 */
	public void setRegister(String name, String value) {
		StyledTextField field = registres.get(name);
		if (field != null) {
			field.setText(value);
		}
	}

	/**
	 * Retourne la valeur d'un registre spécifique.
	 * 
	 * @param name Le nom du registre.
	 * @return La valeur du registre sous forme de chaîne.
	 */
	public String getRegister(String name) {
		StyledTextField field = registres.get(name);
		return field != null ? field.getText() : "";
	}

	/**
	 * Met à jour tous les registres affichés en utilisant l'état actuel du CPU.
	 * Cette méthode est appelée par le PropertyChangeListener.
	 */
	private void updateAllRegisters() {
		if (cpu == null)
			return;

		setRegister("A", String.format("%02X", cpu.getA()));
		setRegister("B", String.format("%02X", cpu.getB()));
		setRegister("U", String.format("%04X", cpu.getU()));
		setRegister("X", String.format("%04X", cpu.getX()));
		setRegister("Y", String.format("%04X", cpu.getY()));
		setRegister("S", String.format("%04X", cpu.getS()));
		setRegister("DP", String.format("%02X", cpu.getDP()));
		setRegister("PC", String.format("%04X", cpu.getPC()));

		flagsPanel.setAllFlags(cpu.getCC());
		
		// Mise à jour initiale de l'opérande
		if (simulatorEngine != null && cpu != null) {
			String operand = simulatorEngine.getOperandForAddress(cpu.getPC());
			setRegister("RES", operand.isEmpty() ? "----" : operand);
		} else {
			setRegister("RES", "----");
		}
	}

	/**
	 * Réinitialise tous les champs de registre à leur valeur par défaut (souvent
	 * "00" ou "0000").
	 */
	public void resetAll() {
		registres.values().forEach(f -> f.setText("0000"));
		registres.get("A").setText("00");
		registres.get("B").setText("00");
		registres.get("DP").setText("00");
		registres.get("RES").setText("----"); // Réinitialiser le champ Opérande

		flagsPanel.resetAll();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		SwingUtilities.invokeLater(() -> {
			if (evt.getPropertyName().equals("cpuState")) {
				updateAllRegisters();
			} else if (evt.getPropertyName().equals("A")) {
				setRegister("A", String.format("%02X", (Integer) evt.getNewValue()));
			} else if (evt.getPropertyName().equals("B")) {
				setRegister("B", String.format("%02X", (Integer) evt.getNewValue()));
			} else if (evt.getPropertyName().equals("DP")) {
				setRegister("DP", String.format("%02X", (Integer) evt.getNewValue()));
			} else if (evt.getPropertyName().equals("X")) {
				setRegister("X", String.format("%04X", (Integer) evt.getNewValue()));
			} else if (evt.getPropertyName().equals("Y")) {
				setRegister("Y", String.format("%04X", (Integer) evt.getNewValue()));
			} else if (evt.getPropertyName().equals("U")) {
				setRegister("U", String.format("%04X", (Integer) evt.getNewValue()));
			} else if (evt.getPropertyName().equals("S")) {
				setRegister("S", String.format("%04X", (Integer) evt.getNewValue()));
			} else if (evt.getPropertyName().equals("PC")) {
				setRegister("PC", String.format("%04X", (Integer) evt.getNewValue()));
				// Mettre à jour l'opérande quand le PC change
				if (simulatorEngine != null && cpu != null) {
					String operand = simulatorEngine.getOperandForAddress((Integer) evt.getNewValue());
					setRegister("RES", operand.isEmpty() ? "----" : operand);
				} else {
					setRegister("RES", "----"); // Réinitialiser si pas de moteur ou CPU
				}
			}
		});
	}
}
