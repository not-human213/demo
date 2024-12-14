package com.example.demo;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

@Service
public class DatabaseService {

    private final JdbcTemplate mysqlJdbcTemplate;
    private final JdbcTemplate postgresJdbcTemplate;
    private final JdbcTemplate mariadbJdbcTemplate;

    public DatabaseService(
            @Qualifier("mysqlDataSource") DataSource mysqlDataSource,
            @Qualifier("postgresDataSource") DataSource postgresDataSource,
            @Qualifier("mariadbDataSource") DataSource mariadbDataSource) {

        this.mysqlJdbcTemplate = new JdbcTemplate(mysqlDataSource);
        this.postgresJdbcTemplate = new JdbcTemplate(postgresDataSource);
        this.mariadbJdbcTemplate = new JdbcTemplate(mariadbDataSource);
    }

    public void performQuery(String dbType, String query) {
        JdbcTemplate jdbcTemplate;
        
        // Decide which JdbcTemplate to use based on the database type
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
                throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }

        // Perform query using the selected JdbcTemplate
        jdbcTemplate.queryForList(query).forEach(row -> System.out.println(row));
    }
}
