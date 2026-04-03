package fr.billetterie.service;

import fr.billetterie.model.Ticket;
import fr.billetterie.repository.TicketAdminRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EventManagementService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TicketAdminRepository ticketAdminRepository;

    public EventManagementService(TicketAdminRepository ticketAdminRepository) {
        this.ticketAdminRepository = ticketAdminRepository;
    }

    public EventFormResult create(String eventName, String eventDate, String price, String stock) {
        ParsedEvent parsed = parse(eventName, eventDate, price, stock);
        if (!parsed.result.success()) {
            return parsed.result;
        }

        boolean saved = ticketAdminRepository.createTicket(new Ticket(0, parsed.eventName, parsed.eventDate, parsed.price, parsed.stock));
        return saved ? EventFormResult.success("Evenement cree.") : EventFormResult.failure("Creation impossible.");
    }

    public EventFormResult update(int ticketId, String eventName, String eventDate, String price, String stock) {
        if (ticketId <= 0) {
            return EventFormResult.failure("Selectionne un evenement a modifier.");
        }

        ParsedEvent parsed = parse(eventName, eventDate, price, stock);
        if (!parsed.result.success()) {
            return parsed.result;
        }

        boolean saved = ticketAdminRepository.updateTicket(new Ticket(ticketId, parsed.eventName, parsed.eventDate, parsed.price, parsed.stock));
        return saved ? EventFormResult.success("Evenement mis a jour.") : EventFormResult.failure("Mise a jour impossible.");
    }

    public EventFormResult delete(int ticketId) {
        if (ticketId <= 0) {
            return EventFormResult.failure("Selectionne un evenement a supprimer.");
        }

        boolean deleted = ticketAdminRepository.deleteTicket(ticketId);
        return deleted ? EventFormResult.success("Evenement supprime.") : EventFormResult.failure("Suppression impossible.");
    }

    private ParsedEvent parse(String eventName, String eventDate, String priceValue, String stockValue) {
        String normalizedName = normalize(eventName);
        if (normalizedName.isEmpty()) {
            return new ParsedEvent(EventFormResult.failure("Le nom du spectacle est obligatoire."));
        }

        LocalDateTime parsedDate;
        try {
            parsedDate = LocalDateTime.parse(normalize(eventDate), FORMATTER);
        } catch (Exception e) {
            return new ParsedEvent(EventFormResult.failure("La date doit etre au format yyyy-MM-dd HH:mm."));
        }

        BigDecimal parsedPrice;
        try {
            parsedPrice = new BigDecimal(normalize(priceValue));
        } catch (Exception e) {
            return new ParsedEvent(EventFormResult.failure("Le prix doit etre un nombre valide."));
        }

        if (parsedPrice.signum() < 0) {
            return new ParsedEvent(EventFormResult.failure("Le prix doit etre positif."));
        }

        int parsedStock;
        try {
            parsedStock = Integer.parseInt(normalize(stockValue));
        } catch (Exception e) {
            return new ParsedEvent(EventFormResult.failure("Le stock doit etre un entier valide."));
        }

        if (parsedStock < 0) {
            return new ParsedEvent(EventFormResult.failure("Le stock doit etre positif ou nul."));
        }

        return new ParsedEvent(EventFormResult.success("ok"), normalizedName, parsedDate, parsedPrice, parsedStock);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private record ParsedEvent(EventFormResult result, String eventName, LocalDateTime eventDate, BigDecimal price, int stock) {
        private ParsedEvent(EventFormResult result) {
            this(result, null, null, null, 0);
        }
    }
}
