package fr.billetterie.dao;

import fr.billetterie.model.Client;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClientDAO {

    private static Client map(ResultSet rs) throws SQLException {
        return new Client(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("email"),
                rs.getString("telephone")
                rs.getString("role")
        );
    }

    // -----------------------------------------
    // GET ALL
    // -----------------------------------------
    public static List<Client> getAll() {
        List<Client> list = new ArrayList<>();
        String sql = "SELECT * FROM Client ORDER BY nom ASC";

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
    public static Client getById(int id) {
        String sql = "SELECT * FROM Client WHERE id = ?";
        Client client = null;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) client = map(rs);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return client;
    }

    // -----------------------------------------
    // SAVE (INSERT)
    // -----------------------------------------
    public static void save(Client c) {
        String sql = """
            INSERT INTO Client (nom, email, telephone)
            VALUES (?, ?, ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, c.getNom());
            stmt.setString(2, c.getEmail());
            stmt.setString(3, c.getTelephone());
            stmt.setString(4, c.getRole());
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) c.setId(rs.getInt(1));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // -----------------------------------------
    // UPDATE
    // -----------------------------------------
    public static void update(Client c) {
        String sql = """
            UPDATE Client
            SET nom = ?, email = ?, telephone = ?
            WHERE id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, c.getNom());
            stmt.setString(2, c.getEmail());
            stmt.setString(3, c.getTelephone());
            stmt.setInt(4, c.getId());
            stmt.setInt(5, c.getId());
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // -----------------------------------------
    // DELETE
    // -----------------------------------------
    public static void delete(int id) {
        String sql = "DELETE FROM Client WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // -----------------------------------------
    // LOGIN / AUTHENTICATE
    // -----------------------------------------
    public static Client authenticate(String email, String telephone) {
    String sql = """
        SELECT * FROM Client
        WHERE email = ? AND telephone = ?
        LIMIT 1
    """;

    try (Connection conn = Database.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

        stmt.setString(1, email);
        stmt.setString(2, telephone);

        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return map(rs); // récupère le rôle aussi !
        }
    } 
    catch (SQLException e) { e.printStackTrace(); }

    return null;
}


    // -----------------------------------------
    // EMAIL EXISTS (pour l'inscription)
    // -----------------------------------------
    public static boolean emailExists(String email) {
        String sql = "SELECT id FROM Client WHERE email = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            return rs.next();

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


public static void register(Client c) {
    String sql = """
        INSERT INTO Client (nom, email, password, role)
        VALUES (?, ?, ?, ?)
    """;

    try (Connection conn = Database.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

        stmt.setString(1, c.getNom());
        stmt.setString(2, c.getEmail());
        stmt.setString(3, c.getPassword());
        stmt.setString(4, c.getRole()); // --> "CLIENT" par défaut

        stmt.executeUpdate();

        ResultSet rs = stmt.getGeneratedKeys();
        if (rs.next()) c.setId(rs.getInt(1));

    } catch (SQLException e) { e.printStackTrace(); }
}


}
