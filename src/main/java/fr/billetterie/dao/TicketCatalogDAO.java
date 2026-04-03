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
                tickets.add(new Ticket(
                        rs.getInt("id"),
                        rs.getString("event_name"),
                        rs.getTimestamp("event_date").toLocalDateTime(),
                        rs.getBigDecimal("price"),
                        rs.getInt("stock")
                ));
            }
        } catch (Exception e) {
            System.out.println("Erreur getAvailableTickets()");
            e.printStackTrace();
        }

        return tickets;
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
                SELECT p.id, p.user_id, p.ticket_id, t.event_name, p.quantity, p.total, p.purchase_date
                FROM purchases p
                JOIN tickets t ON t.id = p.ticket_id
                WHERE p.user_id = ?
                ORDER BY p.purchase_date DESC
                """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                purchases.add(new Purchase(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getInt("ticket_id"),
                        rs.getString("event_name"),
                        rs.getInt("quantity"),
                        rs.getBigDecimal("total"),
                        rs.getTimestamp("purchase_date").toLocalDateTime()
                ));
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
        String purchaseSql = "INSERT INTO purchases (user_id, ticket_id, quantity, total, purchase_date) VALUES (?, ?, ?, ?, NOW())";

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
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
                try (PreparedStatement pst = conn.prepareStatement(purchaseSql, Statement.RETURN_GENERATED_KEYS)) {
                    pst.setInt(1, userId);
                    pst.setInt(2, ticketId);
                    pst.setInt(3, quantity);
                    pst.setBigDecimal(4, total);
                    pst.executeUpdate();
                }

                if (seatIds != null && !seatIds.isEmpty()) {
                    markSeatsTaken(conn, seatIds);
                }

                conn.commit();
                return PurchaseOperationResult.success("Achat confirme pour " + eventName + " (x" + quantity + ").");
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

    public static int cleanupExpiredTickets() {
        String expiredIdsSql = "SELECT id FROM tickets WHERE event_date < NOW()";
        String deletePurchasesSql = "DELETE FROM purchases WHERE ticket_id = ?";
        String deleteSeatsSql = "DELETE FROM seats WHERE ticket_id = ?";
        String deleteTicketSql = "DELETE FROM tickets WHERE id = ?";

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
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
