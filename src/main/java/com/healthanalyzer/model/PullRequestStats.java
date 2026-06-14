package com.healthanalyzer.model;

public record PullRequestStats(
    int totalOpen,
    int totalClosed,
    int recentlyMerged,
    int recentlyOpened,
    double avgMergeTimeDays,
    int stalePRs,
    double mergeRate
) {
    public int total() {
        return totalOpen + totalClosed;
    }

    public boolean hasGoodMergeRate() {
        return mergeRate >= 0.7;
    }

    public boolean hasBacklog() {
        return stalePRs > 3;
    }

    public static PullRequestStats empty() {
        return new PullRequestStats(0, 0, 0, 0, 0.0, 0, 0.0);
    }
}
