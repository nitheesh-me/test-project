package com.sismics.reader.core.reporting;

import java.util.UUID;

import com.sismics.reader.core.reporting.events.BugDeletedEvent;
import com.sismics.reader.core.reporting.events.BugEvent;
import com.sismics.reader.core.reporting.events.BugFiledEvent;
import com.sismics.reader.core.reporting.events.BugStatusUpdateEvent;

import java.util.ArrayList;
import java.util.List;

public class BugReportManager {
    private static BugReportManager instance;
    private final Subject subject;
    private final List<Bug> bugList = new ArrayList<>();

    private BugReportManager() {
        subject = new Subject();
    }

    public static BugReportManager getInstance() {
        if (instance == null) {
            instance = new BugReportManager();
        }
        return instance;
    }

    public void registerObserver(Observer observer) {
        subject.registerObserver(observer);
    }   

    public void reportBug(String description) {
        BugEvent event = new BugFiledEvent(UUID.randomUUID().toString(), description);
        event.trigger(bugList);
        subject.notifyObservers(bugList);
    }

    public void resolveBug(String id) {
        BugEvent event = new BugStatusUpdateEvent(id, BugStatus.RESOLVED);
        event.trigger(bugList);
        subject.notifyObservers(bugList);
    }

    public void deleteBug(String id) {
        BugEvent event = new BugDeletedEvent(id);
        event.trigger(bugList);
        subject.notifyObservers(bugList);
    }

    public List<Bug> getBugs() {
        return bugList;
    }
}
