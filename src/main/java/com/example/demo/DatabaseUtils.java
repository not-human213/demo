package com.example.demo;

import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;


public class DatabaseUtils {
    public static DataSource getDataSource(String selectedDb) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();

        switch (selectedDb.toLowerCase()) {
            case "mysql":
                dataSource.setUrl("jdbc:mysql://localhost:3306/sqlexplorer");
                dataSource.setUsername("root");
                dataSource.setPassword("Harshal@sql");
                dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
                break;
            case "postgres":
                dataSource.setUrl("jdbc:postgresql://localhost:5432/yourdb");
                dataSource.setUsername("postgres");
                dataSource.setPassword("yourpassword");
                dataSource.setDriverClassName("org.postgresql.Driver");
                break;
            case "mariadb":
                dataSource.setUrl("jdbc:mariadb://localhost:3306/yourdb");
                dataSource.setUsername("root");
                dataSource.setPassword("yourpassword");
                dataSource.setDriverClassName("org.mariadb.jdbc.Driver");
                break;
            default:
                throw new IllegalArgumentException("Unsupported database type: " + selectedDb);
        }

        return dataSource;
    }

    public static List<String> getAllTables(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        try {
            // Query to get all tables (works for MySQL, PostgreSQL, and MariaDB)
            String query = "SHOW TABLES";
            List<Map<String, Object>> tables = jdbcTemplate.queryForList(query);
            return tables.stream()
                         .map(table -> table.values().iterator().next().toString())
                         .toList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch tables: " + e.getMessage());
        }
    }
}


