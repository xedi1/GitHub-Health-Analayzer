package com.healthanalyzer.config;

public enum OutputFormat {
    TABLE("table", "Console table format"),
    JSON("json", "JSON output"),
    MARKDOWN("markdown", "Markdown format"),
    HTML("html", "HTML report"),
    CSV("csv", "CSV export"),
    BADGE("badge", "Badge markdown");

    private final String value;
    private final String description;

    OutputFormat(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String value() {
        return value;
    }

    public String description() {
        return description;
    }

    public static OutputFormat fromString(String value) {
        if (value == null) return TABLE;
        for (OutputFormat format : values()) {
            if (format.value.equalsIgnoreCase(value) || format.name().equalsIgnoreCase(value)) {
                return format;
            }
        }
        return TABLE;
    }
}
