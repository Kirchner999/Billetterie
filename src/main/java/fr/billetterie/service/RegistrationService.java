package fr.billetterie.service;

import fr.billetterie.model.Client;
import fr.billetterie.repository.ClientRepository;

public class RegistrationService {

    private final ClientRepository clientRepository;

    public RegistrationService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public RegistrationResult register(String pseudo, String nom, String email, String password) {
        String normalizedPseudo = normalize(pseudo);
        String normalizedNom = normalize(nom);
        String normalizedEmail = normalize(email);
        String normalizedPassword = normalize(password);

        if (normalizedPseudo.isEmpty() || normalizedNom.isEmpty() || normalizedEmail.isEmpty() || normalizedPassword.isEmpty()) {
            return RegistrationResult.failure("Veuillez remplir tous les champs.");
        }

        if (!normalizedEmail.contains("@")) {
            return RegistrationResult.failure("Adresse email invalide.");
        }

        if (normalizedPassword.length() < 6) {
            return RegistrationResult.failure("Le mot de passe doit contenir au moins 6 caracteres.");
        }

        if (clientRepository.emailExists(normalizedEmail)) {
            return RegistrationResult.failure("Cet email est deja utilise !");
        }

        Client client = new Client(
                0,
                normalizedPseudo,
                normalizedNom,
                "",
                "",
                normalizedEmail,
                normalizedPassword,
                "",
                false,
                "CLIENT"
        );

        if (!clientRepository.register(client)) {
            return RegistrationResult.failure("Erreur lors de l'inscription.");
        }

        return RegistrationResult.success("Inscription reussie !");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
