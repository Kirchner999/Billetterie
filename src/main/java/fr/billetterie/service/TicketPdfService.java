package fr.billetterie.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import fr.billetterie.model.Client;
import fr.billetterie.model.Purchase;
import fr.billetterie.model.Seat;
import fr.billetterie.model.Ticket;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class TicketPdfService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final Path RECEIPTS_DIRECTORY = Path.of("target", "generated-billets");
    private static final PDFont FONT_REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont FONT_ITALIC = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

    public TicketReceiptDocument generateReceipt(Client user, Ticket ticket, int quantity, List<Seat> seats) throws IOException {
        List<String> seatLabels = seats == null ? List.of() : seats.stream().map(Seat::displayLabel).toList();
        BigDecimal total = ticket.price().multiply(BigDecimal.valueOf(quantity));
        return writeReceipt(user, ticket.eventName(), ticket.eventDate(), quantity, total, seatLabels, null);
    }

    public TicketReceiptDocument regenerateReceipt(Client user, Purchase purchase, LocalDateTime eventDate) throws IOException {
        List<String> seatLabels = parseSeatLabels(purchase.seatLabels());
        return writeReceipt(user, purchase.eventName(), eventDate, purchase.quantity(), purchase.total(), seatLabels, purchase.ticketNumber());
    }

    private TicketReceiptDocument writeReceipt(
            Client user,
            String eventName,
            LocalDateTime eventDate,
            int quantity,
            BigDecimal total,
            List<String> seatLabels,
            String existingTicketNumber
    ) throws IOException {
        Files.createDirectories(RECEIPTS_DIRECTORY);

        String ticketNumber = existingTicketNumber != null && !existingTicketNumber.isBlank()
                ? existingTicketNumber
                : buildTicketNumber(user);
        String safeEventName = sanitizeFileSegment(eventName);
        String fileName = FILE_TIMESTAMP_FORMAT.format(LocalDateTime.now())
                + "-" + sanitizeFileSegment(user.getUsername())
                + "-" + safeEventName
                + ".pdf";

        Path output = RECEIPTS_DIRECTORY.resolve(fileName);
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float margin = 52;
                float y = page.getMediaBox().getHeight() - margin;

                y = writeLine(content, FONT_BOLD, 22, margin, y, "Billet electronique");
                y = writeLine(content, FONT_REGULAR, 12, margin, y, "Recu de reservation genere par l'application");
                y -= 8;
                y = drawDivider(content, margin, y, page.getMediaBox().getWidth() - margin);

                y = writeLine(content, FONT_BOLD, 12, margin, y, "Numero de billet: " + ticketNumber);
                y = writeLine(content, FONT_REGULAR, 12, margin, y, "Client: " + user.getUsername());
                y = writeLine(content, FONT_REGULAR, 12, margin, y, "Evenement: " + eventName);
                y = writeLine(content, FONT_REGULAR, 12, margin, y,
                        "Date du spectacle: " + (eventDate != null ? DATE_FORMAT.format(eventDate) : "indisponible"));
                y = writeLine(content, FONT_REGULAR, 12, margin, y, "Date d'emission: " + DATE_FORMAT.format(LocalDateTime.now()));
                y -= 8;

                y = writeLine(content, FONT_BOLD, 13, margin, y, "Reservation");
                y = writeLine(content, FONT_REGULAR, 12, margin, y, "Quantite: " + quantity);
                y = writeLine(content, FONT_REGULAR, 12, margin, y, "Prix unitaire: " + formatAmount(calculateUnitPrice(total, quantity)) + " EUR");
                y = writeLine(content, FONT_BOLD, 12, margin, y, "Total: " + formatAmount(total) + " EUR");

                String seatLabel = seatLabels == null || seatLabels.isEmpty()
                        ? "Attribution libre"
                        : String.join("  |  ", seatLabels);
                y = writeLine(content, FONT_REGULAR, 12, margin, y, "Places: " + seatLabel);

                PDImageXObject qrImage = buildQrCode(document, buildQrPayload(ticketNumber, user, eventName, eventDate, quantity, seatLabels));
                content.drawImage(qrImage, page.getMediaBox().getWidth() - margin - 120, y - 10, 120, 120);

                y -= 140;
                y = drawDivider(content, margin, y, page.getMediaBox().getWidth() - margin);
                writeLine(content, FONT_ITALIC, 10, margin, y - 10,
                        "Presente ce billet a l'entree. Le QR code reprend les informations de reservation.");
            }

            document.save(output.toFile());
        }

        return new TicketReceiptDocument(ticketNumber, output.toAbsolutePath().normalize());
    }

    private List<String> parseSeatLabels(String seatLabels) {
        if (seatLabels == null || seatLabels.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(seatLabels.split(","))
                .map(String::trim)
                .filter(label -> !label.isBlank())
                .toList();
    }

    private PDImageXObject buildQrCode(PDDocument document, String payload) throws IOException {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, 220, 220);
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(matrix);
            return LosslessFactory.createFromImage(document, qrImage);
        } catch (Exception e) {
            throw new IOException("Impossible de generer le QR code.", e);
        }
    }

    private String buildTicketNumber(Client user) {
        return "BLT-" + FILE_TIMESTAMP_FORMAT.format(LocalDateTime.now()) + "-U" + user.getId();
    }

    private String buildQrPayload(String ticketNumber, Client user, String eventName, LocalDateTime eventDate, int quantity, List<String> seatLabels) {
        String seatLabel = seatLabels == null || seatLabels.isEmpty()
                ? "Attribution libre"
                : String.join(", ", seatLabels);
        return "ticket=" + ticketNumber
                + ";user=" + user.getUsername()
                + ";event=" + eventName
                + ";date=" + (eventDate != null ? DATE_FORMAT.format(eventDate) : "indisponible")
                + ";quantity=" + quantity
                + ";seats=" + seatLabel;
    }

    private BigDecimal calculateUnitPrice(BigDecimal total, int quantity) {
        if (quantity <= 0) {
            return BigDecimal.ZERO;
        }
        return total.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
    }

    private float drawDivider(PDPageContentStream content, float startX, float y, float endX) throws IOException {
        content.moveTo(startX, y);
        content.lineTo(endX, y);
        content.stroke();
        return y - 18;
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

