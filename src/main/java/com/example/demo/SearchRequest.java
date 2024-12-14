// filepath: /c:/harshal/sql_explorer/demo/src/main/java/com/example/demo/SearchRequest.java
package com.example.demo;

import java.util.List;

public class SearchRequest {
    private String searchTerm;
    private List<String> dbTypes;
    private List<String> databases;

    // Getters and Setters
    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public List<String> getDbTypes() {
        return dbTypes;
    }

    public void setDbTypes(List<String> dbTypes) {
        this.dbTypes = dbTypes;
    }

    public List<String> getDatabases() {
        return databases;
    }

    public void setDatabases(List<String> databases) {
        this.databases = databases;
    }
}