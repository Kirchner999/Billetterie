package fr.billetterie.utils;

import javafx.scene.Scene;
import java.util.prefs.Preferences;

public class ThemeManager {

    private static final Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
    private static String currentTheme = "dark";
    private static Scene mainScene;

    public static void init(Scene scene) {
        mainScene = scene;
        applyTheme();
    }

    public static void toggleTheme() {
        currentTheme = currentTheme.equals("dark") ? "light" : "dark";
        prefs.put("theme", currentTheme);
        applyTheme();
    }

    private static void applyTheme() {
        if (mainScene == null) return;

        mainScene.getStylesheets().clear();
        mainScene.getStylesheets().add(
                ThemeManager.class.getResource("/themes/" + currentTheme + ".css").toExternalForm()
        );
    }

    public static String getTheme() {
        return currentTheme;
    }
}
