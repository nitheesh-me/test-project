package com.sismics.reader.core.reporting.events;

import java.util.List;

import com.sismics.reader.core.reporting.Bug;

public class BugDeletedEvent implements BugEvent {
    private String id;

    public BugDeletedEvent(String id) {
        this.id = id;
    } 

    public String getId() {
        return id;
    }

    @Override
    public void trigger(List<Bug> bugList) {
        bugList.removeIf(bug -> bug.getId().equals(id));
    }
}
