package fr.billetterie.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Ticket(int id, String eventName, LocalDateTime eventDate, BigDecimal price, int stock) {
}
