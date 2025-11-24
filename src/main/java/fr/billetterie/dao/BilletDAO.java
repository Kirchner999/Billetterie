package fr.billetterie.dao;

import fr.billetterie.model.Billet;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BilletDAO {

    // Convertit un ResultSet en objet Billet
    private static Billet map(ResultSet rs) throws SQLException {
        return new Billet(
                rs.getInt("id"),
                rs.getString("numero"),
                rs.getInt("id_representation"),
                rs.getInt("id_client"),
                rs.getString("statut"),
                rs.getTimestamp("date_achat").toLocalDateTime()
        );
    }

    // -----------------------------------------
    // GET ALL
    // -----------------------------------------
    public static List<Billet> getAll() {
        List<Billet> billets = new ArrayList<>();
        String sql = "SELECT * FROM Billet ORDER BY date_achat DESC";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                billets.add(map(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return billets;
    }

    // -----------------------------------------
    // GET BY ID
    // -----------------------------------------
    public static Billet getById(int id) {
        String sql = "SELECT * FROM Billet WHERE id = ?";
        Billet billet = null;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                billet = map(rs);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return billet;
    }

    // -----------------------------------------
    // GET BY CLIENT
    // -----------------------------------------
    public static List<Billet> getByClient(int idClient) {
        List<Billet> billets = new ArrayList<>();
        String sql = """
            SELECT b.*
            FROM Billet b
            WHERE b.id_client = ?
            ORDER BY b.date_achat DESC
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idClient);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                billets.add(map(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return billets;
    }

    // -----------------------------------------
    // SAVE (INSERT)
    // -----------------------------------------
    public static void save(Billet billet) {
        String sql = """
            INSERT INTO Billet (numero, id_representation, id_client, statut, date_achat)
            VALUES (?, ?, ?, ?, NOW())
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, billet.getNumero());
            stmt.setInt(2, billet.getIdRepresentation());
            stmt.setInt(3, billet.getIdClient());
            stmt.setString(4, billet.getStatut());
            stmt.executeUpdate();

            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                billet.setId(keys.getInt(1));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // -----------------------------------------
    // UPDATE STATUT
    // -----------------------------------------
    public static void updateStatut(int id, String statut) {
        String sql = "UPDATE Billet SET statut = ? WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, statut);
            stmt.setInt(2, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // -----------------------------------------
    // DELETE
    // -----------------------------------------
    public static void delete(int id) {
        String sql = "DELETE FROM Billet WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
