package com.healthanalyzer.model;

import java.time.Instant;
import java.util.List;

public record RepoMetrics(
    RepoInfo repoInfo,
    List<Contributor> contributors,
    IssueStats issueStats,
    ReleaseInfo releaseInfo,
    DocumentationInfo documentationInfo,
    DependencyHealth dependencyHealth,
    CommunityHealth communityHealth,
    List<CommitActivity> recentCommits,
    List<PullRequestStats> pullRequestStats,
    Instant fetchedAt
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private RepoInfo repoInfo;
        private List<Contributor> contributors = List.of();
        private IssueStats issueStats = new IssueStats(0, 0, 0, 0, 0, 0, 0);
        private ReleaseInfo releaseInfo = new ReleaseInfo(null, null, 0, 0.0, false, false, null);
        private DocumentationInfo documentationInfo = new DocumentationInfo(false, null, 0, false, false, false, false, false, false, null, false, false, false);
        private DependencyHealth dependencyHealth = new DependencyHealth(List.of(), 0, 0, 0, 0, false);
        private CommunityHealth communityHealth = new CommunityHealth(0, false, 0, 0, null, false);
        private List<CommitActivity> recentCommits = List.of();
        private List<PullRequestStats> pullRequestStats = List.of();
        private Instant fetchedAt = Instant.now();

        public Builder repoInfo(RepoInfo repoInfo) {
            this.repoInfo = repoInfo;
            return this;
        }

        public Builder contributors(List<Contributor> contributors) {
            this.contributors = contributors;
            return this;
        }

        public Builder issueStats(IssueStats issueStats) {
            this.issueStats = issueStats;
            return this;
        }

        public Builder releaseInfo(ReleaseInfo releaseInfo) {
            this.releaseInfo = releaseInfo;
            return this;
        }

        public Builder documentationInfo(DocumentationInfo documentationInfo) {
            this.documentationInfo = documentationInfo;
            return this;
        }

        public Builder dependencyHealth(DependencyHealth dependencyHealth) {
            this.dependencyHealth = dependencyHealth;
            return this;
        }

        public Builder communityHealth(CommunityHealth communityHealth) {
            this.communityHealth = communityHealth;
            return this;
        }

        public Builder recentCommits(List<CommitActivity> recentCommits) {
            this.recentCommits = recentCommits;
            return this;
        }

        public Builder pullRequestStats(List<PullRequestStats> pullRequestStats) {
            this.pullRequestStats = pullRequestStats;
            return this;
        }

        public Builder fetchedAt(Instant fetchedAt) {
            this.fetchedAt = fetchedAt;
            return this;
        }

        public RepoMetrics build() {
            return new RepoMetrics(
                repoInfo,
                contributors,
                issueStats,
                releaseInfo,
                documentationInfo,
                dependencyHealth,
                communityHealth,
                recentCommits,
                pullRequestStats,
                fetchedAt
            );
        }
    }
}
