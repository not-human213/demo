package com.example.demo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.bind.annotation.*;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.jdbc.core.JdbcTemplate;


public class DatabaseUtils {
    private static final Map<String, DataSource> dataSourceMap = new ConcurrentHashMap<>();

    /**
     * Retrieves a DataSource based on dbType.
     *
     * @param dbType The type of the database (e.g., mysql, postgres, mariadb).
     * @return The corresponding DataSource or null if not found.
     */
    public static DataSource getDataSource(String dbType) {
        String key = dbType.toLowerCase();
        return dataSourceMap.get(key);
    }

    /**
     * Adds a new DataSource to the dataSourceMap.
     *
     * @param dbType     The type of the database.
     * @param dataSource The DataSource to be added.
     */
    public static void addDataSource(String dbType, DataSource dataSource) {
        String key = dbType.toLowerCase();
        dataSourceMap.putIfAbsent(key, dataSource);
    }

    /**
     * Removes a DataSource from the dataSourceMap.
     *
     * @param dbType The type of the database.
     * @return true if the DataSource was removed successfully, false otherwise.
     */
    public static boolean removeDataSource(String dbType) {
        String key = dbType.toLowerCase();
        return dataSourceMap.remove(key) != null;
    }

    /**
     * Retrieves all existing database connections.
     *
     * @return A list of ConnectionInfo objects representing each connection.
     */
    public static List<ConnectionInfo> getAllConnections() {
        List<ConnectionInfo> connections = new ArrayList<>();
        for (String key : dataSourceMap.keySet()) {
            connections.add(new ConnectionInfo(key));
        }
        return connections;
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
    public static class DatabaseConnectionRequest {
        private String dbType;     // Type of the database (mysql, postgresql, mariadb)
        private String host;       // Database host (e.g., localhost)
        private Integer port;      // Database port (default: 3306 for MySQL, 5432 for PostgreSQL)
        private String username;   // Database username
        private String password;   // Database password
        private String databaseName; // Database name to connect to
    
        public String getDbType() {
            return dbType;
        }

        public void setDbType(String dbType) {
            this.dbType = dbType;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDatabaseName() {
            return databaseName;
        }

        public void setDatabaseName(String databaseName) {
            this.databaseName = databaseName;
        }
    }

    public class DatabaseConnectionService {

    public boolean testConnection(DatabaseConnectionRequest request) {
        String url = buildDatabaseUrl(request);

        try (Connection connection = DriverManager.getConnection(url, request.getUsername(), request.getPassword())) {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    private String buildDatabaseUrl(DatabaseConnectionRequest request) {
        validateRequest(request);
    
        if ("mysql".equalsIgnoreCase(request.getDbType()) || "mariadb".equalsIgnoreCase(request.getDbType())) {
            return "jdbc:" + request.getDbType() + "://"
                    + request.getHost() + ":" + request.getPort() + "/"
                    + request.getDatabaseName();
        } else if ("postgresql".equalsIgnoreCase(request.getDbType())) {
            return "jdbc:postgresql://"
                    + request.getHost() + ":" + request.getPort() + "/"
                    + request.getDatabaseName();
        } else {
            throw new IllegalArgumentException("Unsupported database type: " + request.getDbType());
        }
    }
    
    private void validateRequest(DatabaseConnectionRequest request) {
        if (request.getDbType() == null || request.getDbType().isEmpty()) {
            throw new IllegalArgumentException("Database type is required.");
        }
        if (request.getHost() == null || request.getHost().isEmpty()) {
            throw new IllegalArgumentException("Host is required.");
        }
        if (request.getPort() == null || request.getPort() <= 0) {
            throw new IllegalArgumentException("Valid port is required.");
        }
        if (request.getDatabaseName() == null || request.getDatabaseName().isEmpty()) {
            throw new IllegalArgumentException("Database name is required.");
        }
    }
    
    
    
}
}