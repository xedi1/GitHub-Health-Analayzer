package com.healthanalyzer.model;

import java.time.Instant;

public record CommitActivity(
    String sha,
    String message,
    String author,
    Instant timestamp,
    int week,
    int additions,
    int deletions
) {
    public boolean isRecent() {
        if (timestamp == null) return false;
        return timestamp.isAfter(Instant.now().minusSeconds(90 * 24 * 3600L)); // 90 days
    }
}
