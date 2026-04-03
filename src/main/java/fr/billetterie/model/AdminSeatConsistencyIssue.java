package fr.billetterie.model;

public record AdminSeatConsistencyIssue(
        int ticketId,
        String eventName,
        int stock,
        int availableSeats,
        int difference
) {
}
