package fr.billetterie.service;

import fr.billetterie.model.Client;
import fr.billetterie.repository.PurchaseOperationResult;
import fr.billetterie.repository.TicketStoreRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PurchaseServiceTest {

    @Test
    void purchaseFailsWithoutUser() {
        PurchaseService service = new PurchaseService(new StubTicketStoreRepository());

        PurchaseOperationResult result = service.purchase(null, 1, "1");

        assertFalse(result.success());
        assertEquals("Aucun utilisateur connecte.", result.message());
    }

    @Test
    void purchaseFailsWithInvalidQuantity() {
        PurchaseService service = new PurchaseService(new StubTicketStoreRepository());

        PurchaseOperationResult result = service.purchase(new Client(1, "alice", "secret", "user"), 1, "abc");

        assertFalse(result.success());
        assertEquals("La quantite doit etre un nombre entier.", result.message());
    }

    @Test
    void purchaseDelegatesToRepository() {
        StubTicketStoreRepository repository = new StubTicketStoreRepository();
        PurchaseService service = new PurchaseService(repository);

        PurchaseOperationResult result = service.purchase(new Client(3, "alice", "secret", "user"), 7, "2");

        assertTrue(result.success());
        assertEquals(3, repository.userId);
        assertEquals(7, repository.ticketId);
        assertEquals(2, repository.quantity);
    }

    private static final class StubTicketStoreRepository implements TicketStoreRepository {
        private int userId;
        private int ticketId;
        private int quantity;

        @Override
        public java.util.List<fr.billetterie.model.Ticket> getAvailableTickets() {
            return List.of();
        }

        @Override
        public java.util.List<fr.billetterie.model.Purchase> getPurchasesByUser(int userId) {
            return List.of();
        }

        @Override
        public PurchaseOperationResult purchaseTicket(int userId, int ticketId, int quantity) {
            this.userId = userId;
            this.ticketId = ticketId;
            this.quantity = quantity;
            return PurchaseOperationResult.success("ok");
        }
    }
}
