package com.sismics.reader.core.reporting.events;

import java.util.List;

import com.sismics.reader.core.reporting.Bug;

public interface BugEvent {

    String getId();

    void trigger(List<Bug> bugList);
    
}
