package com.sismics.reader.core.reporting.events;

import java.util.List;

import com.sismics.reader.core.reporting.Bug;
import com.sismics.reader.core.reporting.BugStatus;

public class BugStatusUpdateEvent implements BugEvent {
    private String id;
    private BugStatus status;

    public BugStatusUpdateEvent(String id, BugStatus status) {
        this.id = id;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public BugStatus getStatus() {
        return status;
    }

    @Override
    public void trigger(List<Bug> bugList) {
        bugList.stream()
            .filter(bug -> bug.getId().equals(id))
            .forEach(bug -> bug.setStatus(status));
    }    
}
