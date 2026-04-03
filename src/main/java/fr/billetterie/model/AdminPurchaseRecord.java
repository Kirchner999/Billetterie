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
        String pdfPath,
        LocalDateTime purchaseDate,
        String status,
        LocalDateTime refundedAt
) {
}
