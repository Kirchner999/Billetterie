package fr.billetterie.service;

import java.nio.file.Path;

public record TicketReceiptDocument(String ticketNumber, Path pdfPath) {
}
