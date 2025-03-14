package com.sismics.reader.core.reporting;

import java.util.List;

public interface Observer {
    void update(List<Bug> bugs);
}
