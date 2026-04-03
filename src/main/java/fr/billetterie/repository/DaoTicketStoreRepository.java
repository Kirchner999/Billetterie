package fr.billetterie.repository;

import fr.billetterie.dao.TicketCatalogDAO;
import fr.billetterie.model.Purchase;
import fr.billetterie.model.Ticket;

import java.util.List;

public class DaoTicketStoreRepository implements TicketStoreRepository {

    @Override
    public List<Ticket> getAvailableTickets() {
        return TicketCatalogDAO.getAvailableTickets();
    }

    @Override
    public List<Purchase> getPurchasesByUser(int userId) {
        return TicketCatalogDAO.getPurchasesByUser(userId);
    }

    @Override
    public PurchaseOperationResult purchaseTicket(int userId, int ticketId, int quantity) {
        return TicketCatalogDAO.purchaseTicket(userId, ticketId, quantity);
    }
}
