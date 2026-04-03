package fr.billetterie.controller;

import fr.billetterie.App;
import fr.billetterie.repository.DaoClientRepository;
import fr.billetterie.service.RegistrationResult;
import fr.billetterie.service.RegistrationService;
import fr.billetterie.utils.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {

    @FXML private TextField pseudoField;
    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    private final RegistrationService registrationService = new RegistrationService(new DaoClientRepository());

    @FXML
    public void handleRegister() {
        RegistrationResult result = registrationService.register(
                pseudoField.getText(),
                nomField.getText(),
                emailField.getText(),
                passwordField.getText()
        );

        showAlert(result.message(), result.success() ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING);
        if (result.success()) {
            App.loadPage("Login.fxml");
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

    private void showAlert(String msg, Alert.AlertType type) {
        Alert alert = new Alert(type, msg);
        alert.showAndWait();
    }
}
