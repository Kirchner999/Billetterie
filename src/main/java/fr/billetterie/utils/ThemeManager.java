package fr.billetterie.utils;

import javafx.scene.Scene;
import java.util.prefs.Preferences;

public class ThemeManager {

    private static final Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
    private static String currentTheme = prefs.get("theme", "dark");

    // Charger le thème sur une scène
    public static void applyTheme(Scene scene) {
        if (scene == null) return;

        scene.getStylesheets().clear();
        scene.getStylesheets().add(
                ThemeManager.class.getResource("/themes/" + currentTheme + ".css").toExternalForm()
        );
    }

    // Changer thème
    public static void switchTheme(Scene scene) {
        currentTheme = currentTheme.equals("dark") ? "light" : "dark";
        prefs.put("theme", currentTheme);

        applyTheme(scene);
    }

    public static String getTheme() {
        return currentTheme;
    }
}
