package com.healthanalyzer.model;

public record CommunityHealth(
    int healthPercentage,
    boolean hasWiki,
    int followerCount,
    int memberCount,
    String description,
    boolean isFriendly
) {
    public boolean isGood() {
        return healthPercentage >= 50;
    }

    public static CommunityHealth empty() {
        return new CommunityHealth(0, false, 0, 0, null, false);
    }
}
