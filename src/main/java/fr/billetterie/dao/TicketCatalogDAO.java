package fr.billetterie.dao;

import fr.billetterie.model.Purchase;
import fr.billetterie.model.Seat;
import fr.billetterie.model.Ticket;
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
                ensurePurchaseReceiptColumns(conn);
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
                SELECT p.id, p.user_id, p.ticket_id, t.event_name, p.quantity, p.total, p.purchase_date, p.ticket_number, p.pdf_path, p.seat_labels
                FROM purchases p
                JOIN tickets t ON t.id = p.ticket_id
                WHERE p.user_id = ?
                ORDER BY p.purchase_date DESC
                """;

        try (Connection conn = Database.getConnection()) {
            ensurePurchaseReceiptColumns(conn);
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
                                rs.getString("ticket_number"),
                                rs.getString("pdf_path"),
                                rs.getString("seat_labels")
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
        String purchaseSql = "INSERT INTO purchases (user_id, ticket_id, quantity, total, purchase_date, ticket_number, pdf_path, seat_labels) VALUES (?, ?, ?, ?, NOW(), NULL, NULL, NULL)";

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensurePurchaseReceiptColumns(conn);

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
        String sql = "UPDATE purchases SET ticket_number = ?, pdf_path = ?, seat_labels = ? WHERE id = ?";
        try (Connection conn = Database.getConnection()) {
            ensurePurchaseReceiptColumns(conn);
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setString(1, ticketNumber);
                pst.setString(2, pdfPath);
                pst.setString(3, seatLabels);
                pst.setInt(4, purchaseId);
                return pst.executeUpdate() > 0;
            }
        } catch (Exception e) {
            System.out.println("Erreur saveReceiptDocument()");
            e.printStackTrace();
            return false;
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
                ensurePurchaseReceiptColumns(conn);
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

    private static Ticket mapTicket(ResultSet rs) throws SQLException {
        return new Ticket(
                rs.getInt("id"),
                rs.getString("event_name"),
                rs.getTimestamp("event_date").toLocalDateTime(),
                rs.getBigDecimal("price"),
                rs.getInt("stock")
        );
    }

    private static void ensurePurchaseReceiptColumns(Connection conn) throws SQLException {
        if (!columnExists(conn, "purchases", "ticket_number")) {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("ALTER TABLE purchases ADD COLUMN ticket_number VARCHAR(120) NULL");
            }
        }
        if (!columnExists(conn, "purchases", "pdf_path")) {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("ALTER TABLE purchases ADD COLUMN pdf_path VARCHAR(500) NULL");
            }
        }
        if (!columnExists(conn, "purchases", "seat_labels")) {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("ALTER TABLE purchases ADD COLUMN seat_labels VARCHAR(255) NULL");
            }
        }
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
}
