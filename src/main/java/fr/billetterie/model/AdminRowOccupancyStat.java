package fr.billetterie.model;

public record AdminRowOccupancyStat(
        String rowLabel,
        int totalSeats,
        int takenSeats,
        int availableSeats,
        double occupancyRate
) {
}
