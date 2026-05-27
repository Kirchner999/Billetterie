package fr.billetterie;

import fr.billetterie.utils.ThemeManager;
import fr.billetterie.model.Client;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private static Stage primaryStage;
    private static Client currentUser;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
        Scene scene = new Scene(root);

        ThemeManager.applyTheme(scene);

        stage.setScene(scene);
        stage.setTitle("Dispelltacle");
        stage.show();
    }

    /** Charger une page FXML */
    public static void loadPage(String fxmlName) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/views/" + fxmlName));
            Parent root = loader.load();
            Scene scene = new Scene(root);

            ThemeManager.applyTheme(scene);
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Getter / Setter utilisateur connecté */
    public static Client getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(Client user) {
        currentUser = user;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch();
    }
}
