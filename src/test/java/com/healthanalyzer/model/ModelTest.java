package com.healthanalyzer.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModelTest {
    
    @Test
    void testRepoInfoOwnerAndName() {
        RepoInfo repo = new RepoInfo(
            "my-org/my-project",
            "Description",
            "Java",
            100, 20, 10,
            5, 50,
            Instant.now(),
            Instant.now(),
            Instant.now(),
            false, false, false,
            "MIT",
            List.of(),
            "main",
            null
        );
        
        assertEquals("my-org", repo.owner());
        assertEquals("my-project", repo.name());
    }
    
    @Test
    void testRepoInfoTotalIssues() {
        RepoInfo repo = new RepoInfo(
            "owner/repo",
            "Description",
            null,
            0, 0, 0,
            10, 20,
            Instant.now(),
            Instant.now(),
            Instant.now(),
            false, false, false,
            null,
            List.of(),
            "main",
            null
        );
        
        assertEquals(30, repo.totalIssues());
        assertTrue(repo.hasIssues());
    }
    
    @Test
    void testIssueStatsOpenRatio() {
        IssueStats stats = new IssueStats(10, 90, 5, 5, 2.0, 3, 0);
        
        assertEquals(0.1, stats.openRatio(), 0.001);
        assertEquals(100, stats.total());
        assertFalse(stats.hasBacklog());
    }
    
    @Test
    void testIssueStatsWithBacklog() {
        IssueStats stats = new IssueStats(10, 90, 0, 0, 0, 10, 5);
        
        assertTrue(stats.hasBacklog());
    }
    
    @Test
    void testReleaseInfoDaysSinceLastRelease() {
        Instant twoWeeksAgo = Instant.now().minus(14, ChronoUnit.DAYS);
        ReleaseInfo release = new ReleaseInfo("v1.0", twoWeeksAgo, 5, 30.0, false, false, null);
        
        assertTrue(release.daysSinceLastRelease() >= 13 && release.daysSinceLastRelease() <= 15);
        assertTrue(release.hasRecentRelease());
        assertTrue(release.hasRegularCadence());
    }
    
    @Test
    void testReleaseInfoEmpty() {
        ReleaseInfo empty = ReleaseInfo.empty();
        
        assertNull(empty.latestVersion());
        assertEquals(-1, empty.daysSinceLastRelease());
        assertFalse(empty.hasRecentRelease());
    }
    
    @Test
    void testDocumentationInfoScore() {
        DocumentationInfo docs = new DocumentationInfo(
            true, "README.md", 5000,
            true, // contributing
            true, // code of conduct
            true, // issue template
            true, // PR template
            true, // changelog
            true, "MIT", // license
            true, // docs folder
            false, false
        );
        
        // Score: README(2) + CONTRIBUTING(2) + COC(1) + IssueTemplate(1) + PRTemplate(1) + Changelog(1) + License(1) + docs(1) = 10
        assertEquals(10, docs.score());
        assertTrue(docs.isComprehensive());
    }
    
    @Test
    void testDependencyHealthEmpty() {
        DependencyHealth empty = new DependencyHealth(List.of(), 0, 0, 0, 0, false);
        
        assertTrue(empty.isHealthy());
        assertEquals(0, empty.totalDependencies());
        assertEquals(0.0, empty.outdatedRatio());
    }
    
    @Test
    void testHealthScoreIsHealthy() {
        HealthScore healthy = new HealthScore(
            75, HealthStatus.ACTIVE,
            20, 15, 10, 12, 8, 10,
            List.of(), "Test summary"
        );
        
        assertTrue(healthy.isHealthy());
        assertFalse(healthy.needsAttention());
    }
    
    @Test
    void testHealthScoreNeedsAttention() {
        HealthScore unhealthy = new HealthScore(
            30, HealthStatus.SLOW,
            5, 5, 5, 5, 5, 5,
            List.of(), "Needs attention"
        );
        
        assertFalse(unhealthy.isHealthy());
        assertTrue(unhealthy.needsAttention());
    }
    
    @Test
    void testHealthStatusLabels() {
        assertEquals("Active", HealthStatus.ACTIVE.label());
        assertEquals("Maintained", HealthStatus.MAINTAINED.label());
        assertEquals("Slow", HealthStatus.SLOW.label());
        assertEquals("Dead", HealthStatus.DEAD.label());
    }
    
    @Test
    void testPullRequestStatsEmpty() {
        PullRequestStats empty = PullRequestStats.empty();
        
        assertEquals(0, empty.total());
        assertFalse(empty.hasGoodMergeRate());
        assertFalse(empty.hasBacklog());
    }
    
    @Test
    void testCommitActivityIsRecent() {
        Instant recent = Instant.now().minus(30, ChronoUnit.DAYS);
        CommitActivity recentCommit = new CommitActivity(
            "abc123", "Fix bug", "author", recent, 1, 10, 5
        );
        
        assertTrue(recentCommit.isRecent());
        
        Instant old = Instant.now().minus(180, ChronoUnit.DAYS);
        CommitActivity oldCommit = new CommitActivity(
            "def456", "Old commit", "author", old, 1, 0, 0
        );
        
        assertFalse(oldCommit.isRecent());
    }
}
