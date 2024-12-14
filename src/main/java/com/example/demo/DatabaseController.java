package com.example.demo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;

import main.java.com.example.demo.DatabaseUtils;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.core.type.TypeReference;



@RestController
public class DatabaseController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/api/show_tables")
    public ResponseEntity<List<Map<String, Object>>> connectToDatabase() {
        // Example: Test if the database connection is working
        try {
            String query = "SHOW TABLES;";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);

            return ResponseEntity.ok(rows);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of(Map.of("error", "Failed to connect to database: " + e.getMessage())));
        }
    }


    @RequestMapping("/api/select")
    public ResponseEntity<List<Map<String, Object>>> selectFromTable(@RequestParam String tableName) {
        try {
            String query = "SELECT * FROM " + tableName + ";";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);

            return ResponseEntity.ok(rows);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of(Map.of("error", "Failed to connect to database: " + e.getMessage())));
        }
    }

    @PostMapping("/api/insert")
    public ResponseEntity<List<Map<String, Object>>> insertIntoTable(
        @RequestParam String tableName,
        @RequestParam String jsonValues) {  // Accept JSON as a string in form-data
    try {
        // Convert the JSON string to a map (or a list of maps depending on your needs)
        ObjectMapper objectMapper = new ObjectMapper();
        List<Object> values = objectMapper.readValue(jsonValues, List.class);

        // Use placeholders for the values
        String placeholders = String.join(",", values.stream().map(v -> "?").toArray(String[]::new));
        String query = "INSERT INTO " + tableName + " VALUES (" + placeholders + ")";

        // Execute the query
        jdbcTemplate.update(query, values.toArray());

        return ResponseEntity.ok(List.of(Map.of("message", "Successfully inserted into table " + tableName)));
    } catch (Exception e) {
        return ResponseEntity.status(500).body(List.of(Map.of("error", "Failed to insert into database: " + e.getMessage())));
    }
}


@PostMapping(value = "/api/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<List<Map<String, Object>>> updateTable(
    @RequestParam("tableName") String tableName,
    @RequestParam("jsonValues") String jsonValues,
    @RequestParam("id") int id
) {
    try {
        // Parse JSON values
        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, Object>> values = objectMapper.readValue(jsonValues, new TypeReference<List<Map<String, Object>>>() {});

        // Update table logic
        // Example: Construct placeholders and execute the update query
        StringBuilder updateQuery = new StringBuilder("UPDATE " + tableName + " SET ");
        values.forEach(entry -> 
            entry.forEach((key, value) -> updateQuery.append(key + " = ?, "))
        );
        updateQuery.delete(updateQuery.length() - 2, updateQuery.length()); // Remove last comma
        updateQuery.append(" WHERE id = ?");
        
        // Execute query
        List<Object> parameters = new ArrayList<>(values.get(0).values());
        parameters.add(id);
        jdbcTemplate.update(updateQuery.toString(), parameters.toArray());

        return ResponseEntity.ok(List.of(Map.of("message", "Successfully updated table " + tableName)));
    } catch (Exception e) {
        return ResponseEntity.status(500).body(List.of(Map.of("error", "Failed to update database: " + e.getMessage())));
    }
}


@PostMapping(value = "/api/delete")
    public ResponseEntity<List<Map<String, Object>>> deleteFromTable(
            @RequestParam("tableName") String tableName,
            @RequestParam("id") int id) {
        try {
            // Build SQL query to delete record
            String query = "DELETE FROM " + tableName + " WHERE id = ?";

            // Execute query
            int rowsAffected = jdbcTemplate.update(query, id);

            if (rowsAffected > 0) {
                return ResponseEntity.ok(List.of(Map.of("message", "Successfully deleted from table " + tableName)));
            } else {
                return ResponseEntity.status(404).body(List.of(Map.of("error", "Record with id " + id + " not found")));
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of(Map.of("error", "Failed to delete from database: " + e.getMessage())));
        }
    }



    @PostMapping("/api/test-connection")
    public String testDatabaseConnection() {
    try {
        jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        return "Database connection successful!";
    } catch (Exception e) {
        return "Database connection failed: " + e.getMessage();
    }
}

public ResponseEntity<List<Map<String, Object>>> searchDatabase(
            @RequestParam String selectedDb,
            @RequestParam String searchTerm) {
        try {
            // Get the correct DataSource based on selectedDb
            DataSource dataSource = DatabaseUtils.getDataSource(selectedDb);
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            // Fetch all table names
            List<String> tables = DatabaseUtils.getAllTables(dataSource);

            // For each table, build a query to search all columns for the search term
            List<Map<String, Object>> searchResults = tables.stream()
                    .flatMap(table -> searchInTable(table, searchTerm, jdbcTemplate).stream())
                    .collect(Collectors.toList());

            return ResponseEntity.ok(searchResults);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of(Map.of("error", "Failed to search database: " + e.getMessage())));
        }
    }

    private List<Map<String, Object>> searchInTable(String tableName, String searchTerm, JdbcTemplate jdbcTemplate) {
        try {
            // Get columns for the current table
            String getColumnsQuery = "DESCRIBE " + tableName;
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(getColumnsQuery);

            // Create a query that searches the entire table for the search term in all columns
            StringBuilder query = new StringBuilder("SELECT * FROM " + tableName + " WHERE ");
            columns.stream()
                    .map(column -> column.get("Field").toString())
                    .forEach(column -> query.append(column).append(" LIKE ? OR "));
            query.delete(query.length() - 3, query.length()); // Remove the last " OR "

            // Execute the query with the search term
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(query.toString(), "%" + searchTerm + "%");
            return rows;
        } catch (Exception e) {
            throw new RuntimeException("Failed to search in table " + tableName + ": " + e.getMessage());
        }
    }
}
