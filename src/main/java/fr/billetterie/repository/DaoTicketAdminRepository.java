package fr.billetterie.repository;

import fr.billetterie.dao.TicketCatalogDAO;
import fr.billetterie.model.Ticket;

import java.util.List;

public class DaoTicketAdminRepository implements TicketAdminRepository {

    @Override
    public List<Ticket> getAllTickets() {
        return TicketCatalogDAO.getAllTickets();
    }

    @Override
    public boolean createTicket(Ticket ticket) {
        return TicketCatalogDAO.createTicket(ticket);
    }

    @Override
    public boolean updateTicket(Ticket ticket) {
        return TicketCatalogDAO.updateTicket(ticket);
    }

    @Override
    public boolean deleteTicket(int ticketId) {
        return TicketCatalogDAO.deleteTicket(ticketId);
    }
}
