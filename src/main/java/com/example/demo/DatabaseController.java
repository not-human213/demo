package com.example.demo;
import com.example.demo.DatabaseUtils.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.example.demo.SearchRequest;
import javax.sql.DataSource;
import java.util.stream.Collectors;
import java.sql.SQLException;

@RestController
public class DatabaseController {


    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ApplicationContext applicationContext;

    private final DatabaseUtils databaseUtils = new DatabaseUtils();
    private final DatabaseConnectionService connectionService = databaseUtils.new DatabaseConnectionService();

    @PostMapping("/api/connect")
    public String connectToDatabase(@RequestBody DatabaseConnectionRequest request) {
        try {
            boolean isConnected = connectionService.testConnection(request);
            if (isConnected) {
                return "Connection to the " + request.getDbType() + " database successful!";
            } else {
                return "Failed to connect to the " + request.getDbType() + " database.";
            }
            
        } catch (Exception e) {
            return "Error connecting to database: " + e.getMessage();
        }
    }


        @PostMapping("/api/display_view")
    public ResponseEntity<List<Map<String, Object>>> displayView(
        @RequestParam String dbType,
        @RequestParam String database,
        @RequestParam String viewName) {
        try {
            DataSource dataSource = DatabaseUtils.getDataSource(dbType, database);
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    
            String query = "SELECT * FROM " + viewName;
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);
            return ResponseEntity.ok(rows);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonList(
                Map.of("error", "Failed to display view " + viewName + ": " + e.getMessage())));
        }
    }


    @PostMapping("/api/show_views")
    public ResponseEntity<List<Map<String, Object>>> showViews(
        @RequestParam String dbType,
        @RequestParam String database) {
        try {
            DataSource dataSource = DatabaseUtils.getDataSource(dbType, database);
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    
            String query;
            if (dbType.equalsIgnoreCase("postgres")) {
                query = "SELECT table_name FROM information_schema.views WHERE table_schema = 'public'";
            } else {
                query = "SHOW FULL TABLES IN " + database + " WHERE TABLE_TYPE LIKE 'VIEW'";
            }
    
            List<Map<String, Object>> views = jdbcTemplate.queryForList(query);
            return ResponseEntity.ok(views);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonList(
                Map.of("error", "Failed to show views: " + e.getMessage())));
        }
    }

    @PostMapping("/api/show_tables")
