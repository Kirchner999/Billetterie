package fr.billetterie.dao;

import fr.billetterie.model.Representation;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RepresentationDAO {

    // Map SQL -> Objet Representation
    private static Representation map(ResultSet rs) throws SQLException {
        return new Representation(
                rs.getInt("id"),
                rs.getInt("id_spectacle"),
                rs.getTimestamp("date_heure").toLocalDateTime(),
                rs.getDouble("prix"),
                rs.getInt("capacite")
        );
    }

    // ----------------------------------------------------
    // GET ALL
    // ----------------------------------------------------
    public static List<Representation> getAll() {
        List<Representation> list = new ArrayList<>();
        String sql = "SELECT * FROM Representation ORDER BY date_heure ASC";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) list.add(map(rs));

        } catch (SQLException e) { e.printStackTrace(); }

        return list;
    }

    // ----------------------------------------------------
    // GET BY ID
    // ----------------------------------------------------
    public static Representation getById(int id) {
        String sql = "SELECT * FROM Representation WHERE id = ?";
        Representation r = null;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) r = map(rs);

        } catch (SQLException e) { e.printStackTrace(); }

        return r;
    }

    // ----------------------------------------------------
    // GET BY SPECTACLE
    // ----------------------------------------------------
    public static List<Representation> getBySpectacle(int idSpectacle) {
        List<Representation> list = new ArrayList<>();
        String sql = "SELECT * FROM Representation WHERE id_spectacle = ? ORDER BY date_heure ASC";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idSpectacle);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) list.add(map(rs));

        } catch (SQLException e) { e.printStackTrace(); }

        return list;
    }

    // ----------------------------------------------------
    // INSERT
    // ----------------------------------------------------
    public static void save(Representation r) {
        String sql = """
            INSERT INTO Representation (id_spectacle, date_heure, prix, capacite)
            VALUES (?, ?, ?, ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, r.getIdSpectacle());
            stmt.setTimestamp(2, Timestamp.valueOf(r.getDateHeure()));
            stmt.setDouble(3, r.getPrix());
            stmt.setInt(4, r.getCapacite());

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) r.setId(rs.getInt(1));

        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ----------------------------------------------------
    // UPDATE
    // ----------------------------------------------------
    public static void update(Representation r) {
        String sql = """
            UPDATE Representation
            SET id_spectacle = ?, date_heure = ?, prix = ?, capacite = ?
            WHERE id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, r.getIdSpectacle());
            stmt.setTimestamp(2, Timestamp.valueOf(r.getDateHeure()));
            stmt.setDouble(3, r.getPrix());
            stmt.setInt(4, r.getCapacite());
            stmt.setInt(5, r.getId());

            stmt.executeUpdate();

        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ----------------------------------------------------
    // DELETE
    // ----------------------------------------------------
    public static void delete(int id) {
        String sql = "DELETE FROM Representation WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) { e.printStackTrace(); }
    }
}
