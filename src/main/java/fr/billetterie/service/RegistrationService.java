package fr.billetterie.service;

import fr.billetterie.model.Client;
import fr.billetterie.repository.ClientRepository;

public class RegistrationService {

    private final ClientRepository clientRepository;

    public RegistrationService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public RegistrationResult register(String username, String fullName, String email, String password) {
        String normalizedUsername = normalize(username);
        String normalizedFullName = normalize(fullName);
        String normalizedEmail = normalize(email);
        String normalizedPassword = normalize(password);

        if (normalizedUsername.isEmpty() || normalizedPassword.isEmpty()) {
            return RegistrationResult.failure("Veuillez remplir les champs obligatoires.");
        }

        if (normalizedUsername.length() < 3) {
            return RegistrationResult.failure("Le nom d'utilisateur doit contenir au moins 3 caract\u00E8res.");
        }

        if (normalizedPassword.length() < 6) {
            return RegistrationResult.failure("Le mot de passe doit contenir au moins 6 caract\u00E8res.");
        }

        if (!normalizedEmail.isEmpty() && !normalizedEmail.contains("@")) {
            return RegistrationResult.failure("L'e-mail n'est pas valide.");
        }

        if (clientRepository.usernameExists(normalizedUsername)) {
            return RegistrationResult.failure("Ce nom d'utilisateur est d\u00E9j\u00E0 utilis\u00E9 !");
        }

        Client client = new Client(
                normalizedUsername,
                normalizedFullName.isEmpty() ? normalizedUsername : normalizedFullName,
                "",
                null,
                normalizedEmail.isEmpty() ? normalizedUsername : normalizedEmail,
                normalizedPassword,
                null,
                "user",
                false
        );
        if (!clientRepository.register(client)) {
            return RegistrationResult.failure("Erreur lors de l'inscription.");
        }

        return RegistrationResult.success("Inscription r\u00E9ussie !");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}