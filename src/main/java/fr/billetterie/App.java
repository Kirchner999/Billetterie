package fr.billetterie;

import fr.billetterie.model.Client;
import fr.billetterie.utils.ThemeManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private static Stage primaryStage;
    private static Scene mainScene;
    private static Client currentUser; // utilisateur connecté

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Login.fxml"));
        mainScene = new Scene(loader.load());

        // Initialiser le thème
        ThemeManager.init(mainScene);

        stage.setScene(mainScene);
        stage.setTitle("Billetterie");
        stage.show();
    }

    // ---------------------------------------------------------
    // PAGE LOADER GLOBAL
    // ---------------------------------------------------------
    public static void loadPage(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/views/" + fxml));
            mainScene.setRoot(loader.load());
            ThemeManager.toggleTheme(); // applique automatiquement le thème
        } catch (Exception e) {
            System.out.println("❌ Erreur loadPage(" + fxml + ")");
            e.printStackTrace();
        }
    }

    // ---------------------------------------------------------
    // UTILISATEUR CONNECTÉ
    // ---------------------------------------------------------
    public static Client getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(Client c) {
        currentUser = c;
    }

    // ---------------------------------------------------------
    // ACCÈS À LA SCENE POUR LE THEME SWITCH
    // ---------------------------------------------------------
    public static Scene getPrimaryScene() {
        return mainScene;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch();
    }
}
