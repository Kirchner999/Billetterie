package fr.billetterie.controller;

import fr.billetterie.App;
import fr.billetterie.model.Client;
import fr.billetterie.repository.DaoClientRepository;
import fr.billetterie.service.AuthService;
import fr.billetterie.service.LoginResult;
import fr.billetterie.utils.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final AuthService authService = new AuthService(new DaoClientRepository());

    @FXML
    public void handleLogin() {
        LoginResult result = authService.login(emailField.getText(), passwordField.getText());
        errorLabel.setText("");

        if (!result.success()) {
            errorLabel.setText(result.message());
            return;
        }

        Client user = result.client();
        App.setCurrentUser(user);
        App.loadPage(result.targetView());
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
