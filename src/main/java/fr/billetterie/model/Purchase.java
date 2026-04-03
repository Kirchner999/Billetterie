package fr.billetterie.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Purchase(
        int id,
        int userId,
        int ticketId,
        String eventName,
        int quantity,
        BigDecimal total,
        LocalDateTime purchaseDate,
        String ticketNumber,
        String pdfPath,
        String seatLabels
) {
}
