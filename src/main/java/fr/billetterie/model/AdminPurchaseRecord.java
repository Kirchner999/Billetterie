package fr.billetterie.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminPurchaseRecord(
        int purchaseId,
        int userId,
        String username,
        int ticketId,
        String eventName,
        int quantity,
        BigDecimal total,
        String seatLabels,
        String ticketNumber,
        LocalDateTime purchaseDate,
        String status
) {
}
