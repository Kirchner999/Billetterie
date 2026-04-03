package fr.billetterie.service;

import fr.billetterie.model.Client;
import fr.billetterie.model.Seat;
import fr.billetterie.model.Ticket;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class TicketPdfService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Path RECEIPTS_DIRECTORY = Path.of("target", "generated-billets");
    private static final PDFont FONT_REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont FONT_ITALIC = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

    public Path generateReceipt(Client user, Ticket ticket, int quantity, List<Seat> seats) throws IOException {
        Files.createDirectories(RECEIPTS_DIRECTORY);

        String safeEventName = sanitizeFileSegment(ticket.eventName());
        String fileName = FILE_TIMESTAMP_FORMAT.format(LocalDateTime.now())
                + "-" + sanitizeFileSegment(user.getUsername())
                + "-" + safeEventName
                + ".pdf";

        Path output = RECEIPTS_DIRECTORY.resolve(fileName);
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float margin = 56;
                float y = page.getMediaBox().getHeight() - margin;

                y = writeLine(content, FONT_BOLD, 20, margin, y, "Billet / Recu de reservation");
                y -= 12;
                y = writeLine(content, FONT_REGULAR, 12, margin, y, "Client: " + user.getUsername());
                y = writeLine(content, FONT_REGULAR, 12, margin, y, "Evenement: " + ticket.eventName());
                y = writeLine(content, FONT_REGULAR, 12, margin, y, "Date du spectacle: " + DATE_FORMAT.format(ticket.eventDate()));
                y = writeLine(content, FONT_REGULAR, 12, margin, y, "Date d'emission: " + DATE_FORMAT.format(LocalDateTime.now()));
                y = writeLine(content, FONT_REGULAR, 12, margin, y, "Quantite: " + quantity);
                y = writeLine(content, FONT_REGULAR, 12, margin, y, "Prix unitaire: " + formatAmount(ticket.price()) + " EUR");
                y = writeLine(content, FONT_BOLD, 12, margin, y, "Total: " + formatAmount(ticket.price().multiply(BigDecimal.valueOf(quantity))) + " EUR");

                if (seats != null && !seats.isEmpty()) {
                    String seatLabel = seats.stream()
                            .map(Seat::displayLabel)
                            .collect(Collectors.joining(", "));
                    y = writeLine(content, FONT_REGULAR, 12, margin, y, "Places: " + seatLabel);
                } else {
                    y = writeLine(content, FONT_REGULAR, 12, margin, y, "Places: attribution libre");
                }

                y -= 20;
                writeLine(content, FONT_ITALIC, 10, margin, y,
                        "Document genere automatiquement par l'application de billetterie.");
            }

            document.save(output.toFile());
        }

        return output.toAbsolutePath().normalize();
    }

    private float writeLine(PDPageContentStream content, PDFont font, int fontSize, float x, float y, String text)
            throws IOException {
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
        return y - (fontSize + 8);
    }

    private String sanitizeFileSegment(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String safe = normalized.replaceAll("[^a-zA-Z0-9-_]+", "-").replaceAll("-{2,}", "-");
        safe = safe.replaceAll("^-|-$", "");
        return safe.isBlank() ? "billet" : safe.toLowerCase();
    }

    private String formatAmount(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }
}
