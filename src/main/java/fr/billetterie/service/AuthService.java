package fr.billetterie.service;

import fr.billetterie.model.Client;
import fr.billetterie.repository.ClientRepository;

public class AuthService {

    private final ClientRepository clientRepository;

    public AuthService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public LoginResult login(String username, String password) {
        String normalizedUsername = normalize(username);
        String normalizedPassword = normalize(password);

        if (normalizedUsername.isEmpty() || normalizedPassword.isEmpty()) {
            return LoginResult.failure("Veuillez remplir tous les champs.");
        }

        Client user = clientRepository.authenticate(normalizedUsername, normalizedPassword);
        if (user == null) {
            return LoginResult.failure("Identifiants incorrects !");
        }

        return LoginResult.success(user, resolveTargetView(user));
    }

    static String resolveTargetView(Client user) {
        return user.isAdmin() ? "AdminDashboard.fxml" : "ClientDashboard.fxml";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
