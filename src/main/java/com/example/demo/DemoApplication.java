package com.example.demo;
import org.springframework.boot.context.properties.ConfigurationProperties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// ... other imports

@ConfigurationProperties(prefix = "database")

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}