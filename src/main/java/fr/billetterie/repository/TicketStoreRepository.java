package fr.billetterie.repository;

import fr.billetterie.model.Purchase;
import fr.billetterie.model.Seat;
import fr.billetterie.model.Ticket;

import java.util.List;

public interface TicketStoreRepository {

    List<Ticket> getAvailableTickets();

    List<Seat> getAvailableSeats(int ticketId);

    List<Purchase> getPurchasesByUser(int userId);

    PurchaseOperationResult purchaseTicket(int userId, int ticketId, List<Integer> seatIds, int quantity);

    boolean saveReceiptDocument(int purchaseId, String ticketNumber, String pdfPath);
}
