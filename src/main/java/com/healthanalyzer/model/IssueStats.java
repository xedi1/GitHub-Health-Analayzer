package com.healthanalyzer.model;

public record IssueStats(
    int totalOpen,
    int totalClosed,
    int recentlyClosed, // last 30 days
    int recentlyOpened,  // last 30 days
    double avgResponseTimeHours,
    int staleIssues,     // issues open > 60 days
    int backlogSize
) {
    public int total() {
        return totalOpen + totalClosed;
    }

    public double openRatio() {
        int total = total();
        return total == 0 ? 0.0 : (double) totalOpen / total;
    }

    public boolean hasBacklog() {
        return staleIssues > 5;
    }
}
