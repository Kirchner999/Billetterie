package fr.billetterie.controller;

import fr.billetterie.App;
import fr.billetterie.dao.ClientDAO;
import fr.billetterie.model.Client;
import fr.billetterie.utils.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final ClientDAO clientDAO = new ClientDAO();

    @FXML
    public void handleLogin() {

        String email = emailField.getText().trim();
        String mdp = passwordField.getText().trim();

        Client user = clientDAO.login(email, mdp);

        if (user == null) {
            errorLabel.setText("Identifiants incorrects");
            return;
        }

        App.setCurrentUser(user);

        switch (user.getRole()) {
            case "ADMIN" -> App.loadPage("AdminDashboard.fxml");
            case "EDITEUR" -> App.loadPage("EditeurDashboard.fxml");
            default -> App.loadPage("ClientDashboard.fxml");
        }
    }

    @FXML
    public void goToRegister() {
        App.loadPage("Register.fxml");
    }

    @FXML
    public void switchTheme() {
        ThemeManager.toggleTheme();
    }
}
