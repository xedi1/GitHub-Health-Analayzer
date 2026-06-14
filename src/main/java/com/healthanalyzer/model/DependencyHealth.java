package com.healthanalyzer.model;

import java.util.List;

public record DependencyHealth(
    List<String> manifestFiles,
    int totalDependencies,
    int directDependencies,
    int outdatedCount,
    int deprecatedCount,
    boolean hasVulnerabilities
) {
    public boolean isHealthy() {
        if (hasVulnerabilities) return false;
        if (totalDependencies == 0) return true; // No dependencies means no issues
        return outdatedCount < totalDependencies * 0.5;
    }

    public double outdatedRatio() {
        return totalDependencies == 0 ? 0.0 : (double) outdatedCount / totalDependencies;
    }

    public static DependencyHealth empty() {
        return new DependencyHealth(List.of(), 0, 0, 0, 0, false);
    }
}
