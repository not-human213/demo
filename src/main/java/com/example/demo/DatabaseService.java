// package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DatabaseService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DatabaseService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String connectToDatabase() {
        try {
            // Example: Test if the database connection is working
            String query = "SELECT 1";
            jdbcTemplate.queryForObject(query, Integer.class);
            return "Connected to database successfully";
        } catch (Exception e) {
            return "Failed to connect to database: " + e.getMessage();
        }
    }
}
