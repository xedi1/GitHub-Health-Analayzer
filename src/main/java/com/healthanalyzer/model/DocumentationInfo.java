package com.healthanalyzer.model;

public record DocumentationInfo(
    boolean hasReadme,
    String readmeFilename,
    int readmeSize,
    boolean hasContributing,
    boolean hasCodeOfConduct,
    boolean hasIssueTemplate,
    boolean hasPRTemplate,
    boolean hasChangelog,
    boolean hasLicense,
    String licenseType,
    boolean hasDocsFolder,
    boolean hasWiki,
    boolean hasPages
) {
    public int score() {
        int score = 0;
        if (hasReadme) score += 2;
        if (hasContributing) score += 2;
        if (hasCodeOfConduct) score += 1;
        if (hasIssueTemplate) score += 1;
        if (hasPRTemplate) score += 1;
        if (hasChangelog) score += 1;
        if (hasLicense) score += 1;
        if (hasDocsFolder) score += 1;
        return score;
    }

    public boolean isComprehensive() {
        return score() >= 6;
    }

    public static DocumentationInfo empty() {
        return new DocumentationInfo(
            false, null, 0, false, false, false, false,
            false, false, null, false, false, false
        );
    }
}
