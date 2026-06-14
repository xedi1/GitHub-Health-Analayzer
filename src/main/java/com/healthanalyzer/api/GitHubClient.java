package com.healthanalyzer.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.healthanalyzer.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GitHubClient {
    private static final Logger log = LoggerFactory.getLogger(GitHubClient.class);
    private static final String GITHUB_API_BASE = "https://api.github.com";
    private static final String GITHUB_API_VERSION = "2022-11-28";

    private final okhttp3.OkHttpClient httpClient;
    private final Gson gson;
    private final AppConfig config;
    private RateLimitInfo rateLimitInfo;

    public GitHubClient(AppConfig config) {
        this.config = config;
        this.httpClient = new okhttp3.OkHttpClient.Builder()
            .connectTimeout(config.requestTimeout())
            .readTimeout(config.requestTimeout())
            .writeTimeout(config.requestTimeout())
            .addInterceptor(chain -> {
                var request = chain.request().newBuilder()
                    .addHeader("Accept", "application/vnd.github+json")
                    .addHeader("X-GitHub-Api-Version", GITHUB_API_VERSION);
                
                if (config.githubToken() != null && !config.githubToken().isEmpty()) {
                    request.addHeader("Authorization", "Bearer " + config.githubToken());
                }
                
                return chain.proceed(request.build());
            })
            .build();
        
        this.gson = new GsonBuilder()
            .setLenient()
            .create();
        
        this.rateLimitInfo = new RateLimitInfo();
    }

    public JsonObject getRepo(String owner, String repo) throws GitHubApiException {
        String endpoint = String.format("/repos/%s/%s", owner, repo);
        return makeRequest(endpoint);
    }

    public JsonObject getRepoWithCommunityProfile(String owner, String repo) throws GitHubApiException {
        String endpoint = String.format("/repos/%s/%s/community/profile", owner, repo);
        try {
            return makeRequest(endpoint);
        } catch (GitHubApiException e) {
            if (e.isNotFound()) {
                log.debug("Community profile not available for {}/{}", owner, repo);
                return null;
            }
            throw e;
        }
    }

    public List<JsonObject> getContributors(String owner, String repo) throws GitHubApiException {
        return paginate(String.format("/repos/%s/%s/contributors", owner, repo));
    }

    public List<JsonObject> getCommits(String owner, String repo, String branch, int days) {
        String endpoint = String.format("/repos/%s/%s/commits", owner, repo);
        String since = Instant.now().minus(Duration.ofDays(days)).toString();
        return paginate(endpoint + "?since=" + since + "&per_page=100");
    }

    public JsonObject getCommitActivity(String owner, String repo) throws GitHubApiException {
        String endpoint = String.format("/repos/%s/%s/stats/commit_activity", owner, repo);
        try {
            return makeRequest(endpoint);
        } catch (GitHubApiException e) {
            log.warn("Commit activity not available: {}", e.getMessage());
            return null;
        }
    }

    public List<JsonObject> getReleases(String owner, String repo) throws GitHubApiException {
        return paginate(String.format("/repos/%s/%s/releases", owner, repo));
    }

    public List<JsonObject> getIssues(String owner, String repo, String state, int days) {
        String endpoint = String.format("/repos/%s/%s/issues?state=%s&since=%s&per_page=100",
            owner, repo, state, Instant.now().minus(Duration.ofDays(days)).toString());
        return paginate(endpoint);
    }

    public JsonObject getIssuesStats(String owner, String repo) throws GitHubApiException {
        String endpoint = String.format("/repos/%s/%s", owner, repo);
        return makeRequest(endpoint);
    }

    public List<JsonObject> getPullRequests(String owner, String repo, String state) {
        String endpoint = String.format("/repos/%s/%s/pulls?state=%s&per_page=100", owner, repo, state);
        return paginate(endpoint);
    }

    public JsonArray getContents(String owner, String repo, String path) throws GitHubApiException {
        String endpoint = String.format("/repos/%s/%s/contents/%s", owner, repo, path);
        try {
            return makeRequestArray(endpoint);
        } catch (GitHubApiException e) {
            if (e.isNotFound()) {
                return null;
            }
            throw e;
        }
    }

    public JsonObject getContent(String owner, String repo, String path) throws GitHubApiException {
        String endpoint = String.format("/repos/%s/%s/contents/%s", owner, repo, path);
        return makeRequest(endpoint);
    }

    public JsonObject getDefaultBranch(String owner, String repo) throws GitHubApiException {
        return getRepo(owner, repo);
    }

    private JsonObject makeRequest(String endpoint) throws GitHubApiException {
        waitForRateLimit();
        
        String url = GITHUB_API_BASE + endpoint;
        log.debug("GET {}", url);
        
        var request = new okhttp3.Request.Builder()
            .url(url)
            .get()
            .build();
        
        try (var response = httpClient.newCall(request).execute()) {
            updateRateLimitInfo(response);
            
            int statusCode = response.code();
            String body = response.body() != null ? response.body().string() : "";
            
            if (!response.isSuccessful()) {
                log.error("API request failed: {} - Status: {}", url, statusCode);
                throw new GitHubApiException(
                    "API request failed: " + response.message(),
                    statusCode,
                    endpoint
                );
            }
            
            if (body.isEmpty()) {
                return new JsonObject();
            }
            
            return gson.fromJson(body, JsonObject.class);
            
        } catch (IOException e) {
            throw new GitHubApiException("Network error: " + e.getMessage(), -1, endpoint, e);
        } catch (Exception e) {
            throw new GitHubApiException("Parse error: " + e.getMessage(), -1, endpoint, e);
        }
    }

    private JsonArray makeRequestArray(String endpoint) throws GitHubApiException {
        waitForRateLimit();
        
        String url = GITHUB_API_BASE + endpoint;
        log.debug("GET {}", url);
        
        var request = new okhttp3.Request.Builder()
            .url(url)
            .get()
            .build();
        
        try (var response = httpClient.newCall(request).execute()) {
            updateRateLimitInfo(response);
            
            int statusCode = response.code();
            String body = response.body() != null ? response.body().string() : "";
            
            if (!response.isSuccessful()) {
                throw new GitHubApiException(
                    "API request failed: " + response.message(),
                    statusCode,
                    endpoint
                );
            }
            
            if (body.isEmpty()) {
                return new JsonArray();
            }
            
            return gson.fromJson(body, JsonArray.class);
            
        } catch (IOException e) {
            throw new GitHubApiException("Network error: " + e.getMessage(), -1, endpoint, e);
        } catch (Exception e) {
            throw new GitHubApiException("Parse error: " + e.getMessage(), -1, endpoint, e);
        }
    }

    private List<JsonObject> paginate(String endpoint) {
        List<JsonObject> results = new ArrayList<>();
        String baseUrl = GITHUB_API_BASE + endpoint;
        String url = baseUrl;
        int page = 1;
        int maxPages = 10;
        
        while (url != null && page <= maxPages) {
            waitForRateLimit();
            
            log.debug("GET {} (page {})", url, page);
            
            var request = new okhttp3.Request.Builder()
                .url(url)
                .get()
                .build();
            
            try (var response = httpClient.newCall(request).execute()) {
                updateRateLimitInfo(response);
                
                int statusCode = response.code();
                if (!response.isSuccessful()) {
                    log.warn("Pagination request failed: {} - Status: {}", url, statusCode);
                    break;
                }
                
                String body = response.body() != null ? response.body().string() : "";
                if (body.isEmpty()) {
                    break;
                }
                
                JsonArray arr = gson.fromJson(body, JsonArray.class);
                if (arr == null || arr.size() == 0) {
                    break;
                }
                
                for (JsonElement elem : arr) {
                    results.add(elem.getAsJsonObject());
                }
                
                // Check for next page
                String next = response.header("Link");
                if (next != null && next.contains("rel=\"next\"")) {
                    // Extract next URL
                    String[] parts = next.split(",");
                    for (String part : parts) {
                        if (part.contains("rel=\"next\"")) {
                            int start = part.indexOf("<");
                            int end = part.indexOf(">");
                            if (start >= 0 && end > start) {
                                url = part.substring(start + 1, end);
                                page++;
                                break;
                            }
                        }
                    }
                } else {
                    url = null;
                }
                
            } catch (IOException e) {
                log.error("Pagination error: {}", e.getMessage());
                break;
            } catch (Exception e) {
                log.error("Parse error during pagination: {}", e.getMessage());
                break;
            }
        }
        
        return results;
    }

    private void waitForRateLimit() {
        if (rateLimitInfo.remaining <= 10) {
            long waitTime = Math.max(0, rateLimitInfo.resetTime - System.currentTimeMillis());
            if (waitTime > 0) {
                log.info("Rate limit low, waiting {}ms", waitTime);
                try {
                    Thread.sleep(waitTime + 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void updateRateLimitInfo(okhttp3.Response response) {
        String limit = response.header("X-RateLimit-Limit");
        String remaining = response.header("X-RateLimit-Remaining");
        String reset = response.header("X-RateLimit-Reset");
        
        if (remaining != null) {
            rateLimitInfo.remaining = Integer.parseInt(remaining);
        }
        if (reset != null) {
            rateLimitInfo.resetTime = Long.parseLong(reset) * 1000;
        }
        
        log.debug("Rate limit: {}/{}", remaining, limit);
    }

    private static class RateLimitInfo {
        int remaining = 5000;
        long resetTime = 0;
    }

    public void close() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }
}
