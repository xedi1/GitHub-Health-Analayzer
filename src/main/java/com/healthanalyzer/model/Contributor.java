package com.healthanalyzer.model;

import java.util.Comparator;

public record Contributor(
    String login,
    String avatarUrl,
    String htmlUrl,
    int contributions,
    double percentage,
    int rank
) {
    public static final Comparator<Contributor> BY_CONTRIBUTIONS = 
        Comparator.comparingInt(Contributor::contributions).reversed();
}
