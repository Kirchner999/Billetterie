package fr.billetterie.dao;

import fr.billetterie.model.Representation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class RepresentationDAO {

    private RepresentationDAO() {
    }

    private static Representation map(ResultSet rs) throws java.sql.SQLException {
        return new Representation(
                rs.getInt("id"),
                rs.getInt("id_spectacle"),
                rs.getTimestamp("date_heure").toLocalDateTime(),
                rs.getString("salle")
        );
    }

    public static List<Representation> getAll() {
        List<Representation> list = new ArrayList<>();
        String sql = "SELECT * FROM representation ORDER BY date_heure ASC";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public static Representation getById(int id) {
        String sql = "SELECT * FROM representation WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return map(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static List<Representation> getBySpectacle(int idSpectacle) {
        List<Representation> list = new ArrayList<>();
        String sql = "SELECT * FROM representation WHERE id_spectacle = ? ORDER BY date_heure ASC";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idSpectacle);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public static void save(Representation representation) {
        String sql = """
            INSERT INTO representation (id_spectacle, date_heure, salle)
            VALUES (?, ?, ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, representation.getIdSpectacle());
            stmt.setTimestamp(2, Timestamp.valueOf(representation.getDateHeure()));
            stmt.setString(3, representation.getSalle());
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                representation.setId(rs.getInt(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void update(Representation representation) {
        String sql = """
            UPDATE representation
            SET id_spectacle = ?, date_heure = ?, salle = ?
            WHERE id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, representation.getIdSpectacle());
            stmt.setTimestamp(2, Timestamp.valueOf(representation.getDateHeure()));
            stmt.setString(3, representation.getSalle());
            stmt.setInt(4, representation.getId());
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void delete(int id) {
        String sql = "DELETE FROM representation WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
