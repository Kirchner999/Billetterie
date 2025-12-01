package fr.billetterie.controller;

import fr.billetterie.App;
import fr.billetterie.utils.ThemeManager;
import fr.billetterie.model.Client;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ClientDashboardController {

    @FXML
    private Label welcomeLabel;

    @FXML
    public void initialize() {
        Client user = App.getCurrentUser();

        if (welcomeLabel != null && user != null) {
            welcomeLabel.setText("Bienvenue, " + user.getNom() + " !");
        }
    }

    @FXML
    private void switchTheme() {
        ThemeManager.toggleTheme();
    }

    @FXML
    private void logout() {
        App.setCurrentUser(null);
        App.loadPage("Login.fxml");
    }
}
