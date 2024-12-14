package com.example.demo;

public class ConnectionInfo {
    private String dbType;

    // Constructors
    public ConnectionInfo() {}

    public ConnectionInfo(String dbType) {
        this.dbType = dbType;
    }

    // Getters and Setters
    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }
}