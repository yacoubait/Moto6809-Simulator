package gui.theme;

import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class Theme {

    private Theme() {}

    public static float UI_SCALE = 1.15f;
    public static float UI_SCALE_OLD = 1.15f;

    public static Color FOND = new Color(240, 242, 245);
    public static Color FOND_CLAIR = new Color(250, 252, 255);
    public static Color FOND_HEADER = new Color(240, 242, 245);
    public static Color REGISTRE = new Color(255, 255, 255);
    public static Color TEXTE = new Color(40, 44, 52);
    public static Color BORDURE = new Color(190, 200, 210);
    public static Color HIGHLIGHT = new Color(7, 160, 215);

    public static Color BTN_PRIMAIRE = new Color(46, 204, 113);
    public static Color BTN_DANGER = new Color(231, 76, 60);
    public static Color BTN_INFO = new Color(52, 152, 219);
    public static Color BTN_NEUTRE = new Color(120, 130, 140);
    public static Color BTN_TOOLBAR = new Color(120, 130, 140);

    public static Color MENU_BAR = new Color(115, 194, 251);
    public static Color TOOL_BAR = new Color(240, 255, 255); // <<< modifiable via properties

    public static Color SYNTAX_MNEMONIC = new Color(0, 102, 204);
    public static Color SYNTAX_OPERAND = new Color(180, 80, 0);
    public static Color SYNTAX_COMMENT = new Color(120, 130, 140);
    public static Color SYNTAX_LABEL = new Color(120, 0, 160);
    public static Color SYNTAX_DIRECTIVE = new Color(128, 0, 0);
    public static Color SYNTAX_NUMBER = new Color(0, 150, 0);

    public static Color CPU_GRADIENT_TOP = new Color(240, 255, 255);
    public static Color CPU_GRADIENT_BOTTOM = new Color(0, 0, 0);
    public static Color CPU_BORDER = new Color(110, 120, 130);
    public static Color BUS_COLOR = new Color(100, 110, 120);

    public static int ESPACEMENT = 20;
    public static int EPAISSEUR_BUS = 3;
    public static int RAYON_ARRONDI = 15;
    public static int RAYON_ARRONDI_GRAND = 20;

    public static String FONT_UI_NAME = "Segoe UI";
    public static String FONT_MONO_NAME = "Consolas";

    public static Font FONT_TITRE;
    public static Font FONT_LABEL;
    public static Font FONT_NORMAL;
    public static Font FONT_REGISTRE;
    public static Font FONT_CODE;
    public static Font FONT_CPU;

    public static Insets INSETS_PETIT;
    public static Insets INSETS_NORMAL;
    public static Insets INSETS_GRAND;
    public static Insets INSETS_DIALOG;

    /**
     * Nom du fichier de configuration embarqué (doit être placé dans src/main/resources)
     */
    public static final String THEME_CONFIG_FILE = "theme.properties";

    static {
        loadDefaultFontsAndInsets();
    }

    public static void loadDefaultFontsAndInsets() {
        FONT_TITRE = fontUI(Font.BOLD, 15);
        FONT_LABEL = fontUI(Font.BOLD, 12);
        FONT_NORMAL = fontUI(Font.PLAIN, 14);
        FONT_REGISTRE = fontMono(Font.BOLD, 14);
        FONT_CODE = fontMono(Font.PLAIN, 14);
        FONT_CPU = fontUI(Font.BOLD, 36);

        INSETS_PETIT = new Insets(Math.round(4 * UI_SCALE), Math.round(6 * UI_SCALE), Math.round(4 * UI_SCALE), Math.round(6 * UI_SCALE));
        INSETS_NORMAL = new Insets(Math.round(6 * UI_SCALE), Math.round(10 * UI_SCALE), Math.round(6 * UI_SCALE), Math.round(10 * UI_SCALE));
        INSETS_GRAND = new Insets(Math.round(10 * UI_SCALE), Math.round(15 * UI_SCALE), Math.round(10 * UI_SCALE), Math.round(15 * UI_SCALE));
        INSETS_DIALOG = new Insets(Math.round(15 * UI_SCALE), Math.round(15 * UI_SCALE), Math.round(15 * UI_SCALE), Math.round(15 * UI_SCALE));
    }

    public static Font font(String name, int style, int size) {
        return new Font(name, style, Math.round(size * UI_SCALE));
    }

    public static Font fontUI(int style, int size) {
        return new FontUIResource(FONT_UI_NAME, style, Math.round(size * UI_SCALE));
    }

    public static Font fontMono(int style, int size) {
        Font font = new FontUIResource(FONT_MONO_NAME, style, Math.round(size * UI_SCALE));
        if (font.getFamily().equals("Dialog") && !FONT_MONO_NAME.equalsIgnoreCase("Monospaced")) {
            font = new FontUIResource("Monospaced", style, Math.round(size * UI_SCALE));
        }
        return font;
    }

    public static Color hover(Color base) {
        return base.brighter();
    }

    public static Color pressed(Color base) {
        return base.darker();
    }

    public static Color parseColor(String hex, Color defaultColor) {
        if (hex == null || hex.trim().isEmpty()) {
            return defaultColor;
        }
        try {
            return Color.decode(hex);
        } catch (NumberFormatException e) {
            System.err.println("Erreur de format de couleur: " + hex + ". Utilisation de la couleur par défaut.");
            return defaultColor;
        }
    }

    public static String toHexString(Color color) {
        return "#" + Integer.toHexString(color.getRGB() & 0xFFFFFF | 0x1000000).substring(1).toUpperCase();
    }

    /**
     * Retourne le chemin du fichier de configuration utilisateur (sous le dossier home).
     */
    private static Path getUserConfigPath() {
        return Paths.get(System.getProperty("user.home"), ".moto6908", THEME_CONFIG_FILE);
    }

    /**
     * Charge les propriétés du thème.
     * Priorité : 1) fichier utilisateur (~/.moto6908/theme.properties) s'il existe,
     *            2) fichier embarqué dans le JAR (src/main/resources/theme.properties).
     */
    public static void loadThemeProperties() {
        Properties props = new Properties();

        // 1) Essayer fichier utilisateur
        Path userConfig = getUserConfigPath();
        if (Files.exists(userConfig)) {
            try (InputStream in = Files.newInputStream(userConfig)) {
                props.load(in);
            } catch (IOException | NumberFormatException e) {
                System.err.println("Impossible de lire le fichier utilisateur de thème: " + e.getMessage());
                // on continue et on tentera le resource embarqué ci-dessous
            }
        } else {
            // 2) Charger depuis le classpath (ressource embarquée)
            try (InputStream in = Theme.class.getClassLoader().getResourceAsStream(THEME_CONFIG_FILE)) {
                if (in == null) {
                    System.err.println("theme.properties introuvable dans le classpath, utilisation des valeurs par défaut.");
                    loadDefaultFontsAndInsets();
                    UI_SCALE_OLD = UI_SCALE;
                    return;
                }
                props.load(in);
            } catch (IOException | NumberFormatException e) {
                System.err.println("Impossible de charger le thème embarqué: " + e.getMessage());
                loadDefaultFontsAndInsets();
                UI_SCALE_OLD = UI_SCALE;
                return;
            }
        }

        // Appliquer les propriétés (si présentes)
        try {
            FOND = parseColor(props.getProperty("color.fond"), FOND);
            FOND_CLAIR = parseColor(props.getProperty("color.fondClair"), FOND_CLAIR);
            FOND_HEADER = parseColor(props.getProperty("color.fondHeader"), FOND_HEADER);
            REGISTRE = parseColor(props.getProperty("color.registre"), REGISTRE);
            TEXTE = parseColor(props.getProperty("color.texte"), TEXTE);
            BORDURE = parseColor(props.getProperty("color.bordure"), BORDURE);
            HIGHLIGHT = parseColor(props.getProperty("color.highlight"), HIGHLIGHT);

            BTN_PRIMAIRE = parseColor(props.getProperty("color.btnPrimaire"), BTN_PRIMAIRE);
            BTN_DANGER = parseColor(props.getProperty("color.btnDanger"), BTN_DANGER);
            BTN_INFO = parseColor(props.getProperty("color.btnInfo"), BTN_INFO);
            BTN_NEUTRE = parseColor(props.getProperty("color.btnNeutre"), BTN_NEUTRE);
            BTN_TOOLBAR = parseColor(props.getProperty("color.btnToolbar"), BTN_TOOLBAR);

            MENU_BAR = parseColor(props.getProperty("color.menuBar"), MENU_BAR);
            TOOL_BAR = parseColor(props.getProperty("color.toolbar"), TOOL_BAR);

            SYNTAX_MNEMONIC = parseColor(props.getProperty("color.syntaxMnemonic"), SYNTAX_MNEMONIC);
            SYNTAX_OPERAND = parseColor(props.getProperty("color.syntaxOperand"), SYNTAX_OPERAND);
            SYNTAX_COMMENT = parseColor(props.getProperty("color.syntaxComment"), SYNTAX_COMMENT);
            SYNTAX_LABEL = parseColor(props.getProperty("color.syntaxLabel"), SYNTAX_LABEL);
            SYNTAX_DIRECTIVE = parseColor(props.getProperty("color.syntaxDirective"), SYNTAX_DIRECTIVE);
            SYNTAX_NUMBER = parseColor(props.getProperty("color.syntaxNumber"), SYNTAX_NUMBER);

            CPU_GRADIENT_TOP = parseColor(props.getProperty("color.cpuGradientTop"), CPU_GRADIENT_TOP);
            CPU_GRADIENT_BOTTOM = parseColor(props.getProperty("color.cpuGradientBottom"), CPU_GRADIENT_BOTTOM);
            CPU_BORDER = parseColor(props.getProperty("color.cpuBorder"), CPU_BORDER);
            BUS_COLOR = parseColor(props.getProperty("color.busColor"), BUS_COLOR);

            UI_SCALE = Float.parseFloat(props.getProperty("dim.uiScale", String.valueOf(UI_SCALE)));
            ESPACEMENT = Integer.parseInt(props.getProperty("dim.espacement", String.valueOf(ESPACEMENT)));
            EPAISSEUR_BUS = Integer.parseInt(props.getProperty("dim.epaisseurBus", String.valueOf(EPAISSEUR_BUS)));
            RAYON_ARRONDI = Integer.parseInt(props.getProperty("dim.rayonArrondi", String.valueOf(RAYON_ARRONDI)));
            RAYON_ARRONDI_GRAND = Integer.parseInt(props.getProperty("dim.rayonArrondiGrand", String.valueOf(RAYON_ARRONDI_GRAND)));

            FONT_UI_NAME = props.getProperty("font.uiName", FONT_UI_NAME);
            FONT_MONO_NAME = props.getProperty("font.monoName", FONT_MONO_NAME);

            loadDefaultFontsAndInsets();
        } catch (NumberFormatException e) {
            System.err.println("Impossible d'interpréter une valeur numérique du thème: " + e.getMessage());
            loadDefaultFontsAndInsets();
        } finally {
            UI_SCALE_OLD = UI_SCALE;
        }
    }

    /**
     * Sauvegarde les propriétés du thème dans le dossier utilisateur (~/.moto6908/theme.properties).
     * Ne tente pas d'écrire dans le JAR.
     */
    public static void saveThemeProperties() {
        Properties props = new Properties();

        props.setProperty("color.fond", toHexString(FOND));
        props.setProperty("color.fondClair", toHexString(FOND_CLAIR));
        props.setProperty("color.fondHeader", toHexString(FOND_HEADER));
        props.setProperty("color.registre", toHexString(REGISTRE));
        props.setProperty("color.texte", toHexString(TEXTE));
        props.setProperty("color.bordure", toHexString(BORDURE));
        props.setProperty("color.highlight", toHexString(HIGHLIGHT));

        props.setProperty("color.btnPrimaire", toHexString(BTN_PRIMAIRE));
        props.setProperty("color.btnDanger", toHexString(BTN_DANGER));
        props.setProperty("color.btnInfo", toHexString(BTN_INFO));
        props.setProperty("color.btnNeutre", toHexString(BTN_NEUTRE));
        props.setProperty("color.btnToolbar", toHexString(BTN_TOOLBAR));

        props.setProperty("color.menuBar", toHexString(MENU_BAR));
        props.setProperty("color.toolbar", toHexString(TOOL_BAR));

        props.setProperty("color.syntaxMnemonic", toHexString(SYNTAX_MNEMONIC));
        props.setProperty("color.syntaxOperand", toHexString(SYNTAX_OPERAND));
        props.setProperty("color.syntaxComment", toHexString(SYNTAX_COMMENT));
        props.setProperty("color.syntaxLabel", toHexString(SYNTAX_LABEL));
        props.setProperty("color.syntaxDirective", toHexString(SYNTAX_DIRECTIVE));
        props.setProperty("color.syntaxNumber", toHexString(SYNTAX_NUMBER));

        props.setProperty("color.cpuGradientTop", toHexString(CPU_GRADIENT_TOP));
        props.setProperty("color.cpuGradientBottom", toHexString(CPU_GRADIENT_BOTTOM));
        props.setProperty("color.cpuBorder", toHexString(CPU_BORDER));
        props.setProperty("color.busColor", toHexString(BUS_COLOR));

        props.setProperty("dim.uiScale", String.valueOf(UI_SCALE));
        props.setProperty("dim.espacement", String.valueOf(ESPACEMENT));
        props.setProperty("dim.epaisseurBus", String.valueOf(EPAISSEUR_BUS));
        props.setProperty("dim.rayonArrondi", String.valueOf(RAYON_ARRONDI));
        props.setProperty("dim.rayonArrondiGrand", String.valueOf(RAYON_ARRONDI_GRAND));

        props.setProperty("font.uiName", FONT_UI_NAME);
        props.setProperty("font.monoName", FONT_MONO_NAME);

        Path userConfig = getUserConfigPath();
        try {
            Files.createDirectories(userConfig.getParent());
            try (OutputStream out = Files.newOutputStream(userConfig)) {
                props.store(out, "Application Theme Settings");
            }
        } catch (IOException e) {
            System.err.println("Impossible de sauvegarder le thème: " + e.getMessage());
        }
    }
}
