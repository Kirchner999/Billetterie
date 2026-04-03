package fr.billetterie.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class TicketArchiveService {

    private static final DateTimeFormatter ARCHIVE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final Path ARCHIVE_DIRECTORY = Path.of("target", "generated-billets", "archives");

    public Path createArchive(String username, List<Path> pdfPaths) throws IOException {
        Files.createDirectories(ARCHIVE_DIRECTORY);
        Path zipPath = ARCHIVE_DIRECTORY.resolve(
                "billets-" + sanitize(username) + "-" + ARCHIVE_TIMESTAMP.format(LocalDateTime.now()) + ".zip"
        );

        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            for (Path pdfPath : pdfPaths) {
                if (pdfPath == null || !Files.exists(pdfPath) || Files.isDirectory(pdfPath)) {
                    continue;
                }

                ZipEntry entry = new ZipEntry(pdfPath.getFileName().toString());
                zip.putNextEntry(entry);
                try (InputStream input = Files.newInputStream(pdfPath)) {
                    input.transferTo(zip);
                }
                zip.closeEntry();
            }
        }

        return zipPath.toAbsolutePath().normalize();
    }

    private String sanitize(String value) {
        return value == null ? "client" : value.replaceAll("[^a-zA-Z0-9-_]+", "-").replaceAll("-{2,}", "-").replaceAll("^-|-$", "").toLowerCase();
    }
}
