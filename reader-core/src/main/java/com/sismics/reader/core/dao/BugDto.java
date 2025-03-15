package com.sismics.reader.core.dao;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * A Data Transfer Object for Bug, exposing only necessary properties.
 */
public class BugDto {
    @JsonProperty
    private String id;

    @JsonProperty
    private String description;

    @JsonProperty
    private String status;

    public BugDto() {
        // Default constructor for Jackson
    }

    public BugDto(String id, String description, String status) {
        this.id = id;
        this.description = description;
        this.status = status;
    }

    // Getters and setters
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
}