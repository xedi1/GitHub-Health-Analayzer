package com.healthanalyzer.report;

import com.healthanalyzer.config.OutputFormat;
import com.healthanalyzer.model.HealthReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportService {
    private static final Logger log = LoggerFactory.getLogger(ReportService.class);
    
    private final Map<OutputFormat, ReportFormatter> formatters = new HashMap<>();

    public ReportService() {
        registerFormatter(new TableReportFormatter());
        registerFormatter(new JsonReportFormatter());
        registerFormatter(new MarkdownReportFormatter());
        // HTML and CSV would be added in later phases
    }

    private void registerFormatter(ReportFormatter formatter) {
        try {
            OutputFormat format = OutputFormat.fromString(formatter.getFormatName());
            formatters.put(format, formatter);
        } catch (Exception e) {
            log.warn("Failed to register formatter: {}", e.getMessage());
        }
    }

    public String generateReport(HealthReport report, OutputFormat format) {
        ReportFormatter formatter = formatters.get(format);
        if (formatter == null) {
            log.warn("No formatter for format {}, using table", format);
            formatter = formatters.get(OutputFormat.TABLE);
        }
        return formatter.format(report);
    }

    public String generateBatchReport(List<HealthReport> reports, OutputFormat format) {
        if (reports.isEmpty()) {
            return "No reports to generate.";
        }

        StringBuilder sb = new StringBuilder();
        
        // Batch header
        sb.append("GitHub Repository Health Analysis - Batch Report\n");
        sb.append("=".repeat(80)).append("\n\n");
        sb.append(String.format("Total Repositories Analyzed: %d\n\n", reports.size()));
        
        // Summary table
        sb.append("## Summary\n\n");
        sb.append("| Repository | Score | Status | Stars | Contributors |\n");
        sb.append("|------------|-------|--------|-------|--------------|\n");
        
        for (HealthReport report : reports) {
            sb.append(String.format("| [%s](%s) | %d/100 | %s | %,d | %d |\n",
                report.repoInfo().fullName(),
                "https://github.com/" + report.repoInfo().fullName(),
                report.healthScore().totalScore(),
                report.healthScore().status().label(),
                report.repoInfo().stars(),
                report.metrics().contributors().size()));
        }
        sb.append("\n");
        
        // Individual reports
        for (HealthReport report : reports) {
            sb.append("---").append("\n\n");
            sb.append(generateReport(report, format));
            sb.append("\n\n");
        }
        
        return sb.toString();
    }

    public void saveReport(String content, String filePath) throws IOException {
        Path path = Path.of(filePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        log.info("Report saved to {}", filePath);
    }

    public void printReport(String content) {
        System.out.println(content);
    }
}
