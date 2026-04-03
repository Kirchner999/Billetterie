package fr.billetterie.service;

import fr.billetterie.model.Client;
import fr.billetterie.repository.ClientRepository;

public class AuthService {

    private final ClientRepository clientRepository;

    public AuthService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public LoginResult login(String email, String password) {
        String normalizedEmail = normalize(email);
        String normalizedPassword = normalize(password);

        if (normalizedEmail.isEmpty() || normalizedPassword.isEmpty()) {
            return LoginResult.failure("Veuillez remplir tous les champs.");
        }

        Client user = clientRepository.authenticate(normalizedEmail, normalizedPassword);
        if (user == null) {
            return LoginResult.failure("Identifiants incorrects !");
        }

        return LoginResult.success(user, resolveTargetView(user));
    }

    static String resolveTargetView(Client user) {
        return switch (user.getRole()) {
            case "ADMIN" -> "AdminDashboard.fxml";
            case "EDITEUR" -> "EditeurDashboard.fxml";
            default -> "ClientDashboard.fxml";
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
