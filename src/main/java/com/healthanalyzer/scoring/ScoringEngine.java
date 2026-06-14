package com.healthanalyzer.scoring;

import com.healthanalyzer.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ScoringEngine {
    private static final Logger log = LoggerFactory.getLogger(ScoringEngine.class);
    
    // Weights for each dimension
    private static final int ACTIVITY_WEIGHT = 25;
    private static final int CONTRIBUTOR_WEIGHT = 20;
    private static final int ISSUE_WEIGHT = 15;
    private static final int DOCUMENTATION_WEIGHT = 15;
    private static final int RELEASE_WEIGHT = 10;
    private static final int POPULARITY_WEIGHT = 15;

    public HealthScore calculateScore(RepoMetrics metrics) {
        log.info("Calculating health score for {}", metrics.repoInfo().fullName());
        
        List<HealthSignal> signals = new ArrayList<>();
        
        // Calculate dimension scores
        int activityScore = calculateActivityScore(metrics, signals);
        int contributorScore = calculateContributorScore(metrics, signals);
        int issueScore = calculateIssueScore(metrics, signals);
        int documentationScore = calculateDocumentationScore(metrics, signals);
        int releaseScore = calculateReleaseScore(metrics, signals);
        int popularityScore = calculatePopularityScore(metrics, signals);
        
        // Calculate total score
        int totalScore = activityScore + contributorScore + issueScore + 
                         documentationScore + releaseScore + popularityScore;
        
        // Determine status
        HealthStatus status = determineStatus(totalScore, metrics, signals);
        
        // Generate summary
        String summary = generateSummary(status, metrics, signals);
        
        return new HealthScore(
            totalScore,
            status,
            activityScore,
            contributorScore,
            issueScore,
            documentationScore,
            releaseScore,
            popularityScore,
            signals,
            summary
        );
    }

    private int calculateActivityScore(RepoMetrics metrics, List<HealthSignal> signals) {
        int score = 0;
        RepoInfo repo = metrics.repoInfo();
        List<CommitActivity> commits = metrics.recentCommits();
        
        // Last push recency
        if (repo.pushedAt() != null) {
            long daysSincePush = Duration.between(repo.pushedAt(), Instant.now()).toDays();
            
            if (daysSincePush <= 7) {
                score += 10;
                signals.add(HealthSignal.builder()
                    .category("activity")
                    .message("Recent push within 7 days")
                    .positive(true)
                    .scoreImpact(10)
                    .details("Last push: " + daysSincePush + " days ago")
                    .build());
            } else if (daysSincePush <= 30) {
                score += 7;
                signals.add(HealthSignal.builder()
                    .category("activity")
                    .message("Recent push within 30 days")
                    .positive(true)
                    .scoreImpact(7)
                    .details("Last push: " + daysSincePush + " days ago")
                    .build());
            } else if (daysSincePush <= 90) {
                score += 4;
                signals.add(HealthSignal.builder()
                    .category("activity")
                    .message("Push within 3 months")
                    .positive(true)
                    .scoreImpact(4)
                    .details("Last push: " + daysSincePush + " days ago")
                    .build());
            } else if (daysSincePush <= 365) {
                score += 2;
                signals.add(HealthSignal.builder()
                    .category("activity")
                    .message("Last push over 3 months ago")
                    .positive(false)
                    .scoreImpact(-3)
                    .details("Last push: " + daysSincePush + " days ago")
                    .build());
            } else {
                signals.add(HealthSignal.builder()
                    .category("activity")
                    .message("No recent activity (over 1 year)")
                    .positive(false)
                    .scoreImpact(-8)
                    .details("Last push: " + daysSincePush + " days ago")
                    .build());
            }
        } else {
            signals.add(HealthSignal.builder()
                .category("activity")
                .message("No push date available")
                .positive(false)
                .scoreImpact(-5)
                .details("Cannot determine activity")
                .build());
        }
        
        // Commit frequency
        if (!commits.isEmpty()) {
            long recentCommits = commits.stream()
                .filter(c -> c.timestamp() != null && 
                    c.timestamp().isAfter(Instant.now().minus(Duration.ofDays(30))))
                .count();
            
            if (recentCommits > 20) {
                score += 8;
                signals.add(HealthSignal.builder()
                    .category("activity")
                    .message("High commit frequency")
                    .positive(true)
                    .scoreImpact(8)
                    .details(recentCommits + " commits in last 30 days")
                    .build());
            } else if (recentCommits > 10) {
                score += 5;
            } else if (recentCommits > 5) {
                score += 3;
            } else if (recentCommits > 0) {
                score += 1;
            }
        }
        
        // Normalize to weight
        return Math.min(ACTIVITY_WEIGHT, score);
    }

    private int calculateContributorScore(RepoMetrics metrics, List<HealthSignal> signals) {
        int score = 0;
        List<Contributor> contributors = metrics.contributors();
        
        if (contributors.isEmpty()) {
            signals.add(HealthSignal.builder()
                .category("contributors")
                .message("No contributors found")
                .positive(false)
                .scoreImpact(-10)
                .details("Unable to determine contributor diversity")
                .build());
            return 0;
        }
        
        // Number of contributors
        if (contributors.size() >= 10) {
            score += 8;
            signals.add(HealthSignal.builder()
                .category("contributors")
                .message("Large contributor base")
                .positive(true)
                .scoreImpact(8)
                .details(contributors.size() + " contributors")
                .build());
        } else if (contributors.size() >= 5) {
            score += 5;
        } else if (contributors.size() >= 2) {
            score += 3;
        } else {
            signals.add(HealthSignal.builder()
                .category("contributors")
                .message("Very few contributors")
                .positive(false)
                .scoreImpact(-5)
                .details("Only " + contributors.size() + " contributor(s)")
                .build());
        }
        
        // Bus factor (top contributor percentage)
        if (!contributors.isEmpty()) {
            double topContributorPct = contributors.get(0).percentage();
            
            if (topContributorPct >= 80) {
                signals.add(HealthSignal.builder()
                    .category("contributors")
                    .message("High concentration - single maintainer risk")
                    .positive(false)
                    .scoreImpact(-8)
                    .details(String.format("Top contributor: %.0f%% of contributions", topContributorPct))
                    .build());
            } else if (topContributorPct >= 60) {
                signals.add(HealthSignal.builder()
                    .category("contributors")
                    .message("Moderate concentration")
                    .positive(false)
                    .scoreImpact(-4)
                    .details(String.format("Top contributor: %.0f%% of contributions", topContributorPct))
                    .build());
            } else if (topContributorPct >= 40) {
                score += 4;
            } else {
                score += 6;
                signals.add(HealthSignal.builder()
                    .category("contributors")
                    .message("Good contributor distribution")
                    .positive(true)
                    .scoreImpact(6)
                    .details(String.format("Top contributor: %.0f%% of contributions", topContributorPct))
                    .build());
            }
        }
        
        return Math.min(CONTRIBUTOR_WEIGHT, score);
    }

    private int calculateIssueScore(RepoMetrics metrics, List<HealthSignal> signals) {
        int score = ISSUE_WEIGHT;
        IssueStats issues = metrics.issueStats();
        
        if (issues.total() == 0) {
            signals.add(HealthSignal.builder()
                .category("issues")
                .message("No issue activity")
                .positive(false)
                .scoreImpact(-5)
                .details("No issues found")
                .build());
            return score - 5;
        }
        
        // Stale issues penalty
        if (issues.staleIssues() > 10) {
            score -= 6;
            signals.add(HealthSignal.builder()
                .category("issues")
                .message("Many stale issues")
                .positive(false)
                .scoreImpact(-6)
                .details(issues.staleIssues() + " issues open > 60 days")
                .build());
        } else if (issues.staleIssues() > 5) {
            score -= 3;
        }
        
        // Backlog penalty
        if (issues.hasBacklog()) {
            score -= 2;
            signals.add(HealthSignal.builder()
                .category("issues")
                .message("Significant issue backlog")
                .positive(false)
                .scoreImpact(-2)
                .details("Backlog size: " + issues.backlogSize())
                .build());
        }
        
        // Open/closed ratio
        double openRatio = issues.openRatio();
        if (openRatio > 0.9) {
            score -= 4;
            signals.add(HealthSignal.builder()
                .category("issues")
                .message("Very high open issue ratio")
                .positive(false)
                .scoreImpact(-4)
                .details(String.format("%.0f%% of issues are still open", openRatio * 100))
                .build());
        } else if (openRatio > 0.7) {
            score -= 2;
        }
        
        // Good closing rate bonus
        if (issues.recentlyClosed() > issues.recentlyOpened()) {
            score += 3;
            signals.add(HealthSignal.builder()
                .category("issues")
                .message("Good issue resolution rate")
                .positive(true)
                .scoreImpact(3)
                .details("Closed " + issues.recentlyClosed() + " vs opened " + issues.recentlyOpened())
                .build());
        }
        
        return Math.max(0, Math.min(ISSUE_WEIGHT, score));
    }

    private int calculateDocumentationScore(RepoMetrics metrics, List<HealthSignal> signals) {
        int score = 0;
        DocumentationInfo docs = metrics.documentationInfo();
        
        if (docs.hasReadme()) {
            score += 4;
            if (docs.readmeSize() > 1000) {
                signals.add(HealthSignal.builder()
                    .category("documentation")
                    .message("Comprehensive README")
                    .positive(true)
                    .scoreImpact(4)
                    .details("README size: " + docs.readmeSize() + " bytes")
                    .build());
            }
        } else {
            signals.add(HealthSignal.builder()
                .category("documentation")
                .message("No README found")
                .positive(false)
                .scoreImpact(-4)
                .details("README is essential for project understanding")
                .build());
        }
        
        if (docs.hasContributing()) {
            score += 3;
            signals.add(HealthSignal.builder()
                .category("documentation")
                .message("Has CONTRIBUTING guide")
                .positive(true)
                .scoreImpact(3)
                .details("Encourages community contributions")
                .build());
        }
        
        if (docs.hasCodeOfConduct()) {
            score += 2;
            signals.add(HealthSignal.builder()
                .category("documentation")
                .message("Has CODE_OF_CONDUCT")
                .positive(true)
                .scoreImpact(2)
                .details("Promotes healthy community")
                .build());
        }
        
        if (docs.hasIssueTemplate()) {
            score += 2;
        }
        
        if (docs.hasPRTemplate()) {
            score += 2;
        }
        
        if (docs.hasLicense()) {
            score += 2;
            signals.add(HealthSignal.builder()
                .category("documentation")
                .message("Has license")
                .positive(true)
                .scoreImpact(2)
                .details("License: " + docs.licenseType())
                .build());
        } else {
            signals.add(HealthSignal.builder()
                .category("documentation")
                .message("No license found")
                .positive(false)
                .scoreImpact(-2)
                .details("Project should have a license")
                .build());
        }
        
        if (docs.hasDocsFolder()) {
            score += 2;
        }
        
        return Math.min(DOCUMENTATION_WEIGHT, score);
    }

    private int calculateReleaseScore(RepoMetrics metrics, List<HealthSignal> signals) {
        int score = 0;
        ReleaseInfo releases = metrics.releaseInfo();
        
        if (releases.latestDate() == null) {
            signals.add(HealthSignal.builder()
                .category("releases")
                .message("No releases found")
                .positive(false)
                .scoreImpact(-5)
                .details("No published releases")
                .build());
            return 0;
        }
        
        // Days since last release
        long daysSinceRelease = releases.daysSinceLastRelease();
        
        if (daysSinceRelease <= 90) {
            score += 4;
            signals.add(HealthSignal.builder()
                .category("releases")
                .message("Recent release")
                .positive(true)
                .scoreImpact(4)
                .details("Last release: " + daysSinceRelease + " days ago")
                .build());
        } else if (daysSinceRelease <= 180) {
            score += 2;
        } else if (daysSinceRelease <= 365) {
            score += 1;
        } else {
            signals.add(HealthSignal.builder()
                .category("releases")
                .message("Old release")
                .positive(false)
                .scoreImpact(-3)
                .details("Last release: " + daysSinceRelease + " days ago")
                .build());
        }
        
        // Release cadence
        if (releases.hasRegularCadence()) {
            score += 4;
            signals.add(HealthSignal.builder()
                .category("releases")
                .message("Regular release cadence")
                .positive(true)
                .scoreImpact(4)
                .details(releases.releasesLastYear() + " releases in last year")
                .build());
        }
        
        // Prerelease warning
        if (releases.hasPrerelease()) {
            signals.add(HealthSignal.builder()
                .category("releases")
                .message("Latest release is pre-release")
                .positive(false)
                .scoreImpact(-2)
                .details("May not be production-ready")
                .build());
        }
        
        return Math.min(RELEASE_WEIGHT, score);
    }

    private int calculatePopularityScore(RepoMetrics metrics, List<HealthSignal> signals) {
        int score = 0;
        RepoInfo repo = metrics.repoInfo();
        
        // Stars
        if (repo.stars() >= 1000) {
            score += 6;
            signals.add(HealthSignal.builder()
                .category("popularity")
                .message("Popular repository")
                .positive(true)
                .scoreImpact(6)
                .details(repo.stars() + " stars")
                .build());
        } else if (repo.stars() >= 100) {
            score += 4;
        } else if (repo.stars() >= 10) {
            score += 2;
        }
        
        // Forks
        if (repo.forks() >= 100) {
            score += 4;
        } else if (repo.forks() >= 10) {
            score += 2;
        }
        
        // Watchers
        if (repo.watchers() >= 50) {
            score += 2;
        }
        
        // Popular but dead detection
        if (repo.stars() > 100 && repo.pushedAt() != null) {
            long daysSincePush = Duration.between(repo.pushedAt(), Instant.now()).toDays();
            if (daysSincePush > 365 && repo.stars() > 1000) {
                signals.add(HealthSignal.builder()
                    .category("popularity")
                    .message("Zombie repo detected - popular but inactive")
                    .positive(false)
                    .scoreImpact(-10)
                    .details(repo.stars() + " stars but no activity for " + daysSincePush + " days")
                    .build());
            }
        }
        
        // Check if popularity aligns with activity
        if (repo.stars() > 50 && metrics.recentCommits().isEmpty()) {
            signals.add(HealthSignal.builder()
                .category("popularity")
                .message("High stars but no recent commits")
                .positive(false)
                .scoreImpact(-5)
                .details("Popularity does not match activity")
                .build());
        }
        
        return Math.min(POPULARITY_WEIGHT, score);
    }

    private HealthStatus determineStatus(int totalScore, RepoMetrics metrics, List<HealthSignal> signals) {
        // Check for dead indicators
        RepoInfo repo = metrics.repoInfo();
        
        if (repo.archived()) {
            signals.add(HealthSignal.builder()
                .category("status")
                .message("Repository is archived")
                .positive(false)
                .scoreImpact(-20)
                .details("Archived repositories are read-only")
                .build());
            return HealthStatus.DEAD;
        }
        
        if (repo.disabled()) {
            signals.add(HealthSignal.builder()
                .category("status")
                .message("Repository is disabled")
                .positive(false)
                .scoreImpact(-30)
                .details("Repository has been disabled")
                .build());
            return HealthStatus.DEAD;
        }
        
        // Determine status based on score and signals
        if (totalScore >= 80) {
            return HealthStatus.ACTIVE;
        } else if (totalScore >= 60) {
            return HealthStatus.MAINTAINED;
        } else if (totalScore >= 35) {
            return HealthStatus.SLOW;
        } else {
            return HealthStatus.DEAD;
        }
    }

    private String generateSummary(HealthStatus status, RepoMetrics metrics, List<HealthSignal> signals) {
        RepoInfo repo = metrics.repoInfo();
        
        long positiveSignals = signals.stream().filter(HealthSignal::positive).count();
        long negativeSignals = signals.stream().filter(s -> !s.positive()).count();
        
        return switch (status) {
            case ACTIVE -> String.format("Healthy project with %d positive and %d negative signals", 
                positiveSignals, negativeSignals);
            case MAINTAINED -> String.format("Active project with some concerns (%d positive, %d negative signals)", 
                positiveSignals, negativeSignals);
            case SLOW -> String.format("Project showing signs of decline (%d negative signals)", 
                negativeSignals);
            case DEAD -> String.format("Likely abandoned project with %d negative signals", 
                negativeSignals);
        };
    }
}
