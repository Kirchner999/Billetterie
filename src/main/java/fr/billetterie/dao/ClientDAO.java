package fr.billetterie.dao;

import fr.billetterie.model.Client;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClientDAO {

    private static Client map(ResultSet rs) throws SQLException {
        return new Client(
                rs.getInt("id"),
                rs.getString("pseudo"),
                rs.getString("nom"),
                rs.getString("prenom"),
                rs.getString("numero"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getString("adresse"),
                rs.getBoolean("is_admin"),
                rs.getString("role")
        );
    }

    public static boolean register(Client c) {
        String sql = """
            INSERT INTO client (pseudo, nom, prenom, numero, email, password, adresse, is_admin, role)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, c.getPseudo());
            pst.setString(2, c.getNom());
            pst.setString(3, c.getPrenom());
            pst.setString(4, c.getNumero());
            pst.setString(5, c.getEmail());
            pst.setString(6, c.getPassword());
            pst.setString(7, c.getAdresse());
            pst.setBoolean(8, c.isAdmin());
            pst.setString(9, c.getRole());

            pst.executeUpdate();
            return true;
        } catch (Exception e) {
            System.out.println("❌ Erreur register()");
            e.printStackTrace();
            return false;
        }
    }

    public static Client authenticate(String email, String password) {
        String sql = "SELECT * FROM client WHERE email = ? AND password = ? LIMIT 1";

        try (Connection conn = Database.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, email);
            pst.setString(2, password);

            ResultSet rs = pst.executeQuery();
            if (rs.next()) return map(rs);

        } catch (Exception e) {
            System.out.println("❌ Erreur authenticate()");
            e.printStackTrace();
        }
        return null;
    }

    public static boolean emailExists(String email) {
        String sql = "SELECT id FROM client WHERE email = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, email);
            ResultSet rs = pst.executeQuery();
            return rs.next();

        } catch (Exception e) {
            System.out.println("❌ Erreur emailExists()");
            e.printStackTrace();
            return true;
        }
    }

    public static List<Client> getAll() {
        List<Client> list = new ArrayList<>();
        String sql = "SELECT * FROM client ORDER BY id DESC";

        try (Connection conn = Database.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) list.add(map(rs));

        } catch (Exception e) {
            System.out.println("❌ Erreur getAll()");
            e.printStackTrace();
        }
        return list;
    }
}
