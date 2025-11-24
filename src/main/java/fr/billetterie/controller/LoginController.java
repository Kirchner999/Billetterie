package fr.billetterie.controller;

import fr.billetterie.dao.ClientDAO;
import fr.billetterie.model.Client;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String pass = passwordField.getText().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            errorLabel.setText("Veuillez remplir tous les champs.");
            return;
        }

        Client c = ClientDAO.authenticate(email, pass);

        if (c == null) {
            errorLabel.setText("Identifiants incorrects.");
        } else {
            // Login OK → redirection vers Home
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Home.fxml"));
                Stage stage = (Stage) emailField.getScene().getWindow();
                stage.setScene(new Scene(loader.load()));
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
                errorLabel.setText("Erreur lors du chargement de la page.");
            }
        }
    }
    
}
