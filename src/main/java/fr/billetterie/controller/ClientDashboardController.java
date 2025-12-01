package fr.billetterie.controller;

import fr.billetterie.App;
import fr.billetterie.model.Client;
import fr.billetterie.utils.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ClientDashboardController {

    @FXML private Label welcomeLabel;

    @FXML
    public void initialize() {
        Client user = App.getCurrentUser();
        if (user != null) {
            welcomeLabel.setText("Bienvenue, " + user.getPseudo());
        }
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
}
