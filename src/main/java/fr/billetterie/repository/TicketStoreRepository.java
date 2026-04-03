package fr.billetterie.repository;

import fr.billetterie.model.Purchase;
import fr.billetterie.model.Seat;
import fr.billetterie.model.Ticket;

import java.util.List;
import java.util.Optional;

public interface TicketStoreRepository {

    List<Ticket> getAvailableTickets();

    List<Seat> getAvailableSeats(int ticketId);

    List<Purchase> getPurchasesByUser(int userId);

    PurchaseOperationResult purchaseTicket(int userId, int ticketId, List<Integer> seatIds, int quantity);

    boolean saveReceiptDocument(int purchaseId, String ticketNumber, String pdfPath, String seatLabels);

    void logTicketEvent(int purchaseId, String eventType, String details);

    Optional<Ticket> findTicketById(int ticketId);
}
