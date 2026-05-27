package fr.billetterie.service;

import fr.billetterie.model.Client;
import fr.billetterie.repository.ClientRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationServiceTest {

    @Test
    void registrationFailsWhenUsernameAlreadyExists() {
        StubClientRepository repository = new StubClientRepository();
        repository.usernameExists = true;
        RegistrationService service = new RegistrationService(repository);

        RegistrationResult result = service.register("alice", "", "", "secret1");

        assertFalse(result.success());
        assertEquals("Ce nom d'utilisateur est déjà utilisé !", result.message());
    }

    @Test
    void registrationFailsWhenPasswordIsTooShort() {
        RegistrationService service = new RegistrationService(new StubClientRepository());

        RegistrationResult result = service.register("alice", "", "", "123");

        assertFalse(result.success());
        assertEquals("Le mot de passe doit contenir au moins 6 caractères.", result.message());
    }

    @Test
    void registrationCreatesDefaultUserRole() {
        StubClientRepository repository = new StubClientRepository();
        RegistrationService service = new RegistrationService(repository);

        RegistrationResult result = service.register("alice", "", "", "secret1");

        assertTrue(result.success());
        assertEquals("user", repository.savedClient.getRole());
        assertEquals("alice", repository.savedClient.getUsername());
    }

    private static final class StubClientRepository implements ClientRepository {
        private boolean usernameExists;
        private Client savedClient;

        @Override
        public Client authenticate(String username, String password) {
            return null;
        }

        @Override
        public boolean usernameExists(String username) {
            return usernameExists;
        }

        @Override
        public boolean register(Client client) {
            this.savedClient = client;
            return true;
        }

        @Override
        public List<Client> getAll() {
            return List.of();
        }
    }
}
