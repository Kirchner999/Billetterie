package fr.billetterie.repository;

import fr.billetterie.dao.TicketCatalogDAO;
import fr.billetterie.model.Purchase;
import fr.billetterie.model.Seat;
import fr.billetterie.model.Ticket;

import java.util.List;
import java.util.Optional;

public class DaoTicketStoreRepository implements TicketStoreRepository {

    @Override
    public List<Ticket> getAvailableTickets() {
        return TicketCatalogDAO.getAvailableTickets();
    }

    @Override
    public List<Seat> getAvailableSeats(int ticketId) {
        return TicketCatalogDAO.getAvailableSeats(ticketId);
    }

    @Override
    public List<Purchase> getPurchasesByUser(int userId) {
        return TicketCatalogDAO.getPurchasesByUser(userId);
    }

    @Override
    public PurchaseOperationResult purchaseTicket(int userId, int ticketId, List<Integer> seatIds, int quantity) {
        return TicketCatalogDAO.purchaseTicket(userId, ticketId, seatIds, quantity);
    }

    @Override
    public boolean saveReceiptDocument(int purchaseId, String ticketNumber, String pdfPath, String seatLabels) {
        return TicketCatalogDAO.saveReceiptDocument(purchaseId, ticketNumber, pdfPath, seatLabels);
    }

    @Override
    public Optional<Ticket> findTicketById(int ticketId) {
        return TicketCatalogDAO.findTicketById(ticketId);
    }
}
