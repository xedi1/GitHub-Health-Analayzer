package com.healthanalyzer.model;

import java.time.Instant;
import java.util.List;

public record RepoInfo(
    String fullName,
    String description,
    String language,
    int stars,
    int forks,
    int watchers,
    int openIssues,
    int closedIssues,
    Instant createdAt,
    Instant updatedAt,
    Instant pushedAt,
    boolean archived,
    boolean disabled,
    boolean fork,
    String license,
    List<String> topics,
    String defaultBranch,
    String homePage
) {
    public int totalIssues() {
        return openIssues + closedIssues;
    }

    public boolean hasIssues() {
        return openIssues > 0 || closedIssues > 0;
    }

    public String owner() {
        int idx = fullName.indexOf('/');
        return idx > 0 ? fullName.substring(0, idx) : fullName;
    }

    public String name() {
        int idx = fullName.indexOf('/');
        return idx > 0 ? fullName.substring(idx + 1) : fullName;
    }
}
