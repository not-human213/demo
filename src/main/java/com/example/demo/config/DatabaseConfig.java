// package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");  // MySQL JDBC Driver
        dataSource.setUrl("jdbc:mysql://localhost:3306/sqlexplorer"); // Replace with your DB URL
        dataSource.setUsername("root"); // Replace with your DB username
        dataSource.setPassword("Harshal@sql"); // Replace with your DB password
        return dataSource;
    }
}
