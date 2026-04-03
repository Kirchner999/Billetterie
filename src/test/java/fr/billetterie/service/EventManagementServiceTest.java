package fr.billetterie.service;

import fr.billetterie.model.Ticket;
import fr.billetterie.repository.TicketAdminRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventManagementServiceTest {

    @Test
    void createRejectsBadDateFormat() {
        EventManagementService service = new EventManagementService(new StubTicketAdminRepository());

        EventFormResult result = service.create("Show", "03/04/2026", "39.90", "10");

        assertFalse(result.success());
        assertEquals("La date doit etre au format yyyy-MM-dd HH:mm.", result.message());
    }

    @Test
    void createSavesValidTicket() {
        StubTicketAdminRepository repository = new StubTicketAdminRepository();
        EventManagementService service = new EventManagementService(repository);

        EventFormResult result = service.create("Show", "2026-09-20 20:00", "39.90", "10");

        assertTrue(result.success());
        assertEquals("Show", repository.savedTicket.eventName());
        assertEquals(10, repository.savedTicket.stock());
    }

    @Test
    void deleteRequiresSelection() {
        EventManagementService service = new EventManagementService(new StubTicketAdminRepository());

        EventFormResult result = service.delete(0);

        assertFalse(result.success());
        assertEquals("Selectionne un evenement a supprimer.", result.message());
    }

    private static final class StubTicketAdminRepository implements TicketAdminRepository {
        private Ticket savedTicket;

        @Override
        public List<Ticket> getAllTickets() {
            return List.of();
        }

        @Override
        public boolean createTicket(Ticket ticket) {
            this.savedTicket = ticket;
            return true;
        }

        @Override
        public boolean updateTicket(Ticket ticket) {
            this.savedTicket = ticket;
            return true;
        }

        @Override
        public boolean deleteTicket(int ticketId) {
            return true;
        }
    }
}
