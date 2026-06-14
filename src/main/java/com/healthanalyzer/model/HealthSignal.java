package com.healthanalyzer.model;

public record HealthSignal(
    String category,
    String message,
    boolean positive,
    int scoreImpact,
    String details
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String category;
        private String message;
        private boolean positive;
        private int scoreImpact;
        private String details;

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder positive(boolean positive) {
            this.positive = positive;
            return this;
        }

        public Builder scoreImpact(int scoreImpact) {
            this.scoreImpact = scoreImpact;
            return this;
        }

        public Builder details(String details) {
            this.details = details;
            return this;
        }

        public HealthSignal build() {
            return new HealthSignal(category, message, positive, scoreImpact, details);
        }
    }
}
