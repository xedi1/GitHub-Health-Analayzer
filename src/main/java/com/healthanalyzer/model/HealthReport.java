package com.healthanalyzer.model;

import java.time.Instant;
import java.util.List;

public record HealthReport(
    RepoInfo repoInfo,
    HealthScore healthScore,
    RepoMetrics metrics,
    Instant generatedAt,
    String analysisVersion,
    List<String> warnings,
    List<String> errors
) {
    public static Builder builder() {
        return new Builder();
    }

    public String summary() {
        return String.format("[%s] %s - Score: %d/100 (%s)",
            healthScore().status().label(),
            repoInfo().fullName(),
            healthScore().totalScore(),
            healthScore().summary()
        );
    }

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public static class Builder {
        private RepoInfo repoInfo;
        private HealthScore healthScore;
        private RepoMetrics metrics;
        private Instant generatedAt = Instant.now();
        private String analysisVersion = "1.0.0";
        private List<String> warnings = List.of();
        private List<String> errors = List.of();

        public Builder repoInfo(RepoInfo repoInfo) {
            this.repoInfo = repoInfo;
            return this;
        }

        public Builder healthScore(HealthScore healthScore) {
            this.healthScore = healthScore;
            return this;
        }

        public Builder metrics(RepoMetrics metrics) {
            this.metrics = metrics;
            return this;
        }

        public Builder generatedAt(Instant generatedAt) {
            this.generatedAt = generatedAt;
            return this;
        }

        public Builder analysisVersion(String analysisVersion) {
            this.analysisVersion = analysisVersion;
            return this;
        }

        public Builder warnings(List<String> warnings) {
            this.warnings = warnings;
            return this;
        }

        public Builder errors(List<String> errors) {
            this.errors = errors;
            return this;
        }

        public HealthReport build() {
            return new HealthReport(
                repoInfo,
                healthScore,
                metrics,
                generatedAt,
                analysisVersion,
                warnings,
                errors
            );
        }
    }
}
