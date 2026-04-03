package fr.billetterie.model;

public record AdminSeatOccupancyStat(
        int ticketId,
        String eventName,
        int totalSeats,
        int takenSeats,
        int availableSeats,
        double occupancyRate
) {
}
