package fr.billetterie.dao;

import fr.billetterie.model.Client;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.List;

public class ClientDAO {

    private ClientDAO() {
    }

    private static Client map(ResultSet rs) throws java.sql.SQLException {
        Timestamp createdAt = rs.getTimestamp("date_creation");
        return new Client(
                rs.getInt("id"),
                rs.getString("pseudo"),
                rs.getString("nom"),
                rs.getString("prenom"),
                rs.getString("numero"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getString("adresse"),
                rs.getString("role"),
                rs.getBoolean("is_admin"),
                createdAt != null ? createdAt.toLocalDateTime() : null
        );
    }

    public static boolean register(Client client) {
        String sql = """
            INSERT INTO client (pseudo, nom, prenom, numero, email, password, adresse, role, is_admin)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, client.getPseudo());
            pst.setString(2, valueOrEmpty(client.getNom()));
            pst.setString(3, valueOrEmpty(client.getPrenom()));
            pst.setString(4, nullable(client.getNumero()));
            pst.setString(5, client.getEmail());
            pst.setString(6, hashPassword(client.getPassword()));
            pst.setString(7, nullable(client.getAdresse()));
            pst.setString(8, normalizeRole(client.getRole()));
            pst.setBoolean(9, client.isAdmin());
            pst.executeUpdate();
            return true;
        } catch (Exception e) {
            System.out.println("Erreur register()");
            e.printStackTrace();
            return false;
        }
    }

    public static Client authenticate(String usernameOrEmail, String password) {
        String sql = """
            SELECT *
            FROM client
            WHERE pseudo = ? OR email = ?
            LIMIT 1
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, usernameOrEmail);
            pst.setString(2, usernameOrEmail);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                if (passwordMatches(password, rs.getString("password"))) {
                    return map(rs);
                }
            }
        } catch (Exception e) {
            System.out.println("Erreur authenticate()");
            e.printStackTrace();
        }
        return null;
    }

    public static boolean usernameExists(String username) {
        String sql = "SELECT id FROM client WHERE pseudo = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, username);
            ResultSet rs = pst.executeQuery();
            return rs.next();
        } catch (Exception e) {
            System.out.println("Erreur usernameExists()");
            e.printStackTrace();
            return false;
        }
    }

    public static List<Client> getAll() {
        List<Client> list = new ArrayList<>();
        String sql = "SELECT * FROM client ORDER BY id DESC";

        try (Connection conn = Database.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (Exception e) {
            System.out.println("Erreur getAll()");
            e.printStackTrace();
        }
        return list;
    }

    public static boolean updateProfile(Client client) {
        String sql = """
            UPDATE client
            SET pseudo = ?, nom = ?, prenom = ?, numero = ?, email = ?, password = ?, adresse = ?
            WHERE id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, valueOrEmpty(client.getPseudo()));
            pst.setString(2, valueOrEmpty(client.getNom()));
            pst.setString(3, valueOrEmpty(client.getPrenom()));
            pst.setString(4, nullable(client.getNumero()));
            pst.setString(5, valueOrEmpty(client.getEmail()));
            pst.setString(6, hashPassword(client.getPassword()));
            pst.setString(7, nullable(client.getAdresse()));
            pst.setInt(8, client.getId());
            return pst.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Erreur updateProfile()");
            e.printStackTrace();
            return false;
        }
    }

    private static String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "CLIENT";
        }

        String normalized = role.trim().toUpperCase();
        return switch (normalized) {
            case "ADMIN", "EDITEUR", "CLIENT" -> normalized;
            case "USER" -> "CLIENT";
            default -> "CLIENT";
        };
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String nullable(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private static String hashPassword(String password) {
        String normalizedPassword = valueOrEmpty(password);
        if (normalizedPassword.startsWith("sha256:")) {
            return normalizedPassword;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalizedPassword.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Hash du mot de passe impossible", e);
        }
    }

    private static boolean passwordMatches(String candidatePassword, String storedPassword) {
        String normalizedCandidate = valueOrEmpty(candidatePassword);
        String normalizedStored = valueOrEmpty(storedPassword);
        if (normalizedStored.startsWith("sha256:")) {
            return hashPassword(normalizedCandidate).equals(normalizedStored);
        }
        return normalizedCandidate.equals(normalizedStored);
    }
}
