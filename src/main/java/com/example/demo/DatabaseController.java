package com.example.demo;
import com.example.demo.DatabaseUtils.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;
// import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.example.demo.SearchRequest;
import javax.sql.DataSource;
import java.util.stream.Collectors;
import java.sql.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import com.example.demo.UserService;
import org.springframework.http.HttpStatus;
import com.example.demo.User;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class DatabaseController {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseController.class);

    @Autowired
    @Qualifier("mysqlJdbcTemplate")
    private JdbcTemplate mysqlJdbcTemplate;

    @Autowired
    @Qualifier("postgresJdbcTemplate")
    private JdbcTemplate postgresJdbcTemplate;

    @Autowired
    @Qualifier("mariadbJdbcTemplate")
    private JdbcTemplate mariadbJdbcTemplate;

    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ApplicationContext applicationContext;

    private final DatabaseUtils databaseUtils = new DatabaseUtils();
    private final DatabaseConnectionService connectionService = databaseUtils.new DatabaseConnectionService();

    @Autowired
    private UserService userService;

    @PostMapping("/api/connect")
    public ResponseEntity<?> connectToDatabase(@RequestBody DatabaseConnectionRequest request) {
        String dbType = request.getDbType();
        Integer port = request.getPort();
        String username = request.getUsername();
        String password = request.getPassword();

        System.out.println("connectToDatabase called");
        // Validate required fields
        if (dbType == null || port == null || username == null || password == null ||
            dbType.isEmpty() || username.isEmpty() || password.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error", "All fields are required."));
        }

        try {
            // Check if connection already exists
            if (DatabaseUtils.getDataSource(dbType) != null) {
                return ResponseEntity.status(400)
                        .body(Collections.singletonMap("error", "Connection already exists for this dbType."));
            }

            // Create and configure DataSource
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            String url;

            switch (dbType.toLowerCase()) {
                case "mysql":
                case "mariadb":
                    url = "jdbc:" + dbType.toLowerCase() + "://localhost:" + port;
                    dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
                    break;
                case "postgres":
                    url = "jdbc:postgresql://localhost:" + port;
                    dataSource.setDriverClassName("org.postgresql.Driver");
                    break;
                default:
                    return ResponseEntity.status(400)
                            .body(Collections.singletonMap("error", "Unsupported database type: " + dbType));
            }

            dataSource.setUrl(url);
            dataSource.setUsername(username);
            dataSource.setPassword(password);

            // Test the connection
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplate.execute("SELECT 1");

            // Add DataSource to DatabaseUtils
            DatabaseUtils.addDataSource(dbType, dataSource);

            return ResponseEntity.ok(Collections.singletonMap("message", "Database connected and details saved successfully."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Collections.singletonMap("error", "Failed to connect to database: " + e.getMessage()));
        }
    }

    /**
     * Endpoint to list all databases for a specific dbType or for all dbTypes.
     *
     * @param dbType Optional. The type of the database (e.g., mysql, postgres, mariadb).
     * @return A ResponseEntity containing a map of dbType to list of databases or an error message.
     */
    @GetMapping("/api/list_databases")
    public ResponseEntity<?> listDatabases(@RequestParam(required = false) String dbType) {
        try {
            Map<String, List<String>> databasesMap = new HashMap<>();

            if (dbType != null && !dbType.isEmpty()) {
                List<String> databases = getDatabasesForDbType(dbType);
                if (databases == null) {
                    return ResponseEntity.status(404)
                            .body(Collections.singletonMap("error", "Connection not found for the specified dbType."));
                }
                databasesMap.put(dbType.toLowerCase(), databases);
            } else {
                // List databases for all connected dbTypes
                List<ConnectionInfo> connections = DatabaseUtils.getAllConnections();
                for (ConnectionInfo connection : connections) {
                    String type = connection.getDbType().toLowerCase();
                    List<String> databases = getDatabasesForDbType(type);
                    if (databases != null) {
                        databasesMap.put(type, databases);
                    }
                }
            }

            return ResponseEntity.ok(databasesMap);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Collections.singletonMap("error", "Failed to list databases: " + e.getMessage()));
        }
    }

    /**
     * Retrieves the list of databases for a given dbType.
     *
     * @param dbType The type of the database (e.g., mysql, postgres, mariadb).
     * @return A list of database names or null if connection not found.
     */
    private List<String> getDatabasesForDbType(String dbType) {
        DataSource dataSource = DatabaseUtils.getDataSource(dbType);
        if (dataSource == null) {
            return null;
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        String query;

        switch (dbType.toLowerCase()) {
            case "mysql":
            case "mariadb":
                query = "SHOW DATABASES;";
                break;
            case "postgres":
                query = "SELECT datname FROM pg_database WHERE datistemplate = false;";
                break;
            default:
                throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }

        List<Map<String, Object>> result = jdbcTemplate.queryForList(query);
        return result.stream()
                .map(row -> {
                    if (dbType.equalsIgnoreCase("postgres")) {
                        return (String) row.get("datname");
                    } else { // mysql/mariadb
                        return (String) row.values().iterator().next();
                    }
                })
                .collect(Collectors.toList());
    }


    /**
     * Endpoint to list all columns of a specific table in a given database.
     *
     * @param dbType   The type of the database (e.g., mysql, postgres, mariadb).
     * @param database The name of the database.
     * @param table    The name of the table.
     * @return A ResponseEntity containing a list of columns or an error message.
     */
    @GetMapping("/api/list_columns")
    public ResponseEntity<?> listColumns(
            @RequestParam String dbType,
            @RequestParam String database,
            @RequestParam String table) {
        try {
            DataSource dataSource = DatabaseUtils.getDataSource(dbType);
            if (dataSource == null) {
                return ResponseEntity.status(404)
                        .body(Collections.singletonMap("error", "Connection not found for the specified dbType."));
            }

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            String query;

            switch (dbType.toLowerCase()) {
                case "mysql":
                case "mariadb":
                    query = "SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT " +
                            "FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_SCHEMA = '" + sanitizeDatabaseName(database) + "' AND TABLE_NAME = ?";
                    break;
                case "postgres":
                    query = "SELECT column_name, data_type, is_nullable, column_default " +
                            "FROM information_schema.columns " +
                            "WHERE table_catalog = ? AND table_name = ? AND table_schema = 'public'";
                    break;
                default:
                    return ResponseEntity.status(400)
                            .body(Collections.singletonMap("error", "Unsupported database type: " + dbType));
            }

            List<Map<String, Object>> columns;

            if (dbType.equalsIgnoreCase("postgres")) {
                columns = jdbcTemplate.queryForList(query, database, table);
            } else { // mysql/mariadb
                columns = jdbcTemplate.queryForList(query, table);
            }

            return ResponseEntity.ok(columns);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Collections.singletonMap("error", "Failed to list columns: " + e.getMessage()));
        }
    }

    /**
     * Sanitizes the database name to prevent SQL injection.
     *
     * @param database The name of the database.
     * @return A sanitized database name.
     */
    private String sanitizeDatabaseName(String database) {
        // Implement proper sanitization based on your application's requirements.
        // This is a simple example that allows only alphanumeric characters and underscores.
        if (database.matches("[a-zA-Z0-9_]+")) {
            return database;
        } else {
            throw new IllegalArgumentException("Invalid database name.");
        }
    }


    /**
     * Endpoint to delete an existing database connection.
     *
     * @param request The DeleteConnectionRequest containing dbType.
     * @return A ResponseEntity indicating success or failure.
     */
    @PostMapping("/api/delete_connection")
    public ResponseEntity<?> deleteConnection(@RequestBody DeleteConnectionRequest request) {
        String dbType = request.getDbType();

        if (dbType == null || dbType.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error", "dbType is required."));
        }

        try {
            boolean removed = DatabaseUtils.removeDataSource(dbType);
            if (removed) {
                return ResponseEntity.ok(Collections.singletonMap("message", "Database connection removed successfully."));
            } else {
                return ResponseEntity.status(404)
                        .body(Collections.singletonMap("error", "Connection not found."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Collections.singletonMap("error", "Failed to delete connection: " + e.getMessage()));
        }
    }

    /**
     * Endpoint to retrieve all existing database connections.
     *
     * @return A ResponseEntity containing a list of ConnectionInfo objects.
     */
    @GetMapping("/api/list_connections")
    public ResponseEntity<?> listAllConnections() {
        try {
            List<ConnectionInfo> connections = DatabaseUtils.getAllConnections();
            return ResponseEntity.ok(connections);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Collections.singletonMap("error", "Failed to retrieve connections: " + e.getMessage()));
        }
    }

    /**
     * Endpoint to retrieve all tables from a specific database.
     *
     * @param dbType   The type of the database (e.g., mysql, postgres, mariadb).
     * @param database The name of the database.
     * @return A ResponseEntity containing a list of tables or an error message.
     */
    @GetMapping("/api/show_tables")
    public ResponseEntity<?> showTables(
        @RequestParam String dbType,
        @RequestParam String database) {
        try {
            DataSource dataSource = DatabaseUtils.getDataSource(dbType);
            if (dataSource == null) {
                return ResponseEntity.status(404)
                        .body(Collections.singletonMap("error", "Connection not found for the specified dbType."));
            }

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            String query;

            switch (dbType.toLowerCase()) {
                case "mysql":
                case "mariadb":
                    query = "SHOW TABLES FROM " + database + ";";
                    try (java.sql.Statement stmt = dataSource.getConnection().createStatement()) {
                        java.sql.ResultSet rs = stmt.executeQuery(query);
                        List<String> tables = new ArrayList<>();
                        while (rs.next()) {
                            tables.add(rs.getString(1)); // Assuming the table name is in the first column
                        }
                        return ResponseEntity.ok(tables);
                    } catch (SQLException e) {
                        return ResponseEntity.status(500)
                                .body(Collections.singletonMap("error", "Failed to retrieve tables: " + e.getMessage()));
                    }
                case "postgres":
                    query = "SELECT table_name FROM information_schema.tables WHERE table_schema='public'";
                    try (PreparedStatement pstmt = dataSource.getConnection().prepareStatement(query)) {
                        ResultSet rs = pstmt.executeQuery();
                        // Process the ResultSet
                    } catch (SQLException e) {
                        // Handle SQL exception
                    }
                    break;
                default:
                    return ResponseEntity.status(400)
                            .body(Collections.singletonMap("error", "Unsupported database type: " + dbType));
            }

            List<String> tables = jdbcTemplate.queryForList(query, String.class, database);
            return ResponseEntity.ok(tables);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Collections.singletonMap("error", "Failed to retrieve tables: " + e.getMessage()));
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


    /**
     * @param dbType       The type of the database (e.g., mysql, postgres, mariadb).
     * @param databaseName The name of the database.
     * @return A ResponseEntity indicating the connection status.
     */
    @PostMapping("/api/test-connection")
    public ResponseEntity<?> testDatabaseConnection(@RequestParam String dbType, @RequestParam String databaseName) {
        try {
            DataSource dataSource = DatabaseUtils.getDataSource(dbType);
            if (dataSource == null) {
                return ResponseEntity.status(400)
                        .body(Collections.singletonMap("error", "Database not connected: " + databaseName));
            }

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok(Collections.singletonMap("message", "Database connection successful!"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Collections.singletonMap("error", "Database connection failed: " + e.getMessage()));
        }
    }
    /**
     * Endpoint to perform search operations across connected databases.
     *
     * @param request The SearchRequest containing search parameters.
     * @return A ResponseEntity with search results or error messages.
     */

     
    /**
     * Search endpoint to search terms across multiple DBMS, databases, tables, and columns.
     *
     * @param request The search request containing searchTerm, dbTypes, and databases.
     * @return ResponseEntity with categorized search results.
     */
    @PostMapping("/api/search")
public ResponseEntity<Map<String, Object>> search(@RequestBody SearchRequest request) {
    try {
        List<SearchResult> results = new ArrayList<>();
        String searchTerm = request.getSearchTerm();

        // Determine DBMS to search
        List<String> dbTypes = request.getDbTypes();
        if (dbTypes == null || dbTypes.isEmpty()) {
            dbTypes = Arrays.asList("mysql", "postgres", "mariadb");
        }

        for (String dbType : dbTypes) {
            JdbcTemplate jdbcTemplate = getJdbcTemplate(dbType);
            if (jdbcTemplate == null) {
                logger.warn("JdbcTemplate for dbType {} is null. Skipping.", dbType);
                continue;
            }

            // Get databases
            List<String> databases = getDatabases(jdbcTemplate, dbType);
            if (request.getDatabases() != null && !request.getDatabases().isEmpty()) {
                databases = databases.stream()
                        .filter(request.getDatabases()::contains)
                        .collect(Collectors.toList());
            }

            for (String database : databases) {
                // Get tables
                List<String> tables = getTablesAndViews(jdbcTemplate, dbType, database);
                for (String table : tables) {
                    // Get columns
                    List<String> columns = getColumns(jdbcTemplate, dbType, database, table);
                    for (String column : columns) {
                        // Search in column
                        List<Map<String, Object>> matchedRows = searchInColumn(jdbcTemplate, dbType, database, table, column, searchTerm);
                        for (Map<String, Object> row : matchedRows) {
                            SearchResult searchResult = new SearchResult();
                            searchResult.setDbType(dbType);
                            searchResult.setDatabase(database);
                            searchResult.setTable(table);
                            searchResult.setColumn(column);
                            searchResult.setValue(row.get(column));
                            searchResult.setRowData(row);
                            results.add(searchResult);
                        }
                    }
                }
            }
        }

        // Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("results", results);
        return ResponseEntity.ok(response);
    } catch (Exception e) {
        logger.error("Error during search operation", e);
        return ResponseEntity.status(500)
                .body(Collections.singletonMap("error", "Failed to perform search: " + e.getMessage()));
    }
}


    /**
     * Retrieves the appropriate JdbcTemplate based on dbType.
     *
     * @param dbType The type of the database (e.g., mysql, postgres, mariadb).
     * @return The corresponding JdbcTemplate or null if not found.
     */
    private JdbcTemplate getJdbcTemplate(String dbType) {
        switch (dbType.toLowerCase()) {
            case "mysql":
                return mysqlJdbcTemplate;
            case "postgres":
                return postgresJdbcTemplate;
            case "mariadb":
                return mariadbJdbcTemplate;
            default:
                return null;
        }
    }

    /**
 * Retrieves the list of databases for the given DBMS.
 *
 * @param jdbcTemplate The JdbcTemplate to use.
 * @param dbType       The type of the database.
 * @return List of database names.
 */
private List<String> getDatabases(JdbcTemplate jdbcTemplate, String dbType) {
    String query = "";
    switch (dbType.toLowerCase()) {
        case "mysql":
        case "mariadb":
            query = "SHOW DATABASES";
            break;
        case "postgres":
            query = "SELECT datname FROM pg_database WHERE datistemplate = false";
            break;
        default:
            logger.warn("Unsupported dbType for getting databases: {}", dbType);
            return Collections.emptyList();
    }
    try {
        logger.debug("Executing getDatabases query: {}", query);
        List<String> databases = jdbcTemplate.queryForList(query, String.class);
        logger.debug("getDatabases returned {} databases.", databases.size());
        return databases;
    } catch (Exception e) {
        logger.error("Error retrieving databases for dbType {}: {}", dbType, e.getMessage());
        return Collections.emptyList();
    }
}

    /**
 * Retrieves the list of tables for the given database.
 *
 * @param jdbcTemplate The JdbcTemplate to use.
 * @param dbType       The type of the database.
 * @param database     The name of the database.
 * @return List of table names.
 */
private List<String> getTablesAndViews(JdbcTemplate jdbcTemplate, String dbType, String database) {
    String query = "";
    List<String> tablesAndViews = new ArrayList<>();
    switch (dbType.toLowerCase()) {
        case "mysql":
        case "mariadb":
            query = "SHOW FULL TABLES FROM " + escapeIdentifier(database);
            try {
                logger.debug("Executing getTablesAndViews query: {}", query);
                List<Map<String, Object>> results = jdbcTemplate.queryForList(query);
                String tableNameKey = "Tables_in_" + database;
                String tableTypeKey = "Table_type";
                for (Map<String, Object> row : results) {
                    String name = (String) row.get(tableNameKey);
                    tablesAndViews.add(name);
                }
            } catch (Exception e) {
                logger.error("Error retrieving tables and views for database {}: {}", database, e.getMessage());
            }
            break;
        case "postgres":
            String tableQuery = "SELECT tablename FROM pg_tables WHERE schemaname = 'public'";
            String viewQuery = "SELECT viewname FROM pg_views WHERE schemaname = 'public'";
            try {
                logger.debug("Executing getTables query: {}", tableQuery);
                List<String> tables = jdbcTemplate.queryForList(tableQuery, String.class);
                logger.debug("Executing getViews query: {}", viewQuery);
                List<String> views = jdbcTemplate.queryForList(viewQuery, String.class);
                tablesAndViews.addAll(tables);
                tablesAndViews.addAll(views);
            } catch (Exception e) {
                logger.error("Error retrieving tables and views for database {}: {}", database, e.getMessage());
            }
            break;
        default:
            logger.warn("Unsupported dbType for getting tables and views: {}", dbType);
            return Collections.emptyList();
    }
    return tablesAndViews;
}

    /**
 * Retrieves the list of columns for the given table.
 *
 * @param jdbcTemplate The JdbcTemplate to use.
 * @param dbType       The type of the database.
 * @param database     The name of the database.
 * @param table        The name of the table.
 * @return List of column names.
 */
private List<String> getColumns(JdbcTemplate jdbcTemplate, String dbType, String database, String table) {
    String query = "";
    switch (dbType.toLowerCase()) {
        case "mysql":
        case "mariadb":
            query = "SHOW COLUMNS FROM " + escapeIdentifier(database) + "." + escapeIdentifier(table);
            break;
        case "postgres":
            query = "SELECT column_name FROM information_schema.columns WHERE table_schema = 'public' AND table_name = '" + escapeString(table) + "'";
            break;
        default:
            logger.warn("Unsupported dbType for getting columns: {}", dbType);
            return Collections.emptyList();
    }
    try {
        logger.debug("Executing getColumns query: {}", query);
        if (dbType.equalsIgnoreCase("postgres")) {
            return jdbcTemplate.queryForList(query, String.class);
        } else {
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(query);
            return columns.stream()
                    .map(row -> (String) row.get("Field"))
                    .collect(Collectors.toList());
        }
    } catch (Exception e) {
        logger.error("Error retrieving columns for table {}: {}", table, e.getMessage());
        return Collections.emptyList();
    }
}

    /**
 * Searches for the term in the specified column.
 *
 * @param jdbcTemplate The JdbcTemplate to use.
 * @param dbType       The type of the database.
 * @param database     The name of the database.
 * @param table        The name of the table.
 * @param column       The name of the column.
 * @param searchTerm   The term to search for.
 * @return List of matched rows.
 */
private List<Map<String, Object>> searchInColumn(JdbcTemplate jdbcTemplate, String dbType, String database, String table, String column, String searchTerm) {
    String query = "";
    switch (dbType.toLowerCase()) {
        case "mysql":
        case "mariadb":
            // Ensure proper escaping and database.table syntax
            query = "SELECT * FROM " + escapeIdentifier(database) + "." + escapeIdentifier(table) + 
                    " WHERE " + escapeIdentifier(column) + " LIKE ?";
            break;
        case "postgres":
            // In PostgreSQL, the database is selected via DataSource, so omit it from the query
            query = "SELECT * FROM " + escapeIdentifier(table) + 
                    " WHERE " + escapeIdentifier(column) + " ILIKE ?";
            break;
        default:
            return Collections.emptyList();
    }
    try {
        logger.debug("Executing query: {}", query);
        List<Map<String, Object>> results = jdbcTemplate.queryForList(query, "%" + searchTerm + "%");
        logger.debug("Query returned {} records.", results.size());
        return results;
    } catch (Exception e) {
        logger.error("Error executing search query: {}", e.getMessage());
        return Collections.emptyList();
    }
}

    
    /**
     * Escapes SQL identifiers to prevent SQL injection.
     *
     * @param identifier The identifier to escape.
     * @return Escaped identifier.
     */
    private String escapeIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    /**
     * Escapes SQL strings to prevent SQL injection.
     *
     * @param value The string to escape.
     * @return Escaped string.
     */
    private String escapeString(String value) {
        return value.replace("'", "''");
    }

private Map<String, List<Map<String, Object>>> searchInDatabase(JdbcTemplate jdbcTemplate, String query) {
    Map<String, List<Map<String, Object>>> dbResults = new HashMap<>();
    
    // Retrieve list of tables in the specified database
    String getTablesQuery = "SELECT table_name FROM information_schema.tables WHERE table_schema = ?";
    List<Map<String, Object>> tables;
    try {
        tables = jdbcTemplate.queryForList(getTablesQuery, jdbcTemplate.getDataSource().getConnection().getCatalog());
    } catch (SQLException e) {
        throw new RuntimeException("Failed to retrieve tables: " + e.getMessage(), e);
    }
    
    // Iterate through the tables and perform search for each one
    for (Map<String, Object> table : tables) {
        String tableName = (String) table.get("table_name");
        
        // Skip system tables like information_schema
        if (tableName.startsWith("information_schema")) {
            continue;
        }
        
        // Get the columns for the current table
        String columns = getColumnsForTable(jdbcTemplate, tableName);
        
        // Build the SELECT query dynamically with IFNULL to handle NULL values
        String searchQuery = "SELECT * FROM " + tableName + " WHERE CONCAT_WS(' ', " + columns + ") LIKE ?";
        
        // Execute the search query and add matching results, binding the search term properly
        List<Map<String, Object>> results = jdbcTemplate.queryForList(searchQuery, "%" + query + "%");
        
        if (!results.isEmpty()) {
            // Group results by database name (table schema)
            String databaseName;
            try {
                databaseName = jdbcTemplate.getDataSource().getConnection().getCatalog();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to get database catalog: " + e.getMessage(), e);
            }
            dbResults.computeIfAbsent(databaseName, k -> new ArrayList<>()).addAll(results);
        }
    }
    
    return dbResults;
}

private String getColumnsForTable(JdbcTemplate jdbcTemplate, String tableName) {
    // This method should return the list of columns to search from the given table
    // Hardcoding the columns for the "city" table in the "world" database
    if (tableName.equalsIgnoreCase("city")) {
        return "IFNULL(ID, ''), IFNULL(Name, ''), IFNULL(CountryCode, ''), IFNULL(District, ''), IFNULL(Population, '')";
    }
    
    // You can extend this method for other tables as needed.
    return ""; // Empty return if no specific logic is implemented
}




private Map<String, List<String>> getFilters() {
    Map<String, List<String>> filters = new HashMap<>();
    filters.put("dbType", Arrays.asList("mysql", "postgres", "mariadb"));
    // Add more filters as needed
    return filters;
}


    
    /**
     * Endpoint to retrieve the schema of a specific database.
     *
     * @param dbType   The type of the database (e.g., mysql, postgres, mariadb).
     * @param database The name of the database whose schema is to be retrieved.
     * @return A ResponseEntity containing the schema information or an error message.
     */
    @GetMapping("/api/get_schema")
    public ResponseEntity<?> getSchema(
            @RequestParam String dbType,
            @RequestParam String database) {
        try {
            DataSource dataSource = DatabaseUtils.getDataSource(dbType);
            if (dataSource == null) {
                return ResponseEntity.status(404)
                        .body(Collections.singletonMap("error", "Connection not found for the specified dbType."));
            }

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            String query;

            switch (dbType.toLowerCase()) {
                case "mysql":
                case "mariadb":
                    query = "SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT " +
                            "FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ?";
                    break;
                case "postgres":
                    query = "SELECT table_name, column_name, data_type, is_nullable, column_default " +
                            "FROM information_schema.columns WHERE table_catalog = ?";
                    break;
                default:
                    return ResponseEntity.status(400)
                            .body(Collections.singletonMap("error", "Unsupported database type: " + dbType));
            }

            List<Map<String, Object>> schema = jdbcTemplate.queryForList(query, database);

            return ResponseEntity.ok(schema);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Collections.singletonMap("error", "Failed to retrieve schema: " + e.getMessage()));
        }
    }

    @GetMapping("/api/get_schemarelation")
    public ResponseEntity<?> getSchemaRelation(@RequestParam String dbType, @RequestParam String database) {
        try {
            DataSource dataSource = DatabaseUtils.getDataSource(dbType);
            if (dataSource == null) {
                return ResponseEntity.status(404)
                        .body(Collections.singletonMap("error", "Connection not found for the specified dbType."));
            }

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            List<Map<String, Object>> tablesInfo = new ArrayList<>();

            // Query to get tables
            String tableQuery = "SELECT table_name FROM information_schema.tables WHERE table_schema = ?";
            List<String> tableNames = jdbcTemplate.queryForList(tableQuery, String.class, database);

            for (String tableName : tableNames) {
                Map<String, Object> tableInfo = new HashMap<>();
                tableInfo.put("tableName", tableName);

                // Query to get columns
                String columnQuery = "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_KEY FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
                List<Map<String, Object>> columns = jdbcTemplate.queryForList(columnQuery, database, tableName);

                // Normalize column names for consistent output
                List<Map<String, Object>> normalizedColumns = columns.stream()
                        .map(column -> {
                            Map<String, Object> normalizedColumn = new HashMap<>();
                            normalizedColumn.put("column_name", column.get("COLUMN_NAME") != null ? column.get("COLUMN_NAME") : column.get("column_name"));
                            normalizedColumn.put("data_type", column.get("DATA_TYPE") != null ? column.get("DATA_TYPE") : column.get("data_type"));
                            normalizedColumn.put("column_key", column.get("COLUMN_KEY") != null ? column.get("COLUMN_KEY") : column.get("column_key"));
                            return normalizedColumn;
                        })
                        .collect(Collectors.toList());

                // Query to get foreign key information
                String foreignKeyQuery = "SELECT kcu.COLUMN_NAME, kcu.REFERENCED_TABLE_NAME, kcu.REFERENCED_COLUMN_NAME " +
                                         "FROM information_schema.KEY_COLUMN_USAGE kcu " +
                                         "WHERE kcu.TABLE_SCHEMA = ? AND kcu.TABLE_NAME = ? AND kcu.REFERENCED_TABLE_NAME IS NOT NULL";
                List<Map<String, Object>> foreignKeys = jdbcTemplate.queryForList(foreignKeyQuery, database, tableName);

                // Add foreign key information to columns
                for (Map<String, Object> foreignKey : foreignKeys) {
                    String columnName = (String) foreignKey.get("COLUMN_NAME");
                    String referencedTable = (String) foreignKey.get("REFERENCED_TABLE_NAME");
                    String referencedColumn = (String) foreignKey.get("REFERENCED_COLUMN_NAME");

                    normalizedColumns.stream()
                            .filter(col -> col.get("column_name").equals(columnName))
                            .findFirst()
                            .ifPresent(col -> {
                                col.put("foreign_key", true);
                                col.put("referenced_table", referencedTable);
                                col.put("referenced_column", referencedColumn);
                            });
                }

                tableInfo.put("columns", normalizedColumns);
                tablesInfo.add(tableInfo);
            }

            return ResponseEntity.ok(Collections.singletonMap("tables", tablesInfo));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Collections.singletonMap("error", "Failed to retrieve schema relation: " + e.getMessage()));
        }
    }

    @PostMapping("/api/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {
        try {
            // Hash the password using SHA-256
            String hashedPassword = hashPassword(user.getPassword());
            user.setPassword(hashedPassword); // Set the hashed password

            // Save the user to the database
            userService.saveUser(user); // Implement this method in UserService

            return ResponseEntity.ok(Collections.singletonMap("message", "User registered successfully."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Collections.singletonMap("error", "Failed to register user: " + e.getMessage()));
        }
    }

    @PostMapping("/api/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        try {
            // Debugging: Log the incoming user object
            System.out.println("Login attempt for user: " + user.getUsername());

            // Check if the user object is valid
            if (user.getUsername() == null || user.getPassword() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Collections.singletonMap("error", "Username and password are required."));
            }

            // Check if the user exists in the database
            String username = user.getUsername();
            User existingUser = userService.findByUsername(username);
            if (existingUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Collections.singletonMap("error", "Invalid credentials."));
            }

            // Hash the input password and compare
            String hashedInputPassword = hashPassword(user.getPassword());
            if (!hashedInputPassword.equals(existingUser.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Collections.singletonMap("error", "Invalid credentials."));
            }

            return ResponseEntity.ok(Collections.singletonMap("message", "Login successful."));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Collections.singletonMap("error", "Failed to login: " + e.getMessage()));
        }
    }

    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(encodedHash);
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}