// package com.example.demo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    // Database URL, username, password
    private static final String DB_URL = "jdbc:mysql://localhost:3306/sqlexplorer"; // Replace with your DB URL
    private static final String USER = "root"; // Replace with your DB username
    private static final String PASSWORD = "Harshal@sql"; // Replace with your DB password

    public static void main(String[] args) {
        // Establish the database connection
        try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            System.out.println("Connected to the database successfully!");

            // Perform a simple query or operation
            String query = "SELECT * FROM ttry"; // Replace with your query
            try (Statement statement = connection.createStatement()) {
                statement.executeQuery(query);
                System.out.println("Query executed successfully!");
            }

        } catch (SQLException e) {
            System.out.println("An error occurred while connecting to the database.");
            e.printStackTrace();
        }
    }
}
