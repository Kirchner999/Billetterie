package fr.billetterie.service;

import fr.billetterie.model.Client;
import fr.billetterie.repository.ClientRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    @Test
    void loginFailsWhenFieldsAreEmpty() {
        AuthService service = new AuthService(new StubClientRepository());

        LoginResult result = service.login(" ", "");

        assertFalse(result.success());
        assertEquals("Veuillez remplir tous les champs.", result.message());
    }

    @Test
    void loginReturnsAdminDashboardForAdmin() {
        StubClientRepository repository = new StubClientRepository();
        repository.authenticatedClient = new Client(1, "admin", "Admin", "", "", "admin@test.fr", "secret", "", true, "ADMIN");
        AuthService service = new AuthService(repository);

        LoginResult result = service.login("admin@test.fr", "secret");

        assertTrue(result.success());
        assertEquals("AdminDashboard.fxml", result.targetView());
    }

    @Test
    void loginReturnsClientDashboardForClient() {
        StubClientRepository repository = new StubClientRepository();
        repository.authenticatedClient = new Client(2, "user", "User", "", "", "user@test.fr", "secret", "", false, "CLIENT");
        AuthService service = new AuthService(repository);

        LoginResult result = service.login("user@test.fr", "secret");

        assertTrue(result.success());
        assertEquals("ClientDashboard.fxml", result.targetView());
    }

    private static final class StubClientRepository implements ClientRepository {
        private Client authenticatedClient;

        @Override
        public Client authenticate(String email, String password) {
            return authenticatedClient;
        }

        @Override
        public boolean emailExists(String email) {
            return false;
        }

        @Override
        public boolean register(Client client) {
            return true;
        }

        @Override
        public List<Client> getAll() {
            return List.of();
        }
    }
}
