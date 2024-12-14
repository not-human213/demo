package com.example.demo;

public class DeleteConnectionRequest {
    private String dbType;

    // Constructors
    public DeleteConnectionRequest() {}

    public DeleteConnectionRequest(String dbType) {
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