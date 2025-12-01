package fr.billetterie.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    private static final String URL = "jdbc:mysql://localhost:3306/billetterie?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "";

    public static Connection getConnection() {
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASS);
            System.out.println("✔ Connexion MySQL établie !");
            return conn;
        } catch (SQLException e) {
            System.out.println("❌ Erreur MySQL : " + e.getMessage());
            return null;
        }
    }
}
