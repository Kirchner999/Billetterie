package fr.billetterie.repository;

import fr.billetterie.model.Purchase;
import fr.billetterie.model.Ticket;

import java.util.List;

public interface TicketStoreRepository {

    List<Ticket> getAvailableTickets();

    List<Purchase> getPurchasesByUser(int userId);

    PurchaseOperationResult purchaseTicket(int userId, int ticketId, int quantity);
}