public ResponseEntity<List<Map<String, Object>>> showTables(
    @RequestParam String dbType,
    @RequestParam String database) {
    try {
        DataSource dataSource = DatabaseUtils.getDataSource(dbType, database);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        String query = "SHOW TABLES";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);
        return ResponseEntity.ok(rows);
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Collections.singletonList(Map.of("error", "Failed to show tables: " + e.getMessage())));
    }
}

    @RequestMapping("/api/select")
    public ResponseEntity<List<Map<String, Object>>> selectFromTable(@RequestParam String tableName, @RequestParam String dbType, @RequestParam String database) {
        try {
            switch (dbType.toLowerCase()) {
                case "mysql":
                    jdbcTemplate = mysqlJdbcTemplate;
                    break;
                case "postgres":
                    jdbcTemplate = postgresJdbcTemplate;
                    break;
                case "mariadb":
                    jdbcTemplate = mariadbJdbcTemplate;
                    break;
                default:
                    return ResponseEntity.status(400).body(Collections.singletonList(Map.of("error", "Unsupported database type: " + dbType)));
            }

            String query = "SELECT * FROM " + tableName + ";";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);
            return ResponseEntity.ok(rows);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonList(Map.of("error", "Failed to query table " + tableName + ": " + e.getMessage())));
        }
    }




    @PostMapping("/api/test-connection")
    public String testDatabaseConnection(@RequestParam String dbType) {
        try {
            switch (dbType.toLowerCase()) {
                case "mysql":
                    jdbcTemplate = mysqlJdbcTemplate;
                    break;
                case "postgres":
                    jdbcTemplate = postgresJdbcTemplate;
                    break;
                case "mariadb":
                    jdbcTemplate = mariadbJdbcTemplate;
                    break;
                default:
                    return "Unsupported database type: " + dbType;
            }

            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "Database connection successful!";
        } catch (Exception e) {
            return "Database connection failed: " + e.getMessage();
        }
    }

    
    @Autowired
    @Qualifier("mysqlJdbcTemplate")
    private JdbcTemplate mysqlJdbcTemplate;

    @Autowired
    @Qualifier("postgresJdbcTemplate")
    private JdbcTemplate postgresJdbcTemplate;

    @Autowired
    @Qualifier("mariadbJdbcTemplate")
    private JdbcTemplate mariadbJdbcTemplate;

    @PostMapping("/api/search")
    public ResponseEntity<Map<String, Object>> search(@RequestBody SearchRequest request) {
        try {
            String searchTerm = request.getSearchTerm();
            List<String> dbTypes = (request.getDbTypes() != null && !request.getDbTypes().isEmpty())
                    ? request.getDbTypes()
                    : Arrays.asList("mysql", "postgres", "mariadb");
            List<String> databases = request.getDatabases();

            List<Map<String, Object>> results = new ArrayList<>();

            for (String dbType : dbTypes) {
                List<String> dbList = (databases != null && !databases.isEmpty())
                        ? databases
                        : getAllDatabases(dbType);

                for (String database : dbList) {
                    JdbcTemplate jdbcTemplate = getJdbcTemplate(dbType, database);
                    results.addAll(searchInDatabase(jdbcTemplate, searchTerm, dbType, database));
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("results", results);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Collections.singletonMap("error", "Failed to perform search: " + e.getMessage()));
        }
    }

    private JdbcTemplate getJdbcTemplate(String dbType, String database) {
        DataSource dataSource = DatabaseUtils.getDataSource(dbType, database);
        return new JdbcTemplate(dataSource);
    }

    private List<String> getAllDatabases(String dbType) {
        try {
            JdbcTemplate jdbcTemplate;
            switch (dbType.toLowerCase()) {
                case "mysql":
                    jdbcTemplate = mysqlJdbcTemplate;
                    break;
                case "postgres":
                    jdbcTemplate = postgresJdbcTemplate;
                    break;
                case "mariadb":
                    jdbcTemplate = mariadbJdbcTemplate;
                    break;
                default:
                    return Collections.emptyList();
            }

            String query;
            if (dbType.equalsIgnoreCase("postgres")) {
                query = "SELECT datname FROM pg_database WHERE datistemplate = false";
                return jdbcTemplate.queryForList(query, String.class);
            } else {
                query = "SHOW DATABASES";
                List<String> databases = jdbcTemplate.queryForList(query, String.class);
                databases.removeIf(db -> db.equalsIgnoreCase("information_schema") ||
                                         db.equalsIgnoreCase("mysql") ||
                                         db.equalsIgnoreCase("performance_schema"));
                return databases;
            }

        } catch (Exception e) {
            System.err.println("Failed to retrieve databases for " + dbType + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

        private List<Map<String, Object>> searchInDatabase(JdbcTemplate jdbcTemplate, String searchTerm, String dbType, String database) {
        List<Map<String, Object>> results = new ArrayList<>();
    
        List<String> objectsToSearch = getAllTablesAndViews(jdbcTemplate, database, dbType);
    
        for (String objectName : objectsToSearch) {
            List<String> columns = getColumnNames(jdbcTemplate, objectName, database);
            if (columns.isEmpty()) {
                continue;
            }
    
            String conditions = columns.stream()
                .map(column -> column + " LIKE ?")
                .collect(Collectors.joining(" OR "));
    
            String query = "SELECT * FROM " + objectName + " WHERE " + conditions;
            Object[] params = new Object[columns.size()];
            Arrays.fill(params, "%" + searchTerm + "%");
    
            try {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(query, params);
                for (Map<String, Object> row : rows) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("dbType", dbType);
                    result.put("database", database);
                    result.put("object", objectName);
                    result.put("row", row);
                    results.add(result);
                }
            } catch (Exception e) {
                System.err.println("Failed to search in " + objectName + ": " + e.getMessage());
            }
        }
    
        return results;
    }

    private List<String> getAllTablesAndViews(JdbcTemplate jdbcTemplate, String database, String dbType) {
        String query;
        if (dbType.equalsIgnoreCase("postgres")) {
            query = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND (table_type = 'BASE TABLE' OR table_type = 'VIEW')";
            return jdbcTemplate.queryForList(query, String.class);
        } else {
            query = "SHOW FULL TABLES IN " + database + " WHERE TABLE_TYPE LIKE '%TABLE%' OR TABLE_TYPE LIKE '%VIEW%'";
            List<Map<String, Object>> results = jdbcTemplate.queryForList(query);
            List<String> tablesAndViews = new ArrayList<>();
            for (Map<String, Object> row : results) {
                for (Object value : row.values()) {
                    if (value != null) {
                        tablesAndViews.add(value.toString());
                        break;
                    }
                }
            }
            return tablesAndViews;
        }
    }

        private List<String> getColumnNames(JdbcTemplate jdbcTemplate, String objectName, String database) {
        String query = "SELECT column_name FROM information_schema.columns WHERE table_name = ? AND table_schema = ?";
        try {
            return jdbcTemplate.queryForList(query, String.class, objectName, database);
        } catch (Exception e) {
            System.err.println("Failed to retrieve columns from " + objectName + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }



@PostMapping("/api/get_schema")
public ResponseEntity<Map<String, Object>> getSchema(
    @RequestParam String dbType,
    @RequestParam String database) {
    try {
        DataSource dataSource = DatabaseUtils.getDataSource(dbType, database);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        Map<String, Object> schema = new HashMap<>();

        List<String> tables;

        if (dbType.equalsIgnoreCase("postgres")) {
            tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND (table_type = 'BASE TABLE' OR table_type = 'VIEW')",
                String.class);
        } else {
            tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND (table_type = 'BASE TABLE' OR table_type = 'VIEW')",
                String.class, database);
        }

        List<Map<String, Object>> tablesInfo = new ArrayList<>();

        for (String table : tables) {
            Map<String, Object> tableInfo = new HashMap<>();
            tableInfo.put("tableName", table);

            List<Map<String, Object>> columns;
            if (dbType.equalsIgnoreCase("postgres")) {
                columns = jdbcTemplate.queryForList(
                    "SELECT column_name, data_type, is_nullable FROM information_schema.columns WHERE table_schema = 'public' AND table_name = ?",
                    table);
            } else {
                columns = jdbcTemplate.queryForList(
                    "SELECT column_name, data_type, column_key FROM information_schema.columns WHERE table_schema = ? AND table_name = ?",
                    database, table);
            }

            tableInfo.put("columns", columns);

            // Get primary keys
            List<String> primaryKeys = new ArrayList<>();
            if (dbType.equalsIgnoreCase("postgres")) {
                List<Map<String, Object>> primaryKeyRows = jdbcTemplate.queryForList(
                    "SELECT a.attname AS column_name FROM pg_index i " +
                    "JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = ANY(i.indkey) " +
                    "WHERE i.indrelid = ?::regclass AND i.indisprimary",
                    table);
                for (Map<String, Object> row : primaryKeyRows) {
                    primaryKeys.add((String) row.get("column_name"));
                }
            } else {
                List<Map<String, Object>> primaryKeyRows = jdbcTemplate.queryForList(
                    "SELECT column_name FROM information_schema.key_column_usage WHERE table_schema = ? AND table_name = ? AND constraint_name = 'PRIMARY'",
                    database, table);
                for (Map<String, Object> row : primaryKeyRows) {
                    primaryKeys.add((String) row.get("column_name"));
                }
            }

            tableInfo.put("primaryKeys", primaryKeys);

            // Get foreign keys
            List<Map<String, Object>> foreignKeys;
            if (dbType.equalsIgnoreCase("postgres")) {
                foreignKeys = jdbcTemplate.queryForList(
                    "SELECT kcu.column_name, ccu.table_name AS referenced_table_name, ccu.column_name AS referenced_column_name " +
                    "FROM information_schema.key_column_usage kcu " +
                    "JOIN information_schema.constraint_column_usage ccu ON ccu.constraint_name = kcu.constraint_name " +
                    "WHERE kcu.table_schema = 'public' AND kcu.table_name = ? AND ccu.table_schema = 'public'",
                    table);
            } else {
                foreignKeys = jdbcTemplate.queryForList(
                    "SELECT column_name, referenced_table_name, referenced_column_name " +
                    "FROM information_schema.key_column_usage " +
                    "WHERE table_schema = ? AND table_name = ? AND referenced_table_name IS NOT NULL",
                    database, table);
            }

            tableInfo.put("foreignKeys", foreignKeys);

            tablesInfo.add(tableInfo);
        }

        schema.put("tables", tablesInfo);

        return ResponseEntity.ok(schema);

    } catch (Exception e) {
        e.printStackTrace(); // Log the full stack trace
        return ResponseEntity.status(500)
            .body(Collections.singletonMap("error", "Failed to retrieve schema: " + e.getMessage()));
    }
}
}