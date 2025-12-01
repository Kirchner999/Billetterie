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

    private final ClientDAO clientDAO = new ClientDAO();

    @FXML
    public void handleRegister() {

        String nom = nomField.getText().trim();
        String email = emailField.getText().trim();
        String mdp = passwordField.getText().trim();

        if (nom.isEmpty() || email.isEmpty() || mdp.isEmpty()) {
            showAlert("Veuillez remplir tous les champs");
            return;
        }

        if (clientDAO.emailExists(email)) {
            showAlert("Cet email existe déjà !");
            return;
        }

        Client c = new Client(
                0,
                "User",     // pseudo par défaut
                nom,
                "",
                "",
                email,
                mdp,
                "",
                false,
                "CLIENT"
        );

        if (clientDAO.register(c)) {
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
