package fr.billetterie.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketArchiveServiceTest {

    @Test
    void createArchiveBuildsZipFromPdfFiles() throws Exception {
        TicketArchiveService service = new TicketArchiveService();
        Path dir = Files.createTempDirectory("archive-test");
        Path pdf1 = dir.resolve("ticket-1.pdf");
        Path pdf2 = dir.resolve("ticket-2.pdf");
        Files.writeString(pdf1, "pdf-one");
        Files.writeString(pdf2, "pdf-two");

        Path zipPath = service.createArchive("alice", List.of(pdf1, pdf2));

        assertTrue(Files.exists(zipPath));
        assertTrue(Files.size(zipPath) > 0);
    }
}
