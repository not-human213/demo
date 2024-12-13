package com.example.demo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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






    @PostMapping("/api/test-connection")
    public String testDatabaseConnection() {
    try {
        jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        return "Database connection successful!";
    } catch (Exception e) {
        return "Database connection failed: " + e.getMessage();
    }
}
}
