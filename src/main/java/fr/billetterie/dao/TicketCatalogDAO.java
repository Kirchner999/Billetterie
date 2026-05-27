package fr.billetterie.dao;

import fr.billetterie.model.AdminPurchaseRecord;
import fr.billetterie.model.AdminSeatDetail;
import fr.billetterie.model.AdminRowOccupancyStat;
import fr.billetterie.model.AdminSalesStat;
import fr.billetterie.model.AdminSalesTimelinePoint;
import fr.billetterie.model.AdminSeatConsistencyIssue;
import fr.billetterie.model.AdminSeatOccupancyStat;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class TicketCatalogDAO {

    private TicketCatalogDAO() {
    }

    public static List<Ticket> getAvailableTickets() {
        List<Ticket> tickets = new ArrayList<>();
        String sql = """
                SELECT
                    t.id,
                    t.event_name,
                    t.event_date,
                    t.price,
                    CASE
                        WHEN COALESCE(seat_stats.total_seats, 0) > 0 THEN seat_stats.available_seats
                        ELSE t.stock
                    END AS stock
                FROM tickets t
                LEFT JOIN (
                    SELECT
                        ticket_id,
                        COUNT(*) AS total_seats,
                        COALESCE(SUM(CASE WHEN is_taken = 0 THEN 1 ELSE 0 END), 0) AS available_seats
                    FROM seats
                    GROUP BY ticket_id
                ) seat_stats ON seat_stats.ticket_id = t.id
                WHERE t.event_date >= NOW()
                  AND CASE
                        WHEN COALESCE(seat_stats.total_seats, 0) > 0 THEN seat_stats.available_seats
                        ELSE t.stock
                      END > 0
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
        String sql = """
                SELECT
                    t.id,
                    t.event_name,
                    t.event_date,
                    t.price,
                    CASE
                        WHEN COALESCE(seat_stats.total_seats, 0) > 0 THEN seat_stats.available_seats
                        ELSE t.stock
                    END AS stock
                FROM tickets t
                LEFT JOIN (
                    SELECT
                        ticket_id,
                        COUNT(*) AS total_seats,
                        COALESCE(SUM(CASE WHEN is_taken = 0 THEN 1 ELSE 0 END), 0) AS available_seats
                    FROM seats
                    GROUP BY ticket_id
                ) seat_stats ON seat_stats.ticket_id = t.id
                ORDER BY t.event_date ASC
                """;

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
        String sql = """
                SELECT
                    t.id,
                    t.event_name,
                    t.event_date,
                    t.price,
                    CASE
                        WHEN COALESCE(seat_stats.total_seats, 0) > 0 THEN seat_stats.available_seats
                        ELSE t.stock
                    END AS stock
                FROM tickets t
                LEFT JOIN (
                    SELECT
                        ticket_id,
                        COUNT(*) AS total_seats,
                        COALESCE(SUM(CASE WHEN is_taken = 0 THEN 1 ELSE 0 END), 0) AS available_seats
                    FROM seats
                    GROUP BY ticket_id
                ) seat_stats ON seat_stats.ticket_id = t.id
                WHERE t.id = ?
                """;
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
        String insertSpectacleSql = """
                INSERT INTO spectacle (
                    titre, lieu, affiche, tags, duree, description_courte, description_longue, langue, age_minimum, photos
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        String insertRepresentationSql = """
                INSERT INTO representation (id_spectacle, date_heure, salle, prix, stock)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int spectacleId;
                try (PreparedStatement pst = conn.prepareStatement(insertSpectacleSql, Statement.RETURN_GENERATED_KEYS)) {
                    pst.setString(1, ticket.eventName());
                    pst.setString(2, "Salle principale");
                    pst.setString(3, null);
                    pst.setString(4, "spectacle");
                    pst.setObject(5, 120);
                    pst.setString(6, ticket.eventName());
                    pst.setString(7, "Spectacle cree depuis le dashboard admin.");
                    pst.setString(8, "Francais");
                    pst.setObject(9, 0);
                    pst.setString(10, null);
                    pst.executeUpdate();
                    try (ResultSet keys = pst.getGeneratedKeys()) {
                        if (!keys.next()) {
                            conn.rollback();
                            return false;
                        }
                        spectacleId = keys.getInt(1);
                    }
                }

                try (PreparedStatement pst = conn.prepareStatement(insertRepresentationSql)) {
                    pst.setInt(1, spectacleId);
                    pst.setTimestamp(2, java.sql.Timestamp.valueOf(ticket.eventDate()));
                    pst.setString(3, "Salle principale");
                    pst.setBigDecimal(4, ticket.price());
                    pst.setInt(5, ticket.stock());
                    pst.executeUpdate();
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            return true;
        } catch (Exception e) {
            System.out.println("Erreur createTicket()");
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateTicket(Ticket ticket) {
        String sql = """
                UPDATE representation r
                JOIN spectacle s ON s.id = r.id_spectacle
                SET s.titre = ?, r.date_heure = ?, r.prix = ?, r.stock = ?
                WHERE r.id = ?
                """;

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
        String deletePurchasesSql = "DELETE FROM billet WHERE id_representation = ?";
        String deleteSeatsSql = "DELETE FROM seats WHERE ticket_id = ?";
        String deleteTicketSql = "DELETE FROM representation WHERE id = ?";

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
        String ticketSql = """
                SELECT s.titre AS event_name, r.prix AS price, r.stock AS stock
                FROM representation r
                JOIN spectacle s ON s.id = r.id_spectacle
                WHERE r.id = ?
                FOR UPDATE
                """;
        String stockUpdateSql = "UPDATE representation SET stock = stock - ? WHERE id = ?";
        String purchaseSql = """
                INSERT INTO billet (numero, id_representation, id_client, statut, quantite, total, date_achat)
                VALUES (?, ?, ?, 'valide', ?, ?, NOW())
                """;

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
                        return PurchaseOperationResult.failure("Choisis des sièges pour cet événement.");
                    }
                    if (seatIds.size() != quantity) {
                        conn.rollback();
                        return PurchaseOperationResult.failure("Le nombre de sièges choisis doit correspondre à la quantité.");
                    }
                    if (!selectedSeatsAreAvailable(conn, ticketId, seatIds)) {
                        conn.rollback();
                        return PurchaseOperationResult.failure("Un ou plusieurs sièges sélectionnés ne sont plus disponibles.");
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
                    pst.setString(1, buildTicketNumber(conn, ticketId));
                    pst.setInt(2, ticketId);
                    pst.setInt(3, userId);
                    pst.setInt(4, quantity);
                    pst.setBigDecimal(5, total);
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
                    tf.pdf_path,
                    NULLIF(GROUP_CONCAT(ps.seat_label ORDER BY ps.seat_label SEPARATOR ', '), '') AS seat_labels,
                    COALESCE(p.status, 'CONFIRMED') AS purchase_status,
                    p.refunded_at
                FROM purchases p
                JOIN users u ON u.id = p.user_id
                JOIN tickets t ON t.id = p.ticket_id
                LEFT JOIN ticket_files tf ON tf.purchase_id = p.id
                LEFT JOIN purchase_seats ps ON ps.purchase_id = p.id
                GROUP BY p.id, p.user_id, u.username, p.ticket_id, t.event_name, p.quantity, p.total, p.purchase_date, tf.ticket_number, tf.pdf_path, p.status, p.refunded_at
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
                            rs.getString("pdf_path"),
                            rs.getTimestamp("purchase_date").toLocalDateTime(),
                            rs.getString("purchase_status"),
                            rs.getTimestamp("refunded_at") != null ? rs.getTimestamp("refunded_at").toLocalDateTime() : null
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

    public static List<TicketEventLog> getTicketEventsForPurchase(int purchaseId) {
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
                WHERE te.purchase_id = ?
                ORDER BY te.created_at DESC, te.id DESC
                """;

        try (Connection conn = Database.getConnection()) {
            ensurePurchaseArtifactsSchema(conn);
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setInt(1, purchaseId);
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
            System.out.println("Erreur getTicketEventsForPurchase()");
            e.printStackTrace();
        }

        return events;
    }

    public static List<AdminSalesStat> getSalesStatsByTicket() {
        return getSalesStatsByTicket("all");
    }

    public static List<AdminSalesStat> getSalesStatsByTicket(String periodMode) {
        List<AdminSalesStat> stats = new ArrayList<>();
        String sql = """
                SELECT
                    t.event_name,
                    COALESCE(SUM(CASE WHEN COALESCE(p.status, 'CONFIRMED') = 'CONFIRMED' THEN 1 ELSE 0 END), 0) AS confirmed_purchases,
                    COALESCE(SUM(CASE WHEN COALESCE(p.status, 'CONFIRMED') = 'REFUNDED' THEN 1 ELSE 0 END), 0) AS refunded_purchases,
                    COALESCE(SUM(CASE WHEN COALESCE(p.status, 'CONFIRMED') = 'CANCELLED' THEN 1 ELSE 0 END), 0) AS cancelled_purchases,
                    COALESCE(SUM(CASE WHEN COALESCE(p.status, 'CONFIRMED') = 'CONFIRMED' THEN p.quantity ELSE 0 END), 0) AS tickets_sold,
                    COALESCE(SUM(CASE WHEN COALESCE(p.status, 'CONFIRMED') = 'CONFIRMED' THEN p.total ELSE 0 END), 0) AS revenue
                FROM tickets t
                LEFT JOIN purchases p ON p.ticket_id = t.id
                """ + buildSalesPeriodJoinClause(periodMode) + """
                GROUP BY t.id, t.event_name
                ORDER BY revenue DESC, tickets_sold DESC, t.event_name ASC
                """;

        try (Connection conn = Database.getConnection()) {
            ensurePurchaseArtifactsSchema(conn);
            try (PreparedStatement pst = conn.prepareStatement(sql);
                 ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    stats.add(new AdminSalesStat(
                            rs.getString("event_name"),
                            rs.getInt("confirmed_purchases"),
                            rs.getInt("refunded_purchases"),
                            rs.getInt("cancelled_purchases"),
                            rs.getInt("tickets_sold"),
                            rs.getBigDecimal("revenue")
                    ));
                }
            }
        } catch (Exception e) {
            System.out.println("Erreur getSalesStatsByTicket()");
            e.printStackTrace();
        }

        return stats;
    }

    public static List<AdminSalesTimelinePoint> getSalesTimeline(String periodMode) {
        List<AdminSalesTimelinePoint> points = new ArrayList<>();
        String sql = """
                SELECT
                    purchase_date,
                    refunded_at,
                    total,
                    COALESCE(status, 'CONFIRMED') AS purchase_status
                FROM purchases
                """ + buildSalesTimelineWhereClause(periodMode) + """
                ORDER BY purchase_date ASC
                """;

        try (Connection conn = Database.getConnection()) {
            ensurePurchaseArtifactsSchema(conn);
            try (PreparedStatement pst = conn.prepareStatement(sql);
                 ResultSet rs = pst.executeQuery()) {
                if (isAllPeriod(periodMode)) {
                    Map<YearMonth, RevenueBucket> buckets = initMonthlyBuckets(12);
                    while (rs.next()) {
                        if ("CONFIRMED".equalsIgnoreCase(rs.getString("purchase_status"))) {
                            LocalDateTime purchaseDate = rs.getTimestamp("purchase_date").toLocalDateTime();
                            YearMonth month = YearMonth.from(purchaseDate);
                            RevenueBucket bucket = buckets.get(month);
                            if (bucket != null) {
                                bucket.confirmed = bucket.confirmed.add(rs.getBigDecimal("total"));
                            }
                        }
                        if ("REFUNDED".equalsIgnoreCase(rs.getString("purchase_status")) && rs.getTimestamp("refunded_at") != null) {
                            LocalDateTime refundDate = rs.getTimestamp("refunded_at").toLocalDateTime();
                            YearMonth month = YearMonth.from(refundDate);
                            RevenueBucket bucket = buckets.get(month);
                            if (bucket != null) {
                                bucket.refunded = bucket.refunded.add(rs.getBigDecimal("total"));
                            }
                        }
                    }

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yyyy");
                    for (Map.Entry<YearMonth, RevenueBucket> entry : buckets.entrySet()) {
                        points.add(new AdminSalesTimelinePoint(
                                entry.getKey().format(formatter),
                                entry.getValue().confirmed,
                                entry.getValue().refunded
                        ));
                    }
                } else {
                    int days = resolveSalesPeriodDays(periodMode);
                    Map<LocalDate, RevenueBucket> buckets = initDailyBuckets(days);
                    while (rs.next()) {
                        if ("CONFIRMED".equalsIgnoreCase(rs.getString("purchase_status"))) {
                            LocalDate purchaseDate = rs.getTimestamp("purchase_date").toLocalDateTime().toLocalDate();
                            RevenueBucket bucket = buckets.get(purchaseDate);
                            if (bucket != null) {
                                bucket.confirmed = bucket.confirmed.add(rs.getBigDecimal("total"));
                            }
                        }
                        if ("REFUNDED".equalsIgnoreCase(rs.getString("purchase_status")) && rs.getTimestamp("refunded_at") != null) {
                            LocalDate refundDate = rs.getTimestamp("refunded_at").toLocalDateTime().toLocalDate();
                            RevenueBucket bucket = buckets.get(refundDate);
                            if (bucket != null) {
                                bucket.refunded = bucket.refunded.add(rs.getBigDecimal("total"));
                            }
                        }
                    }

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
                    for (Map.Entry<LocalDate, RevenueBucket> entry : buckets.entrySet()) {
                        points.add(new AdminSalesTimelinePoint(
                                entry.getKey().format(formatter),
                                entry.getValue().confirmed,
                                entry.getValue().refunded
                        ));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Erreur getSalesTimeline()");
            e.printStackTrace();
        }

        return points;
    }

    public static List<AdminSeatOccupancyStat> getSeatOccupancyStats() {
        List<AdminSeatOccupancyStat> stats = new ArrayList<>();
        String sql = """
                SELECT
                    t.id,
                    t.event_name,
                    COUNT(s.id) AS total_seats,
                    COALESCE(SUM(CASE WHEN s.is_taken = 1 THEN 1 ELSE 0 END), 0) AS taken_seats
                FROM tickets t
                LEFT JOIN seats s ON s.ticket_id = t.id
                GROUP BY t.id, t.event_name
                HAVING COUNT(s.id) > 0
                ORDER BY
                    CASE WHEN COUNT(s.id) = 0 THEN 0 ELSE SUM(CASE WHEN s.is_taken = 1 THEN 1 ELSE 0 END) / COUNT(s.id) END DESC,
                    t.event_name ASC
                """;

        try (Connection conn = Database.getConnection()) {
            ensurePurchaseArtifactsSchema(conn);
            try (PreparedStatement pst = conn.prepareStatement(sql);
                 ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int totalSeats = rs.getInt("total_seats");
                    int takenSeats = rs.getInt("taken_seats");
                    int availableSeats = Math.max(0, totalSeats - takenSeats);
                    double occupancyRate = totalSeats == 0 ? 0.0 : (takenSeats * 100.0) / totalSeats;
                    stats.add(new AdminSeatOccupancyStat(
                            rs.getInt("id"),
                            rs.getString("event_name"),
                            totalSeats,
                            takenSeats,
                            availableSeats,
                            occupancyRate
                    ));
                }
            }
        } catch (Exception e) {
            System.out.println("Erreur getSeatOccupancyStats()");
            e.printStackTrace();
        }

        return stats;
    }

    public static List<AdminRowOccupancyStat> getRowOccupancyStats(int ticketId) {
        List<AdminRowOccupancyStat> stats = new ArrayList<>();
        String sql = """
                SELECT
                    seat_row,
                    COUNT(id) AS total_seats,
                    COALESCE(SUM(CASE WHEN is_taken = 1 THEN 1 ELSE 0 END), 0) AS taken_seats
                FROM seats
                WHERE ticket_id = ?
                GROUP BY seat_row
                ORDER BY seat_row ASC
                """;

        try (Connection conn = Database.getConnection()) {
            ensurePurchaseArtifactsSchema(conn);
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setInt(1, ticketId);
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        int totalSeats = rs.getInt("total_seats");
                        int takenSeats = rs.getInt("taken_seats");
                        int availableSeats = Math.max(0, totalSeats - takenSeats);
                        double occupancyRate = totalSeats == 0 ? 0.0 : (takenSeats * 100.0) / totalSeats;
                        stats.add(new AdminRowOccupancyStat(
                                rs.getString("seat_row"),
                                totalSeats,
                                takenSeats,
                                availableSeats,
                                occupancyRate
                        ));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Erreur getRowOccupancyStats()");
            e.printStackTrace();
        }

        return stats;
    }

    public static List<AdminSeatConsistencyIssue> getSeatConsistencyIssues() {
        List<AdminSeatConsistencyIssue> issues = new ArrayList<>();
        String sql = """
                SELECT
                    t.id,
                    t.event_name,
                    t.stock,
                    COUNT(s.id) AS total_seats,
                    COALESCE(SUM(CASE WHEN s.is_taken = 1 THEN 1 ELSE 0 END), 0) AS taken_seats
                FROM tickets t
                JOIN seats s ON s.ticket_id = t.id
                GROUP BY t.id, t.event_name, t.stock
                HAVING t.stock <> (COUNT(s.id) - COALESCE(SUM(CASE WHEN s.is_taken = 1 THEN 1 ELSE 0 END), 0))
                ORDER BY ABS(t.stock - (COUNT(s.id) - COALESCE(SUM(CASE WHEN s.is_taken = 1 THEN 1 ELSE 0 END), 0))) DESC, t.event_name ASC
                """;

        try (Connection conn = Database.getConnection()) {
            ensurePurchaseArtifactsSchema(conn);
            try (PreparedStatement pst = conn.prepareStatement(sql);
                 ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int availableSeats = rs.getInt("total_seats") - rs.getInt("taken_seats");
                    issues.add(new AdminSeatConsistencyIssue(
                            rs.getInt("id"),
                            rs.getString("event_name"),
                            rs.getInt("stock"),
                            availableSeats,
                            rs.getInt("stock") - availableSeats
                    ));
                }
            }
        } catch (Exception e) {
            System.out.println("Erreur getSeatConsistencyIssues()");
            e.printStackTrace();
        }

        return issues;
    }

    public static PurchaseOperationResult alignTicketStockWithSeats(int ticketId) {
        try (Connection conn = Database.getConnection()) {
            ensurePurchaseArtifactsSchema(conn);
            return alignTicketStockWithSeatsTransactional(conn, ticketId);
        } catch (Exception e) {
            System.out.println("Erreur alignTicketStockWithSeats()");
            e.printStackTrace();
            return PurchaseOperationResult.failure("Impossible d'aligner le stock avec les sièges.");
        }
    }

    public static PurchaseOperationResult generateSeatsForRow(int ticketId, String rowLabel, int seatCount) {
        if (rowLabel == null || rowLabel.isBlank()) {
            return PurchaseOperationResult.failure("Le nom de rangee est obligatoire.");
        }
        if (seatCount <= 0) {
            return PurchaseOperationResult.failure("Le nombre de sièges doit être supérieur à 0.");
        }

        String maxSeatSql = "SELECT COALESCE(MAX(seat_number), 0) FROM seats WHERE ticket_id = ? AND seat_row = ?";
        String insertSeatSql = "INSERT INTO seats (ticket_id, seat_row, seat_number, is_taken) VALUES (?, ?, ?, 0)";

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensurePurchaseArtifactsSchema(conn);
                String normalizedRow = rowLabel.trim().toUpperCase();
                int startNumber;

                try (PreparedStatement pst = conn.prepareStatement(maxSeatSql)) {
                    pst.setInt(1, ticketId);
                    pst.setString(2, normalizedRow);
                    try (ResultSet rs = pst.executeQuery()) {
                        startNumber = rs.next() ? rs.getInt(1) + 1 : 1;
                    }
                }

                try (PreparedStatement pst = conn.prepareStatement(insertSeatSql)) {
                    for (int i = 0; i < seatCount; i++) {
                        pst.setInt(1, ticketId);
                        pst.setString(2, normalizedRow);
                        pst.setInt(3, startNumber + i);
                        pst.addBatch();
                    }
                    pst.executeBatch();
                }

                PurchaseOperationResult syncResult = alignTicketStockWithSeatsTransactional(conn, ticketId);
                if (!syncResult.success()) {
                    conn.rollback();
                    return syncResult;
                }

                conn.commit();
                return PurchaseOperationResult.success("Rangée " + normalizedRow + " générée avec " + seatCount + " sièges.");
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            System.out.println("Erreur generateSeatsForRow()");
            e.printStackTrace();
            return PurchaseOperationResult.failure("Impossible de generer la rangee.");
        }
    }

    public static PurchaseOperationResult generateSeatRows(int ticketId, String startRowLabel, int rowCount, int seatsPerRow) {
        if (startRowLabel == null || startRowLabel.isBlank()) {
            return PurchaseOperationResult.failure("La lettre de depart est obligatoire.");
        }
        if (rowCount <= 0) {
            return PurchaseOperationResult.failure("Le nombre de rangees doit etre superieur a 0.");
        }
        if (seatsPerRow <= 0) {
            return PurchaseOperationResult.failure("Le nombre de sièges par rangée doit être supérieur à 0.");
        }

        String normalizedStart = startRowLabel.trim().toUpperCase();
        char startChar = normalizedStart.charAt(0);
        if (!Character.isLetter(startChar)) {
            return PurchaseOperationResult.failure("La lettre de depart doit etre alphabetique.");
        }

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensurePurchaseArtifactsSchema(conn);

                for (int rowOffset = 0; rowOffset < rowCount; rowOffset++) {
                    String rowLabel = String.valueOf((char) (startChar + rowOffset));
                    int nextSeatNumber = getNextSeatNumberForRow(conn, ticketId, rowLabel);

                    try (PreparedStatement pst = conn.prepareStatement(
                            "INSERT INTO seats (ticket_id, seat_row, seat_number, is_taken) VALUES (?, ?, ?, 0)")) {
                        for (int seatOffset = 0; seatOffset < seatsPerRow; seatOffset++) {
                            pst.setInt(1, ticketId);
                            pst.setString(2, rowLabel);
                            pst.setInt(3, nextSeatNumber + seatOffset);
                            pst.addBatch();
                        }
                        pst.executeBatch();
                    }
                }

                PurchaseOperationResult syncResult = alignTicketStockWithSeatsTransactional(conn, ticketId);
                if (!syncResult.success()) {
                    conn.rollback();
                    return syncResult;
                }

                conn.commit();
                return PurchaseOperationResult.success("Generation de " + rowCount + " rangee(s) a partir de " + normalizedStart + " terminee.");
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            System.out.println("Erreur generateSeatRows()");
            e.printStackTrace();
            return PurchaseOperationResult.failure("Impossible de generer les rangees.");
        }
    }

    public static PurchaseOperationResult alignAllTicketStocksWithSeats() {
        String selectSql = "SELECT DISTINCT ticket_id FROM seats ORDER BY ticket_id";

        try (Connection conn = Database.getConnection()) {
            ensurePurchaseArtifactsSchema(conn);
            int updated = 0;
            try (PreparedStatement pst = conn.prepareStatement(selectSql);
                 ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    if (alignTicketStockWithSeatsTransactional(conn, rs.getInt(1)).success()) {
                        updated++;
                    }
                }
            }
            return PurchaseOperationResult.success("Correction globale terminee sur " + updated + " spectacle(s).");
        } catch (Exception e) {
            System.out.println("Erreur alignAllTicketStocksWithSeats()");
            e.printStackTrace();
            return PurchaseOperationResult.failure("Impossible d'aligner globalement les stocks.");
        }
    }

    public static PurchaseOperationResult deleteSeat(int seatId) {
        String selectSql = "SELECT ticket_id, seat_row, seat_number, is_taken FROM seats WHERE id = ? FOR UPDATE";
        String deleteSql = "DELETE FROM seats WHERE id = ?";

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensurePurchaseArtifactsSchema(conn);
                int ticketId;
                String row;
                int number;
                boolean taken;

                try (PreparedStatement pst = conn.prepareStatement(selectSql)) {
                    pst.setInt(1, seatId);
                    try (ResultSet rs = pst.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return PurchaseOperationResult.failure("Siege introuvable.");
                        }
                        ticketId = rs.getInt("ticket_id");
                        row = rs.getString("seat_row");
                        number = rs.getInt("seat_number");
                        taken = rs.getBoolean("is_taken");
                    }
                }

                if (taken) {
                    conn.rollback();
                    return PurchaseOperationResult.failure("Impossible de supprimer un siege deja reserve.");
                }

                try (PreparedStatement pst = conn.prepareStatement(deleteSql)) {
                    pst.setInt(1, seatId);
                    pst.executeUpdate();
                }

                PurchaseOperationResult syncResult = alignTicketStockWithSeatsTransactional(conn, ticketId);
                if (!syncResult.success()) {
                    conn.rollback();
                    return syncResult;
                }

                conn.commit();
                return PurchaseOperationResult.success("Siege " + row + number + " supprime.");
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            System.out.println("Erreur deleteSeat()");
            e.printStackTrace();
            return PurchaseOperationResult.failure("Impossible de supprimer le siege.");
        }
    }

    public static PurchaseOperationResult deleteSeatRow(int ticketId, String rowLabel) {
        String checkSql = "SELECT COUNT(*) AS total_count, COALESCE(SUM(CASE WHEN is_taken = 1 THEN 1 ELSE 0 END), 0) AS taken_count FROM seats WHERE ticket_id = ? AND seat_row = ?";
        String deleteSql = "DELETE FROM seats WHERE ticket_id = ? AND seat_row = ?";

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensurePurchaseArtifactsSchema(conn);
                String normalizedRow = rowLabel == null ? "" : rowLabel.trim().toUpperCase();
                int totalCount;
                int takenCount;

                try (PreparedStatement pst = conn.prepareStatement(checkSql)) {
                    pst.setInt(1, ticketId);
                    pst.setString(2, normalizedRow);
                    try (ResultSet rs = pst.executeQuery()) {
                        rs.next();
                        totalCount = rs.getInt("total_count");
                        takenCount = rs.getInt("taken_count");
                    }
                }

                if (totalCount == 0) {
                    conn.rollback();
                    return PurchaseOperationResult.failure("Rangee introuvable.");
                }
                if (takenCount > 0) {
                    conn.rollback();
                    return PurchaseOperationResult.failure("Impossible de supprimer une rangée contenant des sièges réservés.");
                }

                try (PreparedStatement pst = conn.prepareStatement(deleteSql)) {
                    pst.setInt(1, ticketId);
                    pst.setString(2, normalizedRow);
                    pst.executeUpdate();
                }

                PurchaseOperationResult syncResult = alignTicketStockWithSeatsTransactional(conn, ticketId);
                if (!syncResult.success()) {
                    conn.rollback();
                    return syncResult;
                }

                conn.commit();
                return PurchaseOperationResult.success("Rangee " + normalizedRow + " supprimee.");
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            System.out.println("Erreur deleteSeatRow()");
            e.printStackTrace();
            return PurchaseOperationResult.failure("Impossible de supprimer la rangee.");
        }
    }

    public static Optional<AdminSeatDetail> findSeatDetail(int seatId) {
        String sql = """
                SELECT
                    s.id AS seat_id,
                    s.ticket_id,
                    t.event_name,
                    CONCAT(s.seat_row, s.seat_number) AS seat_label,
                    s.is_taken,
                    u.username,
                    tf.ticket_number,
                    COALESCE(p.status, 'CONFIRMED') AS purchase_status
                FROM seats s
                JOIN tickets t ON t.id = s.ticket_id
                LEFT JOIN purchase_seats ps ON ps.purchase_id IS NOT NULL AND ps.seat_label = CONCAT(s.seat_row, s.seat_number)
                LEFT JOIN purchases p ON p.id = ps.purchase_id AND p.ticket_id = s.ticket_id
                LEFT JOIN users u ON u.id = p.user_id
                LEFT JOIN ticket_files tf ON tf.purchase_id = p.id
                WHERE s.id = ?
                ORDER BY p.purchase_date DESC
                LIMIT 1
                """;

        try (Connection conn = Database.getConnection()) {
            ensurePurchaseArtifactsSchema(conn);
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setInt(1, seatId);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(new AdminSeatDetail(
                                rs.getInt("seat_id"),
                                rs.getInt("ticket_id"),
                                rs.getString("event_name"),
                                rs.getString("seat_label"),
                                rs.getBoolean("is_taken"),
                                rs.getString("username"),
                                rs.getString("ticket_number"),
                                rs.getString("purchase_status")
                        ));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Erreur findSeatDetail()");
            e.printStackTrace();
        }

        return Optional.empty();
    }

    public static PurchaseOperationResult cancelPurchase(int purchaseId) {
        return cancelPurchase(purchaseId, null);
    }

    public static PurchaseOperationResult cancelPurchase(int purchaseId, String reason) {
        String selectSql = """
                SELECT
                    b.id_representation AS ticket_id,
                    b.quantite AS quantity,
                    CASE
                        WHEN b.statut = 'valide' THEN 'CONFIRMED'
                        WHEN b.statut = 'annule' THEN 'CANCELLED'
                        WHEN b.statut = 'rembourse' THEN 'REFUNDED'
                        ELSE 'CONFIRMED'
                    END AS purchase_status,
                    s.titre AS event_name
                FROM billet b
                JOIN representation r ON r.id = b.id_representation
                JOIN spectacle s ON s.id = r.id_spectacle
                WHERE b.id = ?
                FOR UPDATE
                """;
        String restockSql = "UPDATE representation SET stock = stock + ? WHERE id = ?";
        String releaseSeatsSql = """
                UPDATE seats s
                JOIN purchase_seats ps
                  ON ps.purchase_id = ?
                 AND ps.seat_label = CONCAT(s.seat_row, s.seat_number)
                SET s.is_taken = 0
                WHERE s.ticket_id = ?
                """;
        String cancelSql = "UPDATE billet SET statut = 'annule', refunded_at = NULL WHERE id = ?";

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
                if ("REFUNDED".equalsIgnoreCase(status)) {
                    conn.rollback();
                    return PurchaseOperationResult.failure("Cet achat est deja rembourse.");
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

    public static PurchaseOperationResult refundPurchase(int purchaseId, String reason) {
        String selectSql = """
                SELECT
                    b.id_representation AS ticket_id,
                    b.quantite AS quantity,
                    b.total,
                    CASE
                        WHEN b.statut = 'valide' THEN 'CONFIRMED'
                        WHEN b.statut = 'annule' THEN 'CANCELLED'
                        WHEN b.statut = 'rembourse' THEN 'REFUNDED'
                        ELSE 'CONFIRMED'
                    END AS purchase_status,
                    s.titre AS event_name
                FROM billet b
                JOIN representation r ON r.id = b.id_representation
                JOIN spectacle s ON s.id = r.id_spectacle
                WHERE b.id = ?
                FOR UPDATE
                """;
        String restockSql = "UPDATE representation SET stock = stock + ? WHERE id = ?";
        String releaseSeatsSql = """
                UPDATE seats s
                JOIN purchase_seats ps
                  ON ps.purchase_id = ?
                 AND ps.seat_label = CONCAT(s.seat_row, s.seat_number)
                SET s.is_taken = 0
                WHERE s.ticket_id = ?
                """;
        String refundSql = "UPDATE billet SET statut = 'rembourse', refunded_at = NOW() WHERE id = ?";

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensurePurchaseArtifactsSchema(conn);
                int ticketId;
                int quantity;
                BigDecimal total;
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
                        total = rs.getBigDecimal("total");
                        status = rs.getString("purchase_status");
                        eventName = rs.getString("event_name");
                    }
                }

                if ("REFUNDED".equalsIgnoreCase(status)) {
                    conn.rollback();
                    return PurchaseOperationResult.failure("Cet achat est deja rembourse.");
                }
                if ("CANCELLED".equalsIgnoreCase(status)) {
                    conn.rollback();
                    return PurchaseOperationResult.failure("Un achat annule n'est pas remboursable ici.");
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

                try (PreparedStatement pst = conn.prepareStatement(refundSql)) {
                    pst.setInt(1, purchaseId);
                    pst.executeUpdate();
                }

                String details = "Achat rembourse pour " + eventName + " | montant: " + total + " EUR";
                if (reason != null && !reason.isBlank()) {
                    details += " | motif: " + reason.trim();
                }
                logTicketEvent(conn, purchaseId, "REFUNDED", details);
                conn.commit();
                return PurchaseOperationResult.success("Achat rembourse et stock remis a jour.", purchaseId);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            System.out.println("Erreur refundPurchase()");
            e.printStackTrace();
            return PurchaseOperationResult.failure("Erreur lors du remboursement de l'achat.");
        }
    }

    public static int cleanupExpiredTickets() {
        String expiredIdsSql = "SELECT id FROM representation WHERE date_heure < NOW()";
        String deletePurchasesSql = "DELETE FROM billet WHERE id_representation = ?";
        String deleteSeatsSql = "DELETE FROM seats WHERE ticket_id = ?";
        String deleteTicketSql = "DELETE FROM representation WHERE id = ?";

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

    public static int countRefundedPurchases() {
        return countPurchaseByStatus("REFUNDED");
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
                    CREATE TABLE IF NOT EXISTS ticket_files (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        purchase_id INT NOT NULL,
                        ticket_number VARCHAR(120) NOT NULL,
                        pdf_path VARCHAR(500) NOT NULL,
                        generated_at DATETIME NOT NULL,
                        UNIQUE KEY uk_ticket_files_purchase (purchase_id),
                        CONSTRAINT fk_ticket_files_purchase
                            FOREIGN KEY (purchase_id) REFERENCES billet(id)
                            ON DELETE CASCADE
                    )
                    """);
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS purchase_seats (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        purchase_id INT NOT NULL,
                        seat_label VARCHAR(30) NOT NULL,
                        CONSTRAINT fk_purchase_seats_purchase
                            FOREIGN KEY (purchase_id) REFERENCES billet(id)
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
                            FOREIGN KEY (purchase_id) REFERENCES billet(id)
                            ON DELETE CASCADE
                    )
                    """);
        }
    }

    private static String buildTicketNumber(Connection conn, int ticketId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM billet WHERE id_representation = ?";
        int nextNumber = 1;
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, ticketId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    nextNumber = rs.getInt(1) + 1;
                }
            }
        }
        return "B-" + ticketId + "-" + String.format("%04d", nextNumber);
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

    private static String buildSalesPeriodJoinClause(String periodMode) {
        int days = resolveSalesPeriodDays(periodMode);
        if (days <= 0) {
            return "";
        }
        return " AND p.purchase_date >= NOW() - INTERVAL " + days + " DAY ";
    }

    private static String buildSalesTimelineWhereClause(String periodMode) {
        int days = resolveSalesPeriodDays(periodMode);
        if (days <= 0) {
            return "WHERE purchase_date >= DATE_SUB(CURDATE(), INTERVAL 11 MONTH) OR refunded_at >= DATE_SUB(CURDATE(), INTERVAL 11 MONTH)";
        }
        return "WHERE purchase_date >= NOW() - INTERVAL " + days + " DAY OR refunded_at >= NOW() - INTERVAL " + days + " DAY";
    }

    private static int resolveSalesPeriodDays(String periodMode) {
        return switch (periodMode) {
            case "7d" -> 7;
            case "30d" -> 30;
            default -> 0;
        };
    }

    private static boolean isAllPeriod(String periodMode) {
        return resolveSalesPeriodDays(periodMode) == 0;
    }

    private static Map<LocalDate, RevenueBucket> initDailyBuckets(int days) {
        LinkedHashMap<LocalDate, RevenueBucket> buckets = new LinkedHashMap<>();
        LocalDate start = LocalDate.now().minusDays(Math.max(0, days - 1L));
        for (int i = 0; i < days; i++) {
            buckets.put(start.plusDays(i), new RevenueBucket());
        }
        return buckets;
    }

    private static PurchaseOperationResult alignTicketStockWithSeatsTransactional(Connection conn, int ticketId) throws SQLException {
        String sql = """
                UPDATE representation r
                JOIN (
                    SELECT
                        ticket_id,
                        COUNT(*) - COALESCE(SUM(CASE WHEN is_taken = 1 THEN 1 ELSE 0 END), 0) AS available_seats
                    FROM seats
                    WHERE ticket_id = ?
                    GROUP BY ticket_id
                ) seat_stats ON seat_stats.ticket_id = r.id
                SET r.stock = seat_stats.available_seats
                WHERE r.id = ?
                """;

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, ticketId);
            pst.setInt(2, ticketId);
            int updated = pst.executeUpdate();
            if (updated == 0) {
                return PurchaseOperationResult.failure("Aucun plan de salle a aligner pour cet evenement.");
            }
            return PurchaseOperationResult.success("Stock aligné sur le nombre réel de sièges libres.");
        }
    }

    private static int getNextSeatNumberForRow(Connection conn, int ticketId, String rowLabel) throws SQLException {
        String sql = "SELECT COALESCE(MAX(seat_number), 0) FROM seats WHERE ticket_id = ? AND seat_row = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, ticketId);
            pst.setString(2, rowLabel);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() ? rs.getInt(1) + 1 : 1;
            }
        }
    }

    private static Map<YearMonth, RevenueBucket> initMonthlyBuckets(int months) {
        LinkedHashMap<YearMonth, RevenueBucket> buckets = new LinkedHashMap<>();
        YearMonth start = YearMonth.now().minusMonths(Math.max(0, months - 1L));
        for (int i = 0; i < months; i++) {
            buckets.put(start.plusMonths(i), new RevenueBucket());
        }
        return buckets;
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

    private static final class RevenueBucket {
        private BigDecimal confirmed = BigDecimal.ZERO;
        private BigDecimal refunded = BigDecimal.ZERO;
    }
}
