package fr.billetterie.repository;

import fr.billetterie.model.Ticket;

import java.util.List;

public interface TicketAdminRepository {

    List<Ticket> getAllTickets();

    boolean createTicket(Ticket ticket);

    boolean updateTicket(Ticket ticket);

    boolean deleteTicket(int ticketId);
}
