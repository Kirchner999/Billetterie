package fr.billetterie.dao;

import fr.billetterie.model.AdminPurchaseRecord;
import fr.billetterie.model.Purchase;
import fr.billetterie.model.Seat;
import fr.billetterie.model.Ticket;
import fr.billetterie.model.TicketEventLog;
import fr.billetterie.repository.PurchaseOperationResult;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TicketCatalogDAO {

    private TicketCatalogDAO() {
    }

    public static List<Ticket> getAvailableTickets() {
        List<Ticket> tickets = new ArrayList<>();
        String sql = """
                SELECT id, event_name, event_date, price, stock
                FROM tickets
                WHERE event_date >= NOW() AND stock > 0
                ORDER BY event_date ASC
                """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                tickets.add(mapTicket(rs));
            }
        } catch (Exception e) {
            System.out.println("Erreur getAvailableTickets()");
            e.printStackTrace();
        }

        return tickets;
    }

    public static List<Ticket> getAllTickets() {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT id, event_name, event_date, price, stock FROM tickets ORDER BY event_date ASC";

        try (Connection conn = Database.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                tickets.add(mapTicket(rs));
            }
        } catch (Exception e) {
            System.out.println("Erreur getAllTickets()");
            e.printStackTrace();
        }

        return tickets;
    }

    public static Optional<Ticket> findTicketById(int ticketId) {
        String sql = "SELECT id, event_name, event_date, price, stock FROM tickets WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, ticketId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapTicket(rs));
                }
            }
        } catch (Exception e) {
            System.out.println("Erreur findTicketById()");
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public static boolean createTicket(Ticket ticket) {
        String sql = "INSERT INTO tickets (event_name, event_date, price, stock) VALUES (?, ?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, ticket.eventName());
            pst.setTimestamp(2, java.sql.Timestamp.valueOf(ticket.eventDate()));
            pst.setBigDecimal(3, ticket.price());
            pst.setInt(4, ticket.stock());
            pst.executeUpdate();
            return true;
        } catch (Exception e) {
            System.out.println("Erreur createTicket()");
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateTicket(Ticket ticket) {
        String sql = "UPDATE tickets SET event_name = ?, event_date = ?, price = ?, stock = ? WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, ticket.eventName());
            pst.setTimestamp(2, java.sql.Timestamp.valueOf(ticket.eventDate()));
            pst.setBigDecimal(3, ticket.price());
            pst.setInt(4, ticket.stock());
            pst.setInt(5, ticket.id());
            return pst.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Erreur updateTicket()");
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteTicket(int ticketId) {
        String deletePurchasesSql = "DELETE FROM purchases WHERE ticket_id = ?";
        String deleteSeatsSql = "DELETE FROM seats WHERE ticket_id = ?";
        String deleteTicketSql = "DELETE FROM tickets WHERE id = ?";

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensurePurchaseArtifactsSchema(conn);
                try (PreparedStatement pst = conn.prepareStatement(deletePurchasesSql)) {
                    pst.setInt(1, ticketId);
                    pst.executeUpdate();
                }
                try (PreparedStatement pst = conn.prepareStatement(deleteSeatsSql)) {
                    pst.setInt(1, ticketId);
                    pst.executeUpdate();
                }
                try (PreparedStatement pst = conn.prepareStatement(deleteTicketSql)) {
                    pst.setInt(1, ticketId);
                    pst.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            System.out.println("Erreur deleteTicket()");
            e.printStackTrace();
            return false;
        }
    }

    public static List<Seat> getAvailableSeats(int ticketId) {
        List<Seat> seats = new ArrayList<>();
        String sql = "SELECT id, seat_row, seat_number, is_taken FROM seats WHERE ticket_id = ? ORDER BY seat_row, seat_number";

        try (Connection conn = Database.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, ticketId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    seats.add(new Seat(
                            rs.getInt("id"),
                            rs.getString("seat_row"),
                            rs.getInt("seat_number"),
                            rs.getBoolean("is_taken")
                    ));
                }
            }
        } catch (Exception e) {
            System.out.println("Erreur getAvailableSeats()");
            e.printStackTrace();
        }

        return seats;
    }

    public static List<Purchase> getPurchasesByUser(int userId) {
        List<Purchase> purchases = new ArrayList<>();
        String sql = """
                SELECT
                    p.id,
                    p.user_id,
                    p.ticket_id,
                    t.event_name,
                    p.quantity,
                    p.total,
                    p.purchase_date,
                    tf.ticket_number AS resolved_ticket_number,
                    tf.pdf_path AS resolved_pdf_path,
                    NULLIF(GROUP_CONCAT(ps.seat_label ORDER BY ps.seat_label SEPARATOR ', '), '') AS resolved_seat_labels,
                    COALESCE(p.status, 'CONFIRMED') AS resolved_status
                FROM purchases p
                JOIN tickets t ON t.id = p.ticket_id
                LEFT JOIN ticket_files tf ON tf.purchase_id = p.id
                LEFT JOIN purchase_seats ps ON ps.purchase_id = p.id
                WHERE p.user_id = ?
                GROUP BY p.id, p.user_id, p.ticket_id, t.event_name, p.quantity, p.total, p.purchase_date, tf.ticket_number, tf.pdf_path, p.status
                ORDER BY p.purchase_date DESC
                """;

        try (Connection conn = Database.getConnection()) {
            ensurePurchaseArtifactsSchema(conn);
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setInt(1, userId);
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        purchases.add(new Purchase(
                                rs.getInt("id"),
                                rs.getInt("user_id"),
                                rs.getInt("ticket_id"),
                                rs.getString("event_name"),
                                rs.getInt("quantity"),
                                rs.getBigDecimal("total"),
                                rs.getTimestamp("purchase_date").toLocalDateTime(),
                                rs.getString("resolved_ticket_number"),
                                rs.getString("resolved_pdf_path"),
                                rs.getString("resolved_seat_labels"),
                                rs.getString("resolved_status")
                        ));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Erreur getPurchasesByUser()");
            e.printStackTrace();
        }

        return purchases;
    }

    public static PurchaseOperationResult purchaseTicket(int userId, int ticketId, List<Integer> seatIds, int quantity) {
        String ticketSql = "SELECT event_name, price, stock FROM tickets WHERE id = ? FOR UPDATE";
        String stockUpdateSql = "UPDATE tickets SET stock = stock - ? WHERE id = ?";
        String purchaseSql = "INSERT INTO purchases (user_id, ticket_id, quantity, total, purchase_date, status) VALUES (?, ?, ?, ?, NOW(), 'CONFIRMED')";

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensurePurchaseArtifactsSchema(conn);

                String eventName;
                BigDecimal price;
                int stock;

                try (PreparedStatement pst = conn.prepareStatement(ticketSql)) {
                    pst.setInt(1, ticketId);
                    try (ResultSet rs = pst.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return PurchaseOperationResult.failure("Evenement introuvable.");
                        }
                        eventName = rs.getString("event_name");
                        price = rs.getBigDecimal("price");
                        stock = rs.getInt("stock");
                    }
                }

                if (quantity <= 0) {
                    conn.rollback();
                    return PurchaseOperationResult.failure("La quantite doit etre superieure a 0.");
                }

                if (stock < quantity) {
                    conn.rollback();
                    return PurchaseOperationResult.failure("Stock insuffisant pour cet evenement.");
                }

                int totalSeatCount = countSeatsForTicket(conn, ticketId);
                if (totalSeatCount > 0) {
                    if (seatIds == null || seatIds.isEmpty()) {
                        conn.rollback();
                        return PurchaseOperationResult.failure("Choisis des sieges pour cet evenement.");
                    }
                    if (seatIds.size() != quantity) {
                        conn.rollback();
                        return PurchaseOperationResult.failure("Le nombre de sieges choisis doit correspondre a la quantite.");
                    }
                    if (!selectedSeatsAreAvailable(conn, ticketId, seatIds)) {
                        conn.rollback();
                        return PurchaseOperationResult.failure("Un ou plusieurs sieges selectionnes ne sont plus disponibles.");
                    }
                }

                try (PreparedStatement pst = conn.prepareStatement(stockUpdateSql)) {
                    pst.setInt(1, quantity);
                    pst.setInt(2, ticketId);
                    pst.executeUpdate();
                }

                BigDecimal total = price.multiply(BigDecimal.valueOf(quantity));
                Integer purchaseId = null;
                try (PreparedStatement pst = conn.prepareStatement(purchaseSql, Statement.RETURN_GENERATED_KEYS)) {
                    pst.setInt(1, userId);
                    pst.setInt(2, ticketId);
                    pst.setInt(3, quantity);
                    pst.setBigDecimal(4, total);
                    pst.executeUpdate();

                    try (ResultSet keys = pst.getGeneratedKeys()) {
                        if (keys.next()) {
                            purchaseId = keys.getInt(1);
                        }
                    }
                }

                if (seatIds != null && !seatIds.isEmpty()) {
                    markSeatsTaken(conn, seatIds);
                }

                conn.commit();
                return PurchaseOperationResult.success("Achat confirme pour " + eventName + " (x" + quantity + ").", purchaseId);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            System.out.println("Erreur purchaseTicket()");
            e.printStackTrace();
            return PurchaseOperationResult.failure("Erreur lors de l'achat.");
        }
    }

    public static boolean saveReceiptDocument(int purchaseId, String ticketNumber, String pdfPath, String seatLabels) {
        String upsertFileSql = """
                INSERT INTO ticket_files (purchase_id, ticket_number, pdf_path, generated_at)
                VALUES (?, ?, ?, NOW())
                ON DUPLICATE KEY UPDATE
                    ticket_number = VALUES(ticket_number),
                    pdf_path = VALUES(pdf_path),
                    generated_at = NOW()
                """;
        String deleteSeatsSql = "DELETE FROM purchase_seats WHERE purchase_id = ?";
        String insertSeatSql = "INSERT INTO purchase_seats (purchase_id, seat_label) VALUES (?, ?)";
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            ensurePurchaseArtifactsSchema(conn);
            try {
                try (PreparedStatement pst = conn.prepareStatement(upsertFileSql)) {
                    pst.setInt(1, purchaseId);
                    pst.setString(2, ticketNumber);
                    pst.setString(3, pdfPath);
                    pst.executeUpdate();
                }

                try (PreparedStatement pst = conn.prepareStatement(deleteSeatsSql)) {
                    pst.setInt(1, purchaseId);
                    pst.executeUpdate();
                }

                List<String> seatEntries = parseSeatLabels(seatLabels);
                if (!seatEntries.isEmpty()) {
                    try (PreparedStatement pst = conn.prepareStatement(insertSeatSql)) {
                        for (String seatLabel : seatEntries) {
                            pst.setInt(1, purchaseId);
                            pst.setString(2, seatLabel);
                            pst.addBatch();
                        }
                        pst.executeBatch();
                    }
                }

                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            System.out.println("Erreur saveReceiptDocument()");
            e.printStackTrace();
            return false;
        }
    }

    public static void logTicketEvent(int purchaseId, String eventType, String details) {
        try (Connection conn = Database.getConnection()) {
            ensurePurchaseArtifactsSchema(conn);
            logTicketEvent(conn, purchaseId, eventType, details);
        } catch (Exception e) {
            System.out.println("Erreur logTicketEvent()");
            e.printStackTrace();
        }
    }

    public static List<AdminPurchaseRecord> getAdminPurchases() {
        List<AdminPurchaseRecord> purchases = new ArrayList<>();
        String sql = """
                SELECT
                    p.id AS purchase_id,
                    p.user_id,
                    u.username,
                    p.ticket_id,
                    t.event_name,
                    p.quantity,
                    p.total,
                    p.purchase_date,
                    tf.ticket_number,
                    NULLIF(GROUP_CONCAT(ps.seat_label ORDER BY ps.seat_label SEPARATOR ', '), '') AS seat_labels,
                    COALESCE(p.status, 'CONFIRMED') AS purchase_status
                FROM purchases p
                JOIN users u ON u.id = p.user_id
                JOIN tickets t ON t.id = p.ticket_id
                LEFT JOIN ticket_files tf ON tf.purchase_id = p.id
                LEFT JOIN purchase_seats ps ON ps.purchase_id = p.id
                GROUP BY p.id, p.user_id, u.username, p.ticket_id, t.event_name, p.quantity, p.total, p.purchase_date, tf.ticket_number, p.status
                ORDER BY p.purchase_date DESC
                """;

        try (Connection conn = Database.getConnection()) {
            ensurePurchaseArtifactsSchema(conn);
            try (PreparedStatement pst = conn.prepareStatement(sql);
                 ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    purchases.add(new AdminPurchaseRecord(
                            rs.getInt("purchase_id"),
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getInt("ticket_id"),
                            rs.getString("event_name"),
                            rs.getInt("quantity"),
                            rs.getBigDecimal("total"),
                            rs.getString("seat_labels"),
                            rs.getString("ticket_number"),
                            rs.getTimestamp("purchase_date").toLocalDateTime(),
                            rs.getString("purchase_status")
                    ));
                }
            }
        } catch (Exception e) {
            System.out.println("Erreur getAdminPurchases()");
            e.printStackTrace();
        }

        return purchases;
    }

    public static List<TicketEventLog> getRecentTicketEvents(int limit) {
        List<TicketEventLog> events = new ArrayList<>();
        String sql = """
                SELECT
                    te.id,
                    te.purchase_id,
                    u.username,
                    t.event_name,
                    tf.ticket_number,
                    te.event_type,
                    te.details,
                    te.created_at
                FROM ticket_events te
                JOIN purchases p ON p.id = te.purchase_id
                JOIN users u ON u.id = p.user_id
                JOIN tickets t ON t.id = p.ticket_id
                LEFT JOIN ticket_files tf ON tf.purchase_id = p.id
                ORDER BY te.created_at DESC, te.id DESC
                LIMIT ?
                """;

        try (Connection conn = Database.getConnection()) {
            ensurePurchaseArtifactsSchema(conn);
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setInt(1, limit);
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        events.add(new TicketEventLog(
                                rs.getInt("id"),
                                rs.getInt("purchase_id"),
                                rs.getString("username"),
                                rs.getString("event_name"),
                                rs.getString("ticket_number"),
                                rs.getString("event_type"),
                                rs.getString("details"),
                                rs.getTimestamp("created_at").toLocalDateTime()
                        ));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Erreur getRecentTicketEvents()");
            e.printStackTrace();
        }

        return events;
    }

    public static PurchaseOperationResult cancelPurchase(int purchaseId) {
        return cancelPurchase(purchaseId, null);
    }

    public static PurchaseOperationResult cancelPurchase(int purchaseId, String reason) {
        String selectSql = """
                SELECT p.ticket_id, p.quantity, COALESCE(p.status, 'CONFIRMED') AS purchase_status, t.event_name
                FROM purchases p
                JOIN tickets t ON t.id = p.ticket_id
                WHERE p.id = ?
                FOR UPDATE
                """;
        String restockSql = "UPDATE tickets SET stock = stock + ? WHERE id = ?";
        String releaseSeatsSql = """
                UPDATE seats s
                JOIN purchase_seats ps
                  ON ps.purchase_id = ?
                 AND ps.seat_label = CONCAT(s.seat_row, s.seat_number)
                SET s.is_taken = 0
                WHERE s.ticket_id = ?
                """;
        String cancelSql = "UPDATE purchases SET status = 'CANCELLED' WHERE id = ?";

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensurePurchaseArtifactsSchema(conn);
                int ticketId;
                int quantity;
                String status;
                String eventName;

                try (PreparedStatement pst = conn.prepareStatement(selectSql)) {
                    pst.setInt(1, purchaseId);
                    try (ResultSet rs = pst.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return PurchaseOperationResult.failure("Achat introuvable.");
                        }
                        ticketId = rs.getInt("ticket_id");
                        quantity = rs.getInt("quantity");
                        status = rs.getString("purchase_status");
                        eventName = rs.getString("event_name");
                    }
                }

                if ("CANCELLED".equalsIgnoreCase(status)) {
                    conn.rollback();
                    return PurchaseOperationResult.failure("Cet achat est deja annule.");
                }

                try (PreparedStatement pst = conn.prepareStatement(restockSql)) {
                    pst.setInt(1, quantity);
                    pst.setInt(2, ticketId);
                    pst.executeUpdate();
                }

                try (PreparedStatement pst = conn.prepareStatement(releaseSeatsSql)) {
                    pst.setInt(1, purchaseId);
                    pst.setInt(2, ticketId);
                    pst.executeUpdate();
                }

                try (PreparedStatement pst = conn.prepareStatement(cancelSql)) {
                    pst.setInt(1, purchaseId);
                    pst.executeUpdate();
                }

                String details = "Achat annule depuis l'admin pour " + eventName;
                if (reason != null && !reason.isBlank()) {
                    details += " | motif: " + reason.trim();
                }
                logTicketEvent(conn, purchaseId, "CANCELLED", details);
                conn.commit();
                return PurchaseOperationResult.success("Achat annule et stock remis a jour.", purchaseId);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            System.out.println("Erreur cancelPurchase()");
            e.printStackTrace();
            return PurchaseOperationResult.failure("Erreur lors de l'annulation de l'achat.");
        }
    }

    public static int cleanupExpiredTickets() {
        String expiredIdsSql = "SELECT id FROM tickets WHERE event_date < NOW()";
        String deletePurchasesSql = "DELETE FROM purchases WHERE ticket_id = ?";
        String deleteSeatsSql = "DELETE FROM seats WHERE ticket_id = ?";
        String deleteTicketSql = "DELETE FROM tickets WHERE id = ?";

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensurePurchaseArtifactsSchema(conn);
                List<Integer> expiredIds = new ArrayList<>();
                try (PreparedStatement pst = conn.prepareStatement(expiredIdsSql);
                     ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        expiredIds.add(rs.getInt("id"));
                    }
                }

                for (Integer ticketId : expiredIds) {
                    try (PreparedStatement pst = conn.prepareStatement(deletePurchasesSql)) {
                        pst.setInt(1, ticketId);
                        pst.executeUpdate();
                    }
                    try (PreparedStatement pst = conn.prepareStatement(deleteSeatsSql)) {
                        pst.setInt(1, ticketId);
                        pst.executeUpdate();
                    }
                    try (PreparedStatement pst = conn.prepareStatement(deleteTicketSql)) {
                        pst.setInt(1, ticketId);
                        pst.executeUpdate();
                    }
                }

                conn.commit();
                return expiredIds.size();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            System.out.println("Erreur cleanupExpiredTickets()");
            e.printStackTrace();
            return 0;
        }
    }

    public static int countTickets() {
        return count("SELECT COUNT(*) FROM tickets WHERE event_date >= NOW()");
    }

    public static int countPurchases() {
        return count("SELECT COUNT(*) FROM purchases");
    }

    public static int countConfirmedPurchases() {
        return countPurchaseByStatus("CONFIRMED");
    }

    public static int countCancelledPurchases() {
        return countPurchaseByStatus("CANCELLED");
    }

    private static Ticket mapTicket(ResultSet rs) throws SQLException {
        return new Ticket(
                rs.getInt("id"),
                rs.getString("event_name"),
                rs.getTimestamp("event_date").toLocalDateTime(),
                rs.getBigDecimal("price"),
                rs.getInt("stock")
        );
    }

    private static void ensurePurchaseArtifactsSchema(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                    ALTER TABLE purchases
                    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED'
                    """);
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS ticket_files (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        purchase_id INT NOT NULL,
                        ticket_number VARCHAR(120) NOT NULL,
                        pdf_path VARCHAR(500) NOT NULL,
                        generated_at DATETIME NOT NULL,
                        UNIQUE KEY uk_ticket_files_purchase (purchase_id),
                        CONSTRAINT fk_ticket_files_purchase
                            FOREIGN KEY (purchase_id) REFERENCES purchases(id)
                            ON DELETE CASCADE
                    )
                    """);
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS purchase_seats (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        purchase_id INT NOT NULL,
                        seat_label VARCHAR(30) NOT NULL,
                        CONSTRAINT fk_purchase_seats_purchase
                            FOREIGN KEY (purchase_id) REFERENCES purchases(id)
                            ON DELETE CASCADE
                    )
                    """);
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS ticket_events (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        purchase_id INT NOT NULL,
                        event_type VARCHAR(40) NOT NULL,
                        details VARCHAR(500) NULL,
                        created_at DATETIME NOT NULL,
                        INDEX idx_ticket_events_purchase (purchase_id),
                        CONSTRAINT fk_ticket_events_purchase
                            FOREIGN KEY (purchase_id) REFERENCES purchases(id)
                            ON DELETE CASCADE
                    )
                    """);
        }
        backfillLegacyPurchaseArtifacts(conn);
    }

    private static void logTicketEvent(Connection conn, int purchaseId, String eventType, String details) throws SQLException {
        String sql = """
                INSERT INTO ticket_events (purchase_id, event_type, details, created_at)
                VALUES (?, ?, ?, NOW())
                """;
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, purchaseId);
            pst.setString(2, eventType);
            pst.setString(3, details);
            pst.executeUpdate();
        }
    }

    private static void backfillLegacyPurchaseArtifacts(Connection conn) throws SQLException {
        if (columnExists(conn, "purchases", "ticket_number") && columnExists(conn, "purchases", "pdf_path")) {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("""
                        INSERT INTO ticket_files (purchase_id, ticket_number, pdf_path, generated_at)
                        SELECT p.id, p.ticket_number, p.pdf_path, COALESCE(p.purchase_date, NOW())
                        FROM purchases p
                        LEFT JOIN ticket_files tf ON tf.purchase_id = p.id
                        WHERE tf.id IS NULL
                          AND p.ticket_number IS NOT NULL
                          AND p.pdf_path IS NOT NULL
                        """);
            }
        }

        if (columnExists(conn, "purchases", "seat_labels")) {
            String sql = """
                    SELECT p.id, p.seat_labels
                    FROM purchases p
                    LEFT JOIN purchase_seats ps ON ps.purchase_id = p.id
                    WHERE ps.id IS NULL
                      AND p.seat_labels IS NOT NULL
                      AND TRIM(p.seat_labels) <> ''
                    """;

            try (PreparedStatement pst = conn.prepareStatement(sql);
                 ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int purchaseId = rs.getInt("id");
                    List<String> labels = parseSeatLabels(rs.getString("seat_labels"));
                    if (labels.isEmpty()) {
                        continue;
                    }
                    try (PreparedStatement insert = conn.prepareStatement("INSERT INTO purchase_seats (purchase_id, seat_label) VALUES (?, ?)")) {
                        for (String label : labels) {
                            insert.setInt(1, purchaseId);
                            insert.setString(2, label);
                            insert.addBatch();
                        }
                        insert.executeBatch();
                    }
                }
            }
        }
    }

    private static List<String> parseSeatLabels(String seatLabels) {
        if (seatLabels == null || seatLabels.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(seatLabels.split(","))
                .map(String::trim)
                .filter(label -> !label.isBlank())
                .toList();
    }

    private static boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getColumns(conn.getCatalog(), null, tableName, columnName)) {
            return rs.next();
        }
    }

    private static int countSeatsForTicket(Connection conn, int ticketId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM seats WHERE ticket_id = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, ticketId);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static boolean selectedSeatsAreAvailable(Connection conn, int ticketId, List<Integer> seatIds) throws SQLException {
        String placeholders = seatIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        String sql = "SELECT COUNT(*) FROM seats WHERE ticket_id = ? AND is_taken = 0 AND id IN (" + placeholders + ")";

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, ticketId);
            for (int i = 0; i < seatIds.size(); i++) {
                pst.setInt(i + 2, seatIds.get(i));
            }
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() && rs.getInt(1) == seatIds.size();
            }
        }
    }

    private static void markSeatsTaken(Connection conn, List<Integer> seatIds) throws SQLException {
        String placeholders = seatIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        String sql = "UPDATE seats SET is_taken = 1 WHERE id IN (" + placeholders + ")";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            for (int i = 0; i < seatIds.size(); i++) {
                pst.setInt(i + 1, seatIds.get(i));
            }
            pst.executeUpdate();
        }
    }

    private static int count(String sql) {
        try (Connection conn = Database.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            System.out.println("Erreur count()");
            e.printStackTrace();
            return 0;
        }
    }

    private static int countPurchaseByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM purchases WHERE COALESCE(status, 'CONFIRMED') = ?";
        try (Connection conn = Database.getConnection()) {
            ensurePurchaseArtifactsSchema(conn);
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setString(1, status);
                try (ResultSet rs = pst.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        } catch (SQLException e) {
            System.out.println("Erreur countPurchaseByStatus()");
            e.printStackTrace();
            return 0;
        }
    }
}
