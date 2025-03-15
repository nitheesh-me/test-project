package com.sismics.reader.core.dao;

import com.sismics.reader.core.reporting.Bug;
import com.sismics.reader.core.reporting.BugReportManager;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the Bug feature.
 * Converts internal Bug objects to BugDto objects for JSON serialization.
 */
public class BugDao {

    public static List<BugDto> getAllBugs() {
        List<Bug> bugs = BugReportManager.getInstance().getBugs();
        List<BugDto> dtos = new ArrayList<>();
        for (Bug bug : bugs) {
            // Assuming Bug class has getId(), getDescription(), and getStatus() methods.
            String status = bug.getStatus() != null ? bug.getStatus().toString() : "OPEN";
            dtos.add(new BugDto(bug.getId(), bug.getDescription(), status));
        }
        return dtos;
    }
}