package com.healthanalyzer.model;

public enum HealthStatus {
    ACTIVE("Active", "Very healthy, actively maintained with good community"),
    MAINTAINED("Maintained", "Healthy but may have some concerns"),
    SLOW("Slow", "Declining activity but not abandoned"),
    DEAD("Dead", "No activity, unresponsive, likely abandoned");

    private final String label;
    private final String description;

    HealthStatus(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }
}
