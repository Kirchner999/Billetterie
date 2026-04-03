package fr.billetterie.utils;

import javafx.scene.Scene;

import java.util.prefs.Preferences;

public final class ThemeManager {

    private static final Preferences PREFS = Preferences.userNodeForPackage(ThemeManager.class);
    private static final String DEFAULT_THEME = "dark";
    private static String currentTheme = PREFS.get("theme", DEFAULT_THEME);
    private static Scene mainScene;

    private ThemeManager() {
    }

    public static void init(Scene scene) {
        mainScene = scene;
        applyTheme();
    }

    public static void applyTheme(Scene scene) {
        init(scene);
    }

    public static void toggleTheme() {
        currentTheme = currentTheme.equals("dark") ? "light" : "dark";
        PREFS.put("theme", currentTheme);
        applyTheme();
    }

    public static String getTheme() {
        return currentTheme;
    }

    private static void applyTheme() {
        if (mainScene == null) {
            return;
        }

        mainScene.getStylesheets().clear();
        mainScene.getStylesheets().add(
                ThemeManager.class.getResource("/themes/" + currentTheme + ".css").toExternalForm()
        );
    }
}
