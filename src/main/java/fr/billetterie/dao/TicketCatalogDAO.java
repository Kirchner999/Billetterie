package fr.billetterie.dao;

import fr.billetterie.model.Purchase;
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
        String sql = "SELECT id, event_name, event_date, price, stock FROM tickets ORDER BY event_date ASC";

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

    public static PurchaseOperationResult purchaseTicket(int userId, int ticketId, int quantity) {
        String ticketSql = "SELECT event_name, price, stock FROM tickets WHERE id = ? FOR UPDATE";
        String seatsSql = "SELECT id FROM seats WHERE ticket_id = ? AND is_taken = 0 ORDER BY seat_row, seat_number LIMIT ?";
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

                if (stock < quantity) {
                    conn.rollback();
                    return PurchaseOperationResult.failure("Stock insuffisant pour cet evenement.");
                }

                List<Integer> seatIds = getAvailableSeatIds(conn, ticketId, quantity);
                int totalSeatCount = countSeatsForTicket(conn, ticketId);
                if (totalSeatCount > 0 && seatIds.size() < quantity) {
                    conn.rollback();
                    return PurchaseOperationResult.failure("Pas assez de sieges libres pour cet evenement.");
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

                if (!seatIds.isEmpty()) {
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

    public static int countTickets() {
        return count("SELECT COUNT(*) FROM tickets");
    }

    public static int countPurchases() {
        return count("SELECT COUNT(*) FROM purchases");
    }

    private static List<Integer> getAvailableSeatIds(Connection conn, int ticketId, int quantity) throws SQLException {
        List<Integer> seatIds = new ArrayList<>();
        String sql = "SELECT id FROM seats WHERE ticket_id = ? AND is_taken = 0 ORDER BY seat_row, seat_number LIMIT ?";

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, ticketId);
            pst.setInt(2, quantity);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    seatIds.add(rs.getInt("id"));
                }
            }
        }

        return seatIds;
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
