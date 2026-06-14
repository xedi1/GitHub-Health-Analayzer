package com.healthanalyzer.api;

public class GitHubApiException extends RuntimeException {
    private final int statusCode;
    private final String endpoint;

    public GitHubApiException(String message, int statusCode, String endpoint) {
        super(message);
        this.statusCode = statusCode;
        this.endpoint = endpoint;
    }

    public GitHubApiException(String message, int statusCode, String endpoint, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.endpoint = endpoint;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public boolean isRateLimited() {
        return statusCode == 403;
    }

    public boolean isNotFound() {
        return statusCode == 404;
    }

    public boolean isServerError() {
        return statusCode >= 500;
    }
}
