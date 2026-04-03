package fr.billetterie.service;

import fr.billetterie.model.Client;
import fr.billetterie.model.Seat;
import fr.billetterie.model.Ticket;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketPdfServiceTest {

    @Test
    void generateReceiptCreatesPdfFile() throws Exception {
        TicketPdfService service = new TicketPdfService();
        Client user = new Client(7, "alice", "secret", "user");
        Ticket ticket = new Ticket(3, "Starmania Live", LocalDateTime.of(2026, 6, 15, 21, 0), BigDecimal.valueOf(59.90), 25);

        Path pdf = service.generateReceipt(user, ticket, 2, List.of(
                new Seat(1, "A", 3, false),
                new Seat(2, "A", 4, false)
        ));

        assertTrue(Files.exists(pdf));
        assertTrue(Files.size(pdf) > 0);
        assertTrue(pdf.getFileName().toString().endsWith(".pdf"));
    }
}
