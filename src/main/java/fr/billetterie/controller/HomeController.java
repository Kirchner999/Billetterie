package fr.billetterie.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Parent;

public class HomeController {

    @FXML private Button btnClients;
    @FXML private Button btnSpectacles;
    @FXML private Button btnRepresentations;
    @FXML private Button btnBillets;

    @FXML
    private void initialize() {
        btnClients.setOnAction(e -> openView("ClientView.fxml"));
        btnSpectacles.setOnAction(e -> openView("SpectacleView.fxml"));
        btnRepresentations.setOnAction(e -> openView("RepresentationView.fxml"));
        btnBillets.setOnAction(e -> openView("BilletView.fxml"));
    }

    private void openView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fr/billetterie/view/" + fxmlFile));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Billetterie - " + fxmlFile.replace(".fxml", ""));
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
