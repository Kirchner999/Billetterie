package fr.billetterie.dao;

import fr.billetterie.model.Spectacle;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SpectacleDAO {

    // Mappe une ligne SQL -> objet Spectacle
    private static Spectacle map(ResultSet rs) throws SQLException {
        return new Spectacle(
                rs.getInt("id"),
                rs.getString("titre"),
                rs.getString("description"),
                rs.getString("lieu")
        );
    }

    // -----------------------------------------
    // GET ALL
    // -----------------------------------------
    public static List<Spectacle> getAll() {
        List<Spectacle> list = new ArrayList<>();
        String sql = "SELECT * FROM Spectacle ORDER BY titre ASC";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) list.add(map(rs));

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    // -----------------------------------------
    // GET BY ID
    // -----------------------------------------
    public static Spectacle getById(int id) {
        String sql = "SELECT * FROM Spectacle WHERE id = ?";
        Spectacle s = null;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                s = map(rs);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return s;
    }

    // -----------------------------------------
    // INSERT
    // -----------------------------------------
    public static void save(Spectacle s) {
        String sql = """
            INSERT INTO Spectacle (titre, description, lieu)
            VALUES (?, ?, ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, s.getTitre());
            stmt.setString(2, s.getDescription());
            stmt.setString(3, s.getLieu());
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) s.setId(rs.getInt(1));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------
    public static void update(Spectacle s) {
        String sql = """
            UPDATE Spectacle
            SET titre = ?, description = ?, lieu = ?
            WHERE id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, s.getTitre());
            stmt.setString(2, s.getDescription());
            stmt.setString(3, s.getLieu());
            stmt.setInt(4, s.getId());
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // -----------------------------------------
    // DELETE
    // -----------------------------------------
    public static void delete(int id) {
        String sql = "DELETE FROM Spectacle WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
