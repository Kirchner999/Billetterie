package fr.billetterie.controller;

import fr.billetterie.dao.ClientDAO;
import fr.billetterie.model.Client;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;

public class RegisterController {

    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    @FXML
    public void handleRegister() {
        String nom = nomField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();

        if (nom.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        if (ClientDAO.emailExists(email)) {
            showError("Cet email est déjà utilisé.");
            return;
        }

        Client c = new Client(0, nom, email, password);
        ClientDAO.register(c);

        showInfo("Compte créé avec succès !");
        goToLogin();
    }

    @FXML
    public void goToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Login.fxml"));
            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.showAndWait();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.showAndWait();
    }
}
