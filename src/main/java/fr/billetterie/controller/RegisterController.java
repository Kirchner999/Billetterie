package fr.billetterie.controller;

import fr.billetterie.App;
import fr.billetterie.dao.ClientDAO;
import fr.billetterie.model.Client;
import fr.billetterie.utils.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class RegisterController {

    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    @FXML
    public void handleRegister() {

        String nom = nomField.getText().trim();
        String email = emailField.getText().trim();
        String mdp = passwordField.getText().trim();

        if (nom.isEmpty() || email.isEmpty() || mdp.isEmpty()) {
            showAlert("Veuillez remplir tous les champs.");
            return;
        }

        if (ClientDAO.emailExists(email)) {
            showAlert("Cet email est déjà utilisé !");
            return;
        }

        Client c = new Client(
                0,              // ID auto
                "User",         // pseudo par défaut
                nom,
                "",             // prenom vide
                "",             // numero vide
                email,
                mdp,
                "",             // adresse vide
                false,          // isAdmin
                "CLIENT"        // rôle par défaut
        );

        if (ClientDAO.register(c)) {
            showAlert("Inscription réussie !");
            App.loadPage("Login.fxml");
        } else {
            showAlert("Erreur lors de l'inscription.");
        }
    }

    @FXML
    public void goToLogin() {
        App.loadPage("Login.fxml");
    }

    @FXML
    public void switchTheme() {
        ThemeManager.toggleTheme();
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg);
        a.showAndWait();
    }
}
