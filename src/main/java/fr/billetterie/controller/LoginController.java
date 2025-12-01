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

    @FXML
    public void handleLogin() {

        String email = emailField.getText().trim();
        String mdp = passwordField.getText().trim();

        if (email.isEmpty() || mdp.isEmpty()) {
            errorLabel.setText("Veuillez remplir tous les champs.");
            return;
        }

        Client user = ClientDAO.authenticate(email, mdp);

        if (user == null) {
            errorLabel.setText("Identifiants incorrects !");
            return;
        }

        // Sauvegarde de l'utilisateur connecté
        App.setCurrentUser(user);

        // Redirection selon rôle
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
