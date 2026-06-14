package com.healthanalyzer.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public record ReleaseInfo(
    String latestVersion,
    Instant latestDate,
    int releasesLastYear,
    double avgDaysBetweenReleases,
    boolean hasPrerelease,
    boolean isDraft,
    String releaseNotes
) {
    public boolean hasRecentRelease() {
        if (latestDate == null) return false;
        return ChronoUnit.DAYS.between(latestDate, Instant.now()) <= 90;
    }

    public boolean hasRegularCadence() {
        return releasesLastYear >= 2;
    }

    public long daysSinceLastRelease() {
        if (latestDate == null) return -1;
        return ChronoUnit.DAYS.between(latestDate, Instant.now());
    }

    public static ReleaseInfo empty() {
        return new ReleaseInfo(null, null, 0, 0.0, false, false, null);
    }
}
