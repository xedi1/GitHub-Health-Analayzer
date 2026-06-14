package com.healthanalyzer.config;

import java.time.Duration;

public record AppConfig(
    String githubToken,
    Duration requestTimeout,
    int maxRetries,
    Duration retryDelay,
    boolean verboseLogging,
    OutputFormat defaultFormat,
    String cacheDir
) {
    public static Builder builder() {
        return new Builder();
    }

    public static AppConfig defaults() {
        return builder().build();
    }

    public static class Builder {
        private String githubToken;
        private Duration requestTimeout = Duration.ofSeconds(30);
        private int maxRetries = 3;
        private Duration retryDelay = Duration.ofSeconds(2);
        private boolean verboseLogging = false;
        private OutputFormat defaultFormat = OutputFormat.TABLE;
        private String cacheDir = System.getProperty("user.home") + "/.gha/cache";

        public Builder githubToken(String token) {
            this.githubToken = token;
            return this;
        }

        public Builder requestTimeout(Duration timeout) {
            this.requestTimeout = timeout;
            return this;
        }

        public Builder maxRetries(int retries) {
            this.maxRetries = retries;
            return this;
        }

        public Builder retryDelay(Duration delay) {
            this.retryDelay = delay;
            return this;
        }

        public Builder verboseLogging(boolean verbose) {
            this.verboseLogging = verbose;
            return this;
        }

        public Builder defaultFormat(OutputFormat format) {
            this.defaultFormat = format;
            return this;
        }

        public Builder cacheDir(String dir) {
            this.cacheDir = dir;
            return this;
        }

        public AppConfig build() {
            return new AppConfig(
                githubToken,
                requestTimeout,
                maxRetries,
                retryDelay,
                verboseLogging,
                defaultFormat,
                cacheDir
            );
        }
    }
}
