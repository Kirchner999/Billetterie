package fr.billetterie.service;

import fr.billetterie.model.Client;
import fr.billetterie.model.Seat;
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

        PurchaseOperationResult result = service.purchaseWithoutSeats(null, 1, "1");

        assertFalse(result.success());
        assertEquals("Aucun utilisateur connecte.", result.message());
    }

    @Test
    void purchaseFailsWithInvalidQuantity() {
        PurchaseService service = new PurchaseService(new StubTicketStoreRepository());

        PurchaseOperationResult result = service.purchaseWithoutSeats(new Client(1, "alice", "secret", "user"), 1, "abc");

        assertFalse(result.success());
        assertEquals("La quantite doit etre un nombre entier.", result.message());
    }

    @Test
    void purchaseDelegatesQuantityToRepository() {
        StubTicketStoreRepository repository = new StubTicketStoreRepository();
        PurchaseService service = new PurchaseService(repository);

        PurchaseOperationResult result = service.purchaseWithoutSeats(new Client(3, "alice", "secret", "user"), 7, "2");

        assertTrue(result.success());
        assertEquals(3, repository.userId);
        assertEquals(7, repository.ticketId);
        assertEquals(2, repository.quantity);
        assertTrue(repository.seatIds.isEmpty());
    }

    @Test
    void purchaseWithSeatsFailsWhenNoSeatSelected() {
        PurchaseService service = new PurchaseService(new StubTicketStoreRepository());

        PurchaseOperationResult result = service.purchaseWithSeats(new Client(1, "alice", "secret", "user"), 5, List.of());

        assertFalse(result.success());
        assertEquals("Selectionne au moins un siege.", result.message());
    }

    @Test
    void purchaseWithSeatsDelegatesSelectedSeatIds() {
        StubTicketStoreRepository repository = new StubTicketStoreRepository();
        PurchaseService service = new PurchaseService(repository);
        List<Seat> seats = java.util.Arrays.asList(new Seat(11, "A", 1, false), new Seat(12, "A", 2, false));

        PurchaseOperationResult result = service.purchaseWithSeats(new Client(4, "bob", "secret", "user"), 9, seats);

        assertTrue(result.success());
        assertEquals(4, repository.userId);
        assertEquals(9, repository.ticketId);
        assertEquals(2, repository.quantity);
        assertEquals(List.of(11, 12), repository.seatIds);
    }

    private static final class StubTicketStoreRepository implements TicketStoreRepository {
        private int userId;
        private int ticketId;
        private int quantity;
        private List<Integer> seatIds = List.of();

        @Override
        public java.util.List<fr.billetterie.model.Ticket> getAvailableTickets() {
            return List.of();
        }

        @Override
        public java.util.List<fr.billetterie.model.Seat> getAvailableSeats(int ticketId) {
            return List.of();
        }

        @Override
        public java.util.List<fr.billetterie.model.Purchase> getPurchasesByUser(int userId) {
            return List.of();
        }

        @Override
        public PurchaseOperationResult purchaseTicket(int userId, int ticketId, List<Integer> seatIds, int quantity) {
            this.userId = userId;
            this.ticketId = ticketId;
            this.seatIds = seatIds;
            this.quantity = quantity;
            return PurchaseOperationResult.success("ok", 42);
        }

        @Override
        public boolean saveReceiptDocument(int purchaseId, String ticketNumber, String pdfPath) {
            return true;
        }
    }
}
