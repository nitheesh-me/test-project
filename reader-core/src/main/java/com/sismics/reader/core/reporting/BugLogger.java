package com.sismics.reader.core.reporting;

import java.util.List;

import com.sismics.reader.core.reporting.events.BugEvent;

public class BugLogger implements Observer {
    @Override
    public void update(List<Bug> bugs) {
        for (Bug bug : bugs) {
            System.out.println(bug.toString());
        }
    }
}
