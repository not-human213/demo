package com.example.demo;

import java.util.Map;

public class SearchResult {
    private String dbType;
    private String database;
    private String table;
    private String column;
    private Object value;
    private Map<String, Object> rowData;

    // Constructors
    public SearchResult() {}

    public SearchResult(String dbType, String database, String table, String column, Object value, Map<String, Object> rowData) {
        this.dbType = dbType;
        this.database = database;
        this.table = table;
        this.column = column;
        this.value = value;
        this.rowData = rowData;
    }

    // Getters and Setters
    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Map<String, Object> getRowData() {
        return rowData;
    }

    public void setRowData(Map<String, Object> rowData) {
        this.rowData = rowData;
    }
}