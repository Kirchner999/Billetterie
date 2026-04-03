package fr.billetterie.model;

public record AdminSeatDetail(
        int seatId,
        int ticketId,
        String eventName,
        String seatLabel,
        boolean taken,
        String username,
        String ticketNumber,
        String status
) {
}
