package execution;

import gui.Moto6809;

import javax.swing.SwingUtilities;

/**
 * Point d'entrée principal de l'application. Lance l'interface graphique du
 * simulateur Moto6809.
 */
public class Execution {
	public static void main(String[] args) {
		// Lance l'interface graphique sur l'Event Dispatch Thread (EDT) pour garantir
		// la thread safety de Swing.
		SwingUtilities.invokeLater(() -> {
			Moto6809 app = new Moto6809();
			app.setVisible(true);
			// Si des arguments de ligne de commande sont passés (ex: un chemin de fichier),
			// vous pourriez les gérer ici pour ouvrir un fichier source au démarrage.
			// Exemple : if (args.length > 0) app.openFile(new java.io.File(args[0]));
		});
	}
}
