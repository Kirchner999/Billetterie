package fr.billetterie.controller;

import fr.billetterie.App;
import fr.billetterie.utils.ThemeManager;
import javafx.fxml.FXML;

public class EditeurDashboardController {

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
