package com.example.demo;

public class DatabaseConnection {
    private String dbType;
    private String databaseName;
    private String url;
    private String username;
    private String password;

    // Constructors
    public DatabaseConnection() {}

    public DatabaseConnection(String dbType, String databaseName, String url, String username, String password) {
        this.dbType = dbType;
        this.databaseName = databaseName;
        this.url = url;
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

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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