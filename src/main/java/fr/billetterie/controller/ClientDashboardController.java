package fr.billetterie.controller;

import fr.billetterie.App;
import fr.billetterie.model.Client;
import fr.billetterie.utils.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class ClientDashboardController {

    @FXML private StackPane contentPane;

    @FXML
    public void initialize() {
        Client user = App.getCurrentUser();
        String nom = user != null ? user.getNom() : "client";
        contentPane.getChildren().setAll(new Label("Bienvenue, " + nom + " !"));
    }

    @FXML
    public void switchTheme() {
        ThemeManager.toggleTheme();
    }

    @FXML
    public void logout() {
        App.setCurrentUser(null);
        App.loadPage("Login.fxml");
    }

    @FXML
    public void showSpectacles() {
        contentPane.getChildren().setAll(new Label("Catalogue des spectacles a venir"));
    }

    @FXML
    public void showMesBillets() {
        contentPane.getChildren().setAll(new Label("Historique et billets du client"));
    }
}
