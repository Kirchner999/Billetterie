package fr.billetterie.service;

import fr.billetterie.model.Client;
import fr.billetterie.model.Purchase;
import fr.billetterie.model.Seat;
import fr.billetterie.model.Ticket;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketPdfServiceTest {

    @Test
    void generateReceiptCreatesPdfFileWithTicketNumber() throws Exception {
        TicketPdfService service = new TicketPdfService();
        Client user = new Client(7, "alice", "secret", "user");
        Ticket ticket = new Ticket(3, "Starmania Live", LocalDateTime.of(2026, 6, 15, 21, 0), BigDecimal.valueOf(59.90), 25);

        TicketReceiptDocument receipt = service.generateReceipt(user, ticket, 2, List.of(
                new Seat(1, "A", 3, false),
                new Seat(2, "A", 4, false)
        ));

        assertNotNull(receipt);
        assertTrue(receipt.ticketNumber().startsWith("BLT-"));
        assertTrue(Files.exists(receipt.pdfPath()));
        assertTrue(Files.size(receipt.pdfPath()) > 0);
        assertTrue(receipt.pdfPath().getFileName().toString().endsWith(".pdf"));
    }

    @Test
    void regenerateReceiptKeepsExistingTicketNumber() throws Exception {
        TicketPdfService service = new TicketPdfService();
        Client user = new Client(7, "alice", "secret", "user");
        Purchase purchase = new Purchase(
                1,
                7,
                3,
                "Starmania Live",
                2,
                BigDecimal.valueOf(119.80),
                LocalDateTime.of(2026, 4, 3, 13, 0),
                "BLT-20260403-130000-U7",
                null,
                "A3, A4",
                "CONFIRMED"
        );

        TicketReceiptDocument receipt = service.regenerateReceipt(user, purchase, LocalDateTime.of(2026, 6, 15, 21, 0));

        assertEquals("BLT-20260403-130000-U7", receipt.ticketNumber());
        assertTrue(Files.exists(receipt.pdfPath()));
    }
}
