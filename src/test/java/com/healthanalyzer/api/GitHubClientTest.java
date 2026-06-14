package com.healthanalyzer.api;

import com.healthanalyzer.config.AppConfig;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GitHubClientTest {
    
    @Test
    void testClientCreation() {
        AppConfig config = AppConfig.builder()
            .githubToken("test-token")
            .build();
        
        GitHubClient client = new GitHubClient(config);
        assertNotNull(client);
        client.close();
    }
    
    @Test
    void testClientWithNullToken() {
        AppConfig config = AppConfig.builder()
            .githubToken(null)
            .build();
        
        GitHubClient client = new GitHubClient(config);
        assertNotNull(client);
        client.close();
    }
    
    @Test
    void testExceptionProperties() {
        GitHubApiException rateLimited = new GitHubApiException("Rate limited", 403, "/repos/owner/repo");
        assertTrue(rateLimited.isRateLimited());
        assertFalse(rateLimited.isNotFound());
        assertFalse(rateLimited.isServerError());
        
        GitHubApiException notFound = new GitHubApiException("Not found", 404, "/repos/owner/repo");
        assertFalse(notFound.isRateLimited());
        assertTrue(notFound.isNotFound());
        
        GitHubApiException serverError = new GitHubApiException("Server error", 500, "/repos/owner/repo");
        assertFalse(serverError.isRateLimited());
        assertFalse(serverError.isNotFound());
        assertTrue(serverError.isServerError());
    }
}
