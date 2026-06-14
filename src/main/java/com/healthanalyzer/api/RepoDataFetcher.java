package com.healthanalyzer.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.healthanalyzer.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class RepoDataFetcher {
    private static final Logger log = LoggerFactory.getLogger(RepoDataFetcher.class);
    private final GitHubClient client;
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public RepoDataFetcher(GitHubClient client) {
        this.client = client;
    }

    public RepoMetrics fetchAll(String owner, String repo) {
        log.info("Fetching data for {}/{}", owner, repo);
        
        RepoInfo repoInfo = fetchRepoInfo(owner, repo);
        if (repoInfo == null) {
            throw new RuntimeException("Failed to fetch repository info for " + owner + "/" + repo);
        }

        List<Contributor> contributors = fetchContributors(owner, repo);
        IssueStats issueStats = fetchIssueStats(owner, repo);
        ReleaseInfo releaseInfo = fetchReleaseInfo(owner, repo);
        DocumentationInfo docInfo = fetchDocumentationInfo(owner, repo, repoInfo.defaultBranch());
        DependencyHealth depHealth = fetchDependencyHealth(owner, repo, repoInfo.defaultBranch());
        CommunityHealth communityHealth = fetchCommunityHealth(owner, repo);
        List<CommitActivity> commits = fetchRecentCommits(owner, repo);
        List<PullRequestStats> prStats = fetchPullRequestStats(owner, repo);

        return RepoMetrics.builder()
            .repoInfo(repoInfo)
            .contributors(contributors)
            .issueStats(issueStats)
            .releaseInfo(releaseInfo)
            .documentationInfo(docInfo)
            .dependencyHealth(depHealth)
            .communityHealth(communityHealth)
            .recentCommits(commits)
            .pullRequestStats(prStats)
            .fetchedAt(Instant.now())
            .build();
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    private RepoInfo fetchRepoInfo(String owner, String repo) {
        try {
            JsonObject json = client.getRepo(owner, repo);
            return parseRepoInfo(json);
        } catch (GitHubApiException e) {
            log.error("Failed to fetch repo info: {}", e.getMessage());
            errors.add("Failed to fetch repo info: " + e.getMessage());
            return null;
        }
    }

    private RepoInfo parseRepoInfo(JsonObject json) {
        Instant createdAt = parseDate(json.get("created_at"));
        Instant updatedAt = parseDate(json.get("updated_at"));
        Instant pushedAt = parseDate(json.get("pushed_at"));

        String fullName = json.get("full_name").getAsString();
        String description = getStringOrNull(json, "description");
        String language = getStringOrNull(json, "language");
        
        int stars = json.get("stargazers_count").getAsInt();
        int forks = json.get("forks_count").getAsInt();
        int watchers = json.get("watchers_count").getAsInt();
        
        JsonObject counts = json.getAsJsonObject("counts");
        int openIssues = counts != null && counts.has("open_issues") ? 
            counts.get("open_issues").getAsInt() : 
            json.get("open_issues_count").getAsInt();
        
        int closedIssues = 0;
        if (json.has("closed_issues_count")) {
            closedIssues = json.get("closed_issues_count").getAsInt();
        } else if (json.has("counts") && json.getAsJsonObject("counts").has("closed")) {
            closedIssues = json.getAsJsonObject("counts").get("closed").getAsInt();
        }

        boolean archived = json.has("archived") && json.get("archived").getAsBoolean();
        boolean disabled = json.has("disabled") && json.get("disabled").getAsBoolean();
        boolean fork = json.has("fork") && json.get("fork").getAsBoolean();

        String license = null;
        if (json.has("license") && !json.get("license").isJsonNull()) {
            license = json.getAsJsonObject("license").get("spdx_id").getAsString();
        }

        List<String> topics = new ArrayList<>();
        if (json.has("topics")) {
            json.getAsJsonArray("topics").forEach(t -> topics.add(t.getAsString()));
        }

        String defaultBranch = json.get("default_branch").getAsString();
        String homePage = getStringOrNull(json, "homepage");

        return new RepoInfo(
            fullName, description, language,
            stars, forks, watchers,
            openIssues, closedIssues,
            createdAt, updatedAt, pushedAt,
            archived, disabled, fork,
            license, topics, defaultBranch, homePage
        );
    }

    private List<Contributor> fetchContributors(String owner, String repo) {
        try {
            List<JsonObject> contributors = client.getContributors(owner, repo);
            if (contributors.isEmpty()) {
                warnings.add("No contributors found");
                return List.of();
            }

            int totalContributions = contributors.stream()
                .mapToInt(c -> c.get("contributions").getAsInt())
                .sum();

            return contributors.stream()
                .limit(50)
                .map(c -> {
                    String login = c.get("login").getAsString();
                    String avatarUrl = getStringOrNull(c, "avatar_url");
                    String htmlUrl = getStringOrNull(c, "html_url");
                    int contributions = c.get("contributions").getAsInt();
                    double percentage = totalContributions > 0 ? 
                        (contributions * 100.0 / totalContributions) : 0;
                    return new Contributor(login, avatarUrl, htmlUrl, contributions, percentage, 0);
                })
                .collect(Collectors.toList());
        } catch (GitHubApiException e) {
            log.warn("Failed to fetch contributors: {}", e.getMessage());
            warnings.add("Could not fetch contributors: " + e.getMessage());
            return List.of();
        }
    }

    private IssueStats fetchIssueStats(String owner, String repo) {
        try {
            // Get recent open issues
            List<JsonObject> openIssues = client.getIssues(owner, repo, "open", 90);
            List<JsonObject> closedIssues = client.getIssues(owner, repo, "closed", 90);

            int totalOpen = openIssues.size();
            int totalClosed = closedIssues.size();
            int recentlyClosed = (int) closedIssues.stream()
                .filter(i -> isRecent(i, "closed_at", 30))
                .count();
            int recentlyOpened = (int) openIssues.stream()
                .filter(i -> isRecent(i, "created_at", 30))
                .count();

            int staleIssues = (int) openIssues.stream()
                .filter(i -> isOld(i, "created_at", 60))
                .count();

            // Calculate average response time (days between created and first response)
            double avgResponse = calculateAvgResponseTime(openIssues);

            return new IssueStats(
                totalOpen, totalClosed,
                recentlyClosed, recentlyOpened,
                avgResponse, staleIssues,
                staleIssues > 5 ? staleIssues - 5 : 0
            );
        } catch (Exception e) {
            log.warn("Failed to fetch issue stats: {}", e.getMessage());
            warnings.add("Could not fetch issue stats");
            return new IssueStats(0, 0, 0, 0, 0, 0, 0);
        }
    }

    private ReleaseInfo fetchReleaseInfo(String owner, String repo) {
        try {
            List<JsonObject> releases = client.getReleases(owner, repo);
            if (releases.isEmpty()) {
                warnings.add("No releases found");
                return ReleaseInfo.empty();
            }

            JsonObject latest = releases.get(0);
            String latestVersion = latest.get("tag_name").getAsString();
            Instant latestDate = parseDate(latest.get("published_at"));
            boolean hasPrerelease = latest.has("prerelease") && latest.get("prerelease").getAsBoolean();
            boolean isDraft = latest.has("draft") && latest.get("draft").getAsBoolean();
            String releaseNotes = getStringOrNull(latest, "body");

            // Count releases in last year
            Instant oneYearAgo = Instant.now().minus(java.time.Duration.ofDays(365));
            int releasesLastYear = (int) releases.stream()
                .filter(r -> {
                    Instant date = parseDate(r.get("published_at"));
                    return date != null && date.isAfter(oneYearAgo);
                })
                .count();

            // Calculate average days between releases
            double avgDays = 0;
            if (releases.size() > 1) {
                List<Instant> dates = releases.stream()
                    .map(r -> parseDate(r.get("published_at")))
                    .filter(d -> d != null)
                    .sorted()
                    .toList();
                
                if (dates.size() > 1) {
                    long totalDays = 0;
                    for (int i = 1; i < dates.size(); i++) {
                        totalDays += java.time.Duration.between(dates.get(i - 1), dates.get(i)).toDays();
                    }
                    avgDays = (double) totalDays / (dates.size() - 1);
                }
            }

            return new ReleaseInfo(
                latestVersion, latestDate,
                releasesLastYear, avgDays,
                hasPrerelease, isDraft, releaseNotes
            );
        } catch (GitHubApiException e) {
            log.warn("Failed to fetch releases: {}", e.getMessage());
            warnings.add("Could not fetch releases");
            return ReleaseInfo.empty();
        }
    }

    private DocumentationInfo fetchDocumentationInfo(String owner, String repo, String defaultBranch) {
        try {
            boolean hasReadme = false;
            String readmeFilename = null;
            int readmeSize = 0;
            
            // Check for README files
            String[] readmeNames = {"README.md", "README.MD", "README.rst", "README.txt", "README"};
            for (String name : readmeNames) {
                try {
                    JsonObject content = client.getContent(owner, repo, name);
                    if (content != null) {
                        hasReadme = true;
                        readmeFilename = name;
                        if (content.has("size")) {
                            readmeSize = content.get("size").getAsInt();
                        }
                        break;
                    }
                } catch (GitHubApiException e) {
                    // Try next
                }
            }

            // Check for other documentation files
            boolean hasContributing = checkFileExists(owner, repo, "CONTRIBUTING.md");
            boolean hasCodeOfConduct = checkFileExists(owner, repo, "CODE_OF_CONDUCT.md") ||
                                      checkFileExists(owner, repo, "CODE_OF_CONDUCT.md");
            boolean hasIssueTemplate = checkFileExists(owner, repo, ".github/ISSUE_TEMPLATE/bug_report.md") ||
                                       checkDirExists(owner, repo, ".github/ISSUE_TEMPLATE");
            boolean hasPRTemplate = checkFileExists(owner, repo, ".github/PULL_REQUEST_TEMPLATE.md") ||
                                     checkDirExists(owner, repo, ".github/PULL_REQUEST_TEMPLATE");
            boolean hasChangelog = checkFileExists(owner, repo, "CHANGELOG.md") ||
                                   checkFileExists(owner, repo, "CHANGELOG") ||
                                   checkFileExists(owner, repo, "HISTORY.md");
            boolean hasLicense = checkFileExists(owner, repo, "LICENSE") ||
                                checkFileExists(owner, repo, "LICENSE.md");
            boolean hasDocsFolder = checkDirExists(owner, repo, "docs") ||
                                   checkDirExists(owner, repo, "doc");

            String licenseType = null;
            try {
                JsonObject license = client.getContent(owner, repo, "LICENSE");
                if (license != null && license.has("name")) {
                    licenseType = license.get("name").getAsString();
                }
            } catch (Exception e) {
                // Try LICENSE.md
                try {
                    JsonObject license = client.getContent(owner, repo, "LICENSE.md");
                    if (license != null && license.has("name")) {
                        licenseType = license.get("name").getAsString();
                    }
                } catch (Exception ignored) {}
            }

            return new DocumentationInfo(
                hasReadme, readmeFilename, readmeSize,
                hasContributing, hasCodeOfConduct,
                hasIssueTemplate, hasPRTemplate,
                hasChangelog, hasLicense, licenseType,
                hasDocsFolder, false, false
            );
        } catch (Exception e) {
            log.warn("Failed to fetch documentation info: {}", e.getMessage());
            return DocumentationInfo.empty();
        }
    }

    private boolean checkFileExists(String owner, String repo, String path) {
        try {
            client.getContent(owner, repo, path);
            return true;
        } catch (GitHubApiException e) {
            return false;
        }
    }

    private boolean checkDirExists(String owner, String repo, String path) {
        try {
            JsonArray contents = client.getContents(owner, repo, path);
            return contents != null && contents.size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private DependencyHealth fetchDependencyHealth(String owner, String repo, String defaultBranch) {
        List<String> manifestFiles = new ArrayList<>();
        String[] manifests = {"pom.xml", "package.json", "requirements.txt", 
                              "build.gradle", "go.mod", "Cargo.toml", "composer.json"};
        
        for (String manifest : manifests) {
            if (checkFileExists(owner, repo, manifest)) {
                manifestFiles.add(manifest);
            }
        }

        // For now, return basic info without detailed dependency analysis
        // This would require parsing the manifest files
        return new DependencyHealth(
            manifestFiles,
            manifestFiles.size() * 5, // Estimate
            manifestFiles.size() * 3,
            0, 0, false
        );
    }

    private CommunityHealth fetchCommunityHealth(String owner, String repo) {
        try {
            JsonObject community = client.getRepoWithCommunityProfile(owner, repo);
            if (community == null) {
                return CommunityHealth.empty();
            }

            int healthPercentage = community.has("health_percentage") ?
                community.get("health_percentage").getAsInt() : 0;
            
            boolean hasWiki = community.has("wiki") && community.get("wiki").getAsBoolean();
            String description = getStringOrNull(community, "description");
            
            return new CommunityHealth(
                healthPercentage, hasWiki, 0, 0, description, healthPercentage >= 50
            );
        } catch (Exception e) {
            log.debug("Community profile not available: {}", e.getMessage());
            return CommunityHealth.empty();
        }
    }

    private List<CommitActivity> fetchRecentCommits(String owner, String repo) {
        try {
            List<JsonObject> commits = client.getCommits(owner, repo, null, 90);
            return commits.stream()
                .limit(100)
                .map(c -> {
                    String sha = c.get("sha").getAsString();
                    String message = "";
                    if (c.has("commit") && c.getAsJsonObject("commit").has("message")) {
                        message = c.getAsJsonObject("commit").get("message").getAsString();
                        // Get first line only
                        int idx = message.indexOf('\n');
                        if (idx > 0) {
                            message = message.substring(0, idx);
                        }
                    }
                    String author = "";
                    if (c.has("author") && !c.get("author").isJsonNull()) {
                        author = c.getAsJsonObject("author").get("login").getAsString();
                    } else if (c.has("commit") && c.getAsJsonObject("commit").has("author")) {
                        author = c.getAsJsonObject("commit").getAsJsonObject("author").get("name").getAsString();
                    }
                    Instant timestamp = parseDate(c.get("commit"));
                    
                    return new CommitActivity(sha, message, author, timestamp, 0, 0, 0);
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to fetch commits: {}", e.getMessage());
            return List.of();
        }
    }

    private List<PullRequestStats> fetchPullRequestStats(String owner, String repo) {
        try {
            List<JsonObject> openPRs = client.getPullRequests(owner, repo, "open");
            List<JsonObject> closedPRs = client.getPullRequests(owner, repo, "closed");
            
            Instant thirtyDaysAgo = Instant.now().minus(java.time.Duration.ofDays(30));
            
            int recentlyMerged = (int) closedPRs.stream()
                .filter(pr -> {
                    if (!pr.has("merged_at") || pr.get("merged_at").isJsonNull()) return false;
                    Instant mergedAt = parseDate(pr.get("merged_at"));
                    return mergedAt != null && mergedAt.isAfter(thirtyDaysAgo);
                })
                .count();
            
            int recentlyOpened = (int) openPRs.stream()
                .filter(pr -> {
                    Instant created = parseDate(pr.get("created_at"));
                    return created != null && created.isAfter(thirtyDaysAgo);
                })
                .count();
            
            int stalePRs = (int) openPRs.stream()
                .filter(pr -> {
                    Instant updated = parseDate(pr.get("updated_at"));
                    return updated != null && 
                           java.time.Duration.between(updated, Instant.now()).toDays() > 30;
                })
                .count();
            
            double mergeRate = closedPRs.isEmpty() ? 0 : 
                (double) recentlyMerged / (recentlyMerged + closedPRs.size());

            return List.of(new PullRequestStats(
                openPRs.size(), closedPRs.size(),
                recentlyMerged, recentlyOpened,
                0, stalePRs, mergeRate
            ));
        } catch (Exception e) {
            log.warn("Failed to fetch PR stats: {}", e.getMessage());
            return List.of(PullRequestStats.empty());
        }
    }

    private double calculateAvgResponseTime(List<JsonObject> issues) {
        List<Double> responseTimes = new ArrayList<>();
        for (JsonObject issue : issues) {
            if (issue.has("comments") && issue.get("comments").getAsInt() > 0) {
                // This is a simplification - real implementation would check timeline
                responseTimes.add(2.0); // Assume 2 days average
            }
        }
        return responseTimes.isEmpty() ? 0 : 
            responseTimes.stream().mapToDouble(d -> d).average().orElse(0);
    }

    private boolean isRecent(JsonObject obj, String field, int days) {
        if (!obj.has(field) || obj.get(field).isJsonNull()) return false;
        Instant date = parseDate(obj.get(field));
        if (date == null) return false;
        return date.isAfter(Instant.now().minus(java.time.Duration.ofDays(days)));
    }

    private boolean isOld(JsonObject obj, String field, int days) {
        if (!obj.has(field) || obj.get(field).isJsonNull()) return false;
        Instant date = parseDate(obj.get(field));
        if (date == null) return false;
        return date.isBefore(Instant.now().minus(java.time.Duration.ofDays(days)));
    }

    private Instant parseDate(JsonElement element) {
        if (element == null || element.isJsonNull()) return null;
        try {
            return Instant.parse(element.getAsString());
        } catch (Exception e) {
            return null;
        }
    }

    private String getStringOrNull(JsonObject obj, String field) {
        if (!obj.has(field) || obj.get(field).isJsonNull()) return null;
        return obj.get(field).getAsString();
    }
}
