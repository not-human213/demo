package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;


@Configuration
public class DatabaseConfig {

    @Bean(name = "mysqlDataSource")
    public DataSource mysqlDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3306/world");
        dataSource.setUsername("root");
        dataSource.setPassword("Harshal@sql");
        return dataSource;
    }

    @Bean(name = "mysqlJdbcTemplate")
    public JdbcTemplate mysqlJdbcTemplate(@Qualifier("mysqlDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "postgresDataSource")
    public DataSource postgresDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl("jdbc:postgresql://localhost:5432/");
        dataSource.setUsername("root");
        dataSource.setPassword("Harshal@sql");
        return dataSource;
    }

    @Bean(name = "postgresJdbcTemplate")
    public JdbcTemplate postgresJdbcTemplate(@Qualifier("postgresDataSource") DataSource postgresDataSource) {
        return new JdbcTemplate(postgresDataSource);
    }

    @Bean(name = "mariadbDataSource")
    public DataSource mariadbDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.mariadb.jdbc.Driver");
        dataSource.setUrl("jdbc:mariadb://localhost:3316/");
        dataSource.setUsername("root");
        dataSource.setPassword("Harshal@sql");
        return dataSource;
    }

    @Bean(name = "mariadbJdbcTemplate")
    public JdbcTemplate mariadbJdbcTemplate(@Qualifier("mariadbDataSource") DataSource mariadbDataSource) {
        return new JdbcTemplate(mariadbDataSource);
    }
}