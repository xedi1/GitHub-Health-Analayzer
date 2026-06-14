package com.healthanalyzer.report;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.healthanalyzer.model.HealthReport;

import java.time.Instant;

public class JsonReportFormatter implements ReportFormatter {
    private final Gson gson;

    public JsonReportFormatter() {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();
    }

    @Override
    public String format(HealthReport report) {
        return gson.toJson(report);
    }

    @Override
    public String getFormatName() {
        return "json";
    }
}
