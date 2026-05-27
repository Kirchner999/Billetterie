package fr.billetterie.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Database {

    private static final Map<String, String> DOTENV = loadDotenv();
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
        if (value != null && !value.isBlank()) {
            return value;
        }

        value = DOTENV.get(key);
        return value == null ? defaultValue : value;
    }

    private static Map<String, String> loadDotenv() {
        Map<String, String> values = new HashMap<>();
        Path envPath = Path.of(".env");
        if (!Files.exists(envPath)) {
            return values;
        }

        try {
            for (String line : Files.readAllLines(envPath)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                int separator = trimmed.indexOf('=');
                if (separator <= 0) {
                    continue;
                }

                String key = trimmed.substring(0, separator).trim();
                String value = trimmed.substring(separator + 1).trim();
                values.put(key, unquote(value));
            }
        } catch (Exception e) {
            System.out.println("Impossible de lire le fichier .env");
            e.printStackTrace();
        }
        return values;
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
