package com.sismics.reader.core.reporting;

public class Bug {
    private String id;
    private BugStatus status;
    private String description;

    public Bug(String id, String description) {
        this.id = id;
        this.status = BugStatus.FILED;
        this.description = description;
    }   

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public BugStatus getStatus() {
        return status;
    }

    public void setStatus(BugStatus status) {
        this.status = status;
    }
}
