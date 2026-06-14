package com.healthanalyzer.model;

import java.util.List;

public record HealthScore(
    int totalScore,
    HealthStatus status,
    int activityScore,
    int contributorScore,
    int issueScore,
    int documentationScore,
    int releaseScore,
    int popularityScore,
    List<HealthSignal> signals,
    String summary
) {
    public boolean isHealthy() {
        return totalScore >= 60;
    }

    public boolean needsAttention() {
        return totalScore < 40;
    }

    public boolean isZombie() {
        return status == HealthStatus.DEAD && 
               activityScore < 10 && 
               contributorScore < 10;
    }

    public int busFactor() {
        // Returns the minimum number of contributors for 80% of contributions
        return Math.max(1, contributorScore / 10);
    }
}
