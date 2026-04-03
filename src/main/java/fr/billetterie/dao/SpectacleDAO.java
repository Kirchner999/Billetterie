package fr.billetterie.dao;

import fr.billetterie.model.Spectacle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SpectacleDAO {

    private SpectacleDAO() {
    }

    private static Spectacle map(ResultSet rs) throws java.sql.SQLException {
        return new Spectacle(
                rs.getInt("id"),
                rs.getString("titre"),
                rs.getString("lieu"),
                rs.getString("affiche"),
                rs.getString("tags"),
                (Integer) rs.getObject("duree"),
                rs.getString("description_courte"),
                rs.getString("description_longue"),
                rs.getString("langue"),
                (Integer) rs.getObject("age_minimum"),
                rs.getString("photos")
        );
    }

    public static List<Spectacle> getAll() {
        List<Spectacle> list = new ArrayList<>();
        String sql = "SELECT * FROM spectacle ORDER BY titre ASC";

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

    public static Spectacle getById(int id) {
        String sql = "SELECT * FROM spectacle WHERE id = ?";

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

    public static void save(Spectacle spectacle) {
        String sql = """
            INSERT INTO spectacle (titre, lieu, affiche, tags, duree, description_courte, description_longue, langue, age_minimum, photos)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, spectacle.getTitre());
            stmt.setString(2, spectacle.getLieu());
            stmt.setString(3, spectacle.getAffiche());
            stmt.setString(4, spectacle.getTags());
            stmt.setObject(5, spectacle.getDuree());
            stmt.setString(6, spectacle.getDescriptionCourte());
            stmt.setString(7, spectacle.getDescriptionLongue());
            stmt.setString(8, spectacle.getLangue());
            stmt.setObject(9, spectacle.getAgeMinimum());
            stmt.setString(10, spectacle.getPhotos());
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                spectacle.setId(rs.getInt(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void update(Spectacle spectacle) {
        String sql = """
            UPDATE spectacle
            SET titre = ?, lieu = ?, affiche = ?, tags = ?, duree = ?, description_courte = ?,
                description_longue = ?, langue = ?, age_minimum = ?, photos = ?
            WHERE id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, spectacle.getTitre());
            stmt.setString(2, spectacle.getLieu());
            stmt.setString(3, spectacle.getAffiche());
            stmt.setString(4, spectacle.getTags());
            stmt.setObject(5, spectacle.getDuree());
            stmt.setString(6, spectacle.getDescriptionCourte());
            stmt.setString(7, spectacle.getDescriptionLongue());
            stmt.setString(8, spectacle.getLangue());
            stmt.setObject(9, spectacle.getAgeMinimum());
            stmt.setString(10, spectacle.getPhotos());
            stmt.setInt(11, spectacle.getId());
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void delete(int id) {
        String sql = "DELETE FROM spectacle WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
