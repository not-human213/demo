package com.example.demo;

public class DatabaseConnectionRequest {
    private String dbType;   // Type of the database (e.g., mysql, postgres, mariadb)
    private Integer port;    // Database port (e.g., 3306 for MySQL)
    private String username; // Database username
    private String password; // Database password

    // Constructors
    public DatabaseConnectionRequest() {}

    public DatabaseConnectionRequest(String dbType, Integer port, String username, String password) {
        this.dbType = dbType;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    // Getters and Setters
    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
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
}