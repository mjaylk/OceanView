package com.oceanview.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    // singleton instance
    private static DatabaseConnection instance;

    // database config
    private static final String USER = "root";
    private static final String PASSWORD = "";
    private static final String URL =
        "jdbc:mysql://localhost:3306/ocean_view_resort?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    // oop encapsulation
    private DatabaseConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    // singleton access
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    // service method
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // test cases
    public static void main(String[] args) {

        // test case 01 instance check
        DatabaseConnection db1 = DatabaseConnection.getInstance();
        DatabaseConnection db2 = DatabaseConnection.getInstance();
        System.out.println("Test 01 Singleton check: " + (db1 == db2));

        // test case 02 connection test
        try {
            Connection con = db1.getConnection();
            System.out.println("Test 02 DB connection: SUCCESS");
            con.close();
        } catch (SQLException e) {
            System.out.println("Test 02 DB connection: FAILED");
        }

        // test case 03 close connection test
        try {
            Connection con = db1.getConnection();
            con.close();
            System.out.println("Test 03 Connection close: SUCCESS");
        } catch (SQLException e) {
            System.out.println("Test 03 Connection close: FAILED");
        }
    }
}
