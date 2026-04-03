package fr.billetterie.model;

public record Seat(int id, String seatRow, int seatNumber, boolean taken) {

    public String displayLabel() {
        return seatRow + seatNumber;
    }

    @Override
    public String toString() {
        return displayLabel();
    }
}
