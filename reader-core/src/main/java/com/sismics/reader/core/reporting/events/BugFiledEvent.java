package com.sismics.reader.core.reporting.events;

import java.util.List;

import com.sismics.reader.core.reporting.Bug;
import com.sismics.reader.core.reporting.BugStatus;

public class BugFiledEvent implements BugEvent{
    private String id;
    private String description;
    private BugStatus status;

    public BugFiledEvent(String id, String description) {
        this.id = id;
        this.description = description;
        this.status = BugStatus.FILED;
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

    @Override
    public void trigger(List<Bug> bugList) {
        Bug bug = new Bug(id, description);
        bugList.add(bug);   
    }

}
