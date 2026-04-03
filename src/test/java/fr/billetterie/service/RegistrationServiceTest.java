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
    void registrationFailsWhenEmailAlreadyExists() {
        StubClientRepository repository = new StubClientRepository();
        repository.emailExists = true;
        RegistrationService service = new RegistrationService(repository);

        RegistrationResult result = service.register("alice", "Alice", "alice@test.fr", "secret1");

        assertFalse(result.success());
        assertEquals("Cet email est deja utilise !", result.message());
    }

    @Test
    void registrationFailsWhenPasswordIsTooShort() {
        RegistrationService service = new RegistrationService(new StubClientRepository());

        RegistrationResult result = service.register("alice", "Alice", "alice@test.fr", "123");

        assertFalse(result.success());
        assertEquals("Le mot de passe doit contenir au moins 6 caracteres.", result.message());
    }

    @Test
    void registrationCreatesDefaultClientRole() {
        StubClientRepository repository = new StubClientRepository();
        RegistrationService service = new RegistrationService(repository);

        RegistrationResult result = service.register("alice", "Alice", "alice@test.fr", "secret1");

        assertTrue(result.success());
        assertEquals("CLIENT", repository.savedClient.getRole());
        assertEquals("alice", repository.savedClient.getPseudo());
    }

    private static final class StubClientRepository implements ClientRepository {
        private boolean emailExists;
        private Client savedClient;

        @Override
        public Client authenticate(String email, String password) {
            return null;
        }

        @Override
        public boolean emailExists(String email) {
            return emailExists;
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
