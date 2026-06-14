package com.healthanalyzer.scoring;

import com.healthanalyzer.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScoringEngineTest {
    
    private ScoringEngine scoringEngine;
    
    @BeforeEach
    void setUp() {
        scoringEngine = new ScoringEngine();
    }
    
    @Test
    void testActiveRepositoryGetsHighScore() {
        RepoInfo repoInfo = new RepoInfo(
            "owner/repo",
            "A great project",
            "Java",
            1000, 200, 50,
            10, 90,
            Instant.now().minus(5, ChronoUnit.DAYS), // pushed 5 days ago
            Instant.now().minus(30, ChronoUnit.DAYS),
            Instant.now().minus(5, ChronoUnit.DAYS),
            false, false, false,
            "MIT",
            List.of("java", "project"),
            "main",
            "https://example.com"
        );
        
        Contributor contributor1 = new Contributor("user1", null, null, 500, 50.0, 1);
        Contributor contributor2 = new Contributor("user2", null, null, 300, 30.0, 2);
        Contributor contributor3 = new Contributor("user3", null, null, 200, 20.0, 3);
        
        RepoMetrics metrics = RepoMetrics.builder()
            .repoInfo(repoInfo)
            .contributors(List.of(contributor1, contributor2, contributor3))
            .issueStats(new IssueStats(10, 90, 10, 5, 2.0, 0, 0))
            .releaseInfo(new ReleaseInfo("v1.0.0", Instant.now().minus(30, ChronoUnit.DAYS), 6, 60.0, false, false, "Release notes"))
            .documentationInfo(new DocumentationInfo(true, "README.md", 5000, true, true, true, true, true, true, "MIT", true, false, false))
            .build();
        
        HealthScore score = scoringEngine.calculateScore(metrics);
        
        assertTrue(score.totalScore() >= 60, "Active repo should have score >= 60");
        assertTrue(score.status() == HealthStatus.ACTIVE || score.status() == HealthStatus.MAINTAINED,
            "Active repo should be ACTIVE or MAINTAINED, got: " + score.status());
    }
    
    @Test
    void testZombieRepositoryDetection() {
        RepoInfo repoInfo = new RepoInfo(
            "owner/popular-repo",
            "Was popular but now dead",
            "JavaScript",
            5000, 1000, 100, // high popularity
            50, 100,
            Instant.now().minus(730, ChronoUnit.DAYS), // created 2 years ago
            Instant.now().minus(730, ChronoUnit.DAYS), // updated 2 years ago
            Instant.now().minus(400, ChronoUnit.DAYS), // pushed 400 days ago - no recent activity
            false, false, false,
            "MIT",
            List.of(),
            "main",
            null
        );
        
        Contributor singleContributor = new Contributor("maintainer", null, null, 5000, 95.0, 1);
        
        RepoMetrics metrics = RepoMetrics.builder()
            .repoInfo(repoInfo)
            .contributors(List.of(singleContributor))
            .issueStats(new IssueStats(50, 100, 0, 0, 0, 45, 40)) // many stale issues
            .releaseInfo(new ReleaseInfo("v0.9.0", Instant.now().minus(365, ChronoUnit.DAYS), 1, 0.0, true, false, null))
            .documentationInfo(new DocumentationInfo(false, null, 0, false, false, false, false, false, false, null, false, false, false))
            .build();
        
        HealthScore score = scoringEngine.calculateScore(metrics);
        
        assertTrue(score.totalScore() < 40, "Zombie repo should have low score");
        assertTrue(score.isZombie() || score.status() == HealthStatus.DEAD);
    }
    
    @Test
    void testSingleContributorRisk() {
        RepoInfo repoInfo = new RepoInfo(
            "owner/single-maintainer",
            "Project with one main maintainer",
            "Python",
            100, 20, 10,
            5, 20,
            Instant.now().minus(180, ChronoUnit.DAYS),
            Instant.now().minus(5, ChronoUnit.DAYS),
            Instant.now().minus(5, ChronoUnit.DAYS),
            false, false, false,
            "MIT",
            List.of(),
            "main",
            null
        );
        
        Contributor dominant = new Contributor("maintainer", null, null, 950, 95.0, 1);
        Contributor minor1 = new Contributor("contributor1", null, null, 30, 3.0, 2);
        Contributor minor2 = new Contributor("contributor2", null, null, 20, 2.0, 3);
        
        RepoMetrics metrics = RepoMetrics.builder()
            .repoInfo(repoInfo)
            .contributors(List.of(dominant, minor1, minor2))
            .issueStats(new IssueStats(5, 20, 5, 3, 1.0, 0, 0))
            .releaseInfo(new ReleaseInfo("v1.0.0", Instant.now().minus(60, ChronoUnit.DAYS), 4, 90.0, false, false, "Release"))
            .documentationInfo(new DocumentationInfo(true, "README.md", 2000, true, true, true, true, true, true, "MIT", true, false, false))
            .build();
        
        HealthScore score = scoringEngine.calculateScore(metrics);
        
        // Should detect high concentration
        assertTrue(score.contributorScore() <= 10, "Single maintainer risk should reduce score");
        
        // Check for negative signal about concentration
        boolean hasConcentrationSignal = score.signals().stream()
            .anyMatch(s -> s.category().equals("contributors") && !s.positive());
        assertTrue(hasConcentrationSignal, "Should have signal about contributor concentration");
    }
    
    @Test
    void testArchivedRepositoryIsDead() {
        RepoInfo repoInfo = new RepoInfo(
            "owner/archived-repo",
            "An archived repository",
            "Java",
            500, 100, 20,
            0, 50,
            Instant.now().minus(1000, ChronoUnit.DAYS),
            Instant.now().minus(500, ChronoUnit.DAYS),
            Instant.now().minus(500, ChronoUnit.DAYS),
            true, // archived
            false, false,
            "MIT",
            List.of(),
            "main",
            null
        );
        
        RepoMetrics metrics = RepoMetrics.builder()
            .repoInfo(repoInfo)
            .contributors(List.of())
            .build();
        
        HealthScore score = scoringEngine.calculateScore(metrics);
        
        assertEquals(HealthStatus.DEAD, score.status());
    }
    
    @Test
    void testDocumentationScore() {
        RepoInfo repoInfo = new RepoInfo(
            "owner/well-documented",
            "Well documented project",
            "Go",
            200, 50, 15,
            3, 30,
            Instant.now().minus(365, ChronoUnit.DAYS),
            Instant.now().minus(7, ChronoUnit.DAYS),
            Instant.now().minus(7, ChronoUnit.DAYS),
            false, false, false,
            "MIT",
            List.of(),
            "main",
            null
        );
        
        RepoMetrics metrics = RepoMetrics.builder()
            .repoInfo(repoInfo)
            .contributors(List.of(
                new Contributor("user1", null, null, 100, 100.0, 1)
            ))
            .documentationInfo(new DocumentationInfo(
                true, "README.md", 8000, // comprehensive README
                true, // CONTRIBUTING
                true, // CODE_OF_CONDUCT
                true, // Issue template
                true, // PR template
                true, // Changelog
                true, "MIT", // License
                true, // docs folder
                false, false
            ))
            .build();
        
        HealthScore score = scoringEngine.calculateScore(metrics);
        
        assertEquals(15, score.documentationScore(), "Full documentation should get max score");
    }
    
    @Test
    void testEmptyRepository() {
        RepoInfo repoInfo = new RepoInfo(
            "owner/empty",
            "Empty repository",
            null,
            0, 0, 0,
            0, 0,
            Instant.now(),
            Instant.now(),
            Instant.now(),
            false, false, false,
            null,
            List.of(),
            "main",
            null
        );
        
        RepoMetrics metrics = RepoMetrics.builder()
            .repoInfo(repoInfo)
            .contributors(List.of())
            .documentationInfo(new DocumentationInfo(
                false, null, 0, false, false, false, false, false, false, null, false, false, false
            ))
            .build();
        
        HealthScore score = scoringEngine.calculateScore(metrics);
        
        assertTrue(score.totalScore() < 30, "Empty repo should have low score");
        assertEquals(HealthStatus.DEAD, score.status());
    }
}
