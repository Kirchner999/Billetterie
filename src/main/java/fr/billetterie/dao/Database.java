package fr.billetterie.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    private static final String URL = "jdbc:mysql://localhost:3306/billetterie?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "";

    private static Connection connection;

    public static Connection getConnection() {
        // Si déjà connectée → réutiliser
        try {
            if (connection != null && !connection.isClosed()) {
                return connection;
            }
        } catch (SQLException ignored) {}

        try {
            connection = DriverManager.getConnection(URL, USER, PASS);
            System.out.println("✅ Connexion MySQL établie !");
        } catch (SQLException e) {
            System.err.println("❌ Erreur de connexion MySQL : " + e.getMessage());
            connection = null;
        }

        return connection;
    }
}
