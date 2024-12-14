// filepath: /C:/harshal/sql_explorer/demo/src/main/java/com/example/demo/config/DatabaseConfig.java
package com.example.demo.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    // PostgreSQL DataSource and JdbcTemplate
    @Bean(name = "postgresDataSource")
    public DataSource postgresDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl("jdbc:postgresql://localhost:5432/your_postgres_db?useSSL=false&serverTimezone=UTC");
        dataSource.setUsername("postgres_user");
        dataSource.setPassword("your_password");
        return dataSource;
    }

    @Bean(name = "postgresJdbcTemplate")
    public JdbcTemplate postgresJdbcTemplate(@Qualifier("postgresDataSource") DataSource postgresDataSource) {
        return new JdbcTemplate(postgresDataSource);
    }

    // MariaDB DataSource and JdbcTemplate
    @Bean(name = "mariadbDataSource")
public DataSource mariadbDataSource() {
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName("org.mariadb.jdbc.Driver"); // MariaDB-specific driver
    dataSource.setUrl("jdbc:mariadb://localhost:3316/world"); // MariaDB connection string
    dataSource.setUsername("root"); // Replace with your MariaDB username
    dataSource.setPassword("Harshal@sql"); // Replace with your MariaDB password
    return dataSource;
}

@Bean(name = "mariadbJdbcTemplate")
public JdbcTemplate mariadbJdbcTemplate(@Qualifier("mariadbDataSource") DataSource mariadbDataSource) {
    return new JdbcTemplate(mariadbDataSource);
}
    // MySQL DataSource and JdbcTemplate
    @Bean(name = "mysqlDataSource")
    public DataSource mysqlDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3306/world");
        dataSource.setUsername("root"); // Replace with your MySQL username
        dataSource.setPassword("Harshal@sql"); // Replace with your MySQL password
        return dataSource;
    }

    @Bean(name = "mysqlJdbcTemplate")
    public JdbcTemplate mysqlJdbcTemplate(@Qualifier("mysqlDataSource") DataSource mysqlDataSource) {
        return new JdbcTemplate(mysqlDataSource);
    }
}