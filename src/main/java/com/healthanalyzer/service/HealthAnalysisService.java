package com.healthanalyzer.service;

import com.healthanalyzer.api.GitHubClient;
import com.healthanalyzer.api.RepoDataFetcher;
import com.healthanalyzer.config.AppConfig;
import com.healthanalyzer.model.HealthReport;
import com.healthanalyzer.model.HealthScore;
import com.healthanalyzer.model.RepoMetrics;
import com.healthanalyzer.scoring.ScoringEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class HealthAnalysisService {
    private static final Logger log = LoggerFactory.getLogger(HealthAnalysisService.class);
    
    private final GitHubClient client;
    private final RepoDataFetcher fetcher;
    private final ScoringEngine scoringEngine;
    private final AppConfig config;

    public HealthAnalysisService(AppConfig config) {
        this.config = config;
        this.client = new GitHubClient(config);
        this.fetcher = new RepoDataFetcher(client);
        this.scoringEngine = new ScoringEngine();
    }

    public HealthReport analyze(String owner, String repo) {
        log.info("Analyzing repository: {}/{}", owner, repo);
        
        try {
            // Fetch all data
            RepoMetrics metrics = fetcher.fetchAll(owner, repo);
            
            // Calculate score
            HealthScore score = scoringEngine.calculateScore(metrics);
            
            // Build report
            HealthReport report = HealthReport.builder()
                .repoInfo(metrics.repoInfo())
                .healthScore(score)
                .metrics(metrics)
                .warnings(fetcher.getWarnings())
                .errors(fetcher.getErrors())
                .build();
            
            log.info("Analysis complete for {}/{}: Score={}, Status={}", 
                owner, repo, score.totalScore(), score.status());
            
            return report;
            
        } catch (Exception e) {
            log.error("Analysis failed for {}/{}: {}", owner, repo, e.getMessage());
            throw new RuntimeException("Failed to analyze repository: " + owner + "/" + repo, e);
        }
    }

    public List<HealthReport> analyzeBatch(List<String> repoPaths) {
        List<HealthReport> reports = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        for (String path : repoPaths) {
            try {
                String[] parts = path.split("/");
                if (parts.length >= 2) {
                    String owner = parts[parts.length - 2];
                    String repo = parts[parts.length - 1].replace(".git", "");
                    HealthReport report = analyze(owner, repo);
                    reports.add(report);
                }
            } catch (Exception e) {
                log.error("Failed to analyze {}: {}", path, e.getMessage());
                errors.add(path + ": " + e.getMessage());
            }
        }
        
        return reports;
    }

    public void close() {
        if (client != null) {
            client.close();
        }
    }
}
