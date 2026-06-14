package com.healthanalyzer.report;

import com.healthanalyzer.model.HealthReport;

public interface ReportFormatter {
    String format(HealthReport report);
    String getFormatName();
}
