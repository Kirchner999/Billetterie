package fr.billetterie.model;

import java.time.LocalDateTime;

public record TicketEventLog(
        int id,
        int purchaseId,
        String username,
        String eventName,
        String ticketNumber,
        String eventType,
        String details,
        LocalDateTime createdAt
) {
}
