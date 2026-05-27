package fr.billetterie.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    private static final String URL = config("BILLETTERIE_DB_URL", "jdbc:mysql://localhost:3306/dispelltacle?useSSL=false&serverTimezone=UTC");
    private static final String USER = config("BILLETTERIE_DB_USER", "root");
    private static final String PASS = config("BILLETTERIE_DB_PASSWORD", "");

    private Database() {
    }

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (SQLException e) {
            throw new IllegalStateException("Connexion MySQL impossible: " + e.getMessage(), e);
        }
    }

    private static String config(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
