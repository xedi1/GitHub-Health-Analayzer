package com.healthanalyzer.report;

import com.healthanalyzer.model.DocumentationInfo;
import com.healthanalyzer.model.HealthReport;
import com.healthanalyzer.model.HealthScore;
import com.healthanalyzer.model.HealthSignal;

import java.time.format.DateTimeFormatter;

public class MarkdownReportFormatter implements ReportFormatter {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public String format(HealthReport report) {
        StringBuilder sb = new StringBuilder();
        
        HealthScore score = report.healthScore();
        
        // Badge
        String badge = getStatusBadge(score.status().name());
        sb.append("# ").append(report.repoInfo().fullName()).append(" ").append(badge).append("\n\n");
        
        // Description
        if (report.repoInfo().description() != null) {
            sb.append(report.repoInfo().description()).append("\n\n");
        }
        
        // Quick Stats
        sb.append("## Quick Stats\n\n");
        sb.append("| Metric | Value |\n");
        sb.append("|--------|-------|\n");
        sb.append(String.format("| ⭐ Stars | %,d |\n", report.repoInfo().stars()));
        sb.append(String.format("| 🍴 Forks | %,d |\n", report.repoInfo().forks()));
        sb.append(String.format("| 👁 Watchers | %,d |\n", report.repoInfo().watchers()));
        sb.append(String.format("| 📂 Open Issues | %,d |\n", report.repoInfo().openIssues()));
        sb.append(String.format("| ✅ Closed Issues | %,d |\n", report.repoInfo().closedIssues()));
        
        if (report.repoInfo().pushedAt() != null) {
            sb.append(String.format("| 🕐 Last Push | %s |\n",
                report.repoInfo().pushedAt().atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime().format(DATE_FORMAT)));
        }
        
        if (report.repoInfo().language() != null) {
            sb.append(String.format("| 💻 Language | %s |\n", report.repoInfo().language()));
        }
        sb.append("\n");
        
        // Health Score
        sb.append("## Health Score\n\n");
        sb.append(String.format("**Overall Score: %d/100** - %s\n\n", 
            score.totalScore(), score.status().label()));
        
        sb.append("| Dimension | Score | Max |\n");
        sb.append("|-----------|-------|-----|\n");
        sb.append(String.format("| Activity | %d | 25 |\n", score.activityScore()));
        sb.append(String.format("| Contributors | %d | 20 |\n", score.contributorScore()));
        sb.append(String.format("| Issues | %d | 15 |\n", score.issueScore()));
        sb.append(String.format("| Documentation | %d | 15 |\n", score.documentationScore()));
        sb.append(String.format("| Releases | %d | 10 |\n", score.releaseScore()));
        sb.append(String.format("| Popularity | %d | 15 |\n", score.popularityScore()));
        sb.append("\n");
        
        // Signals
        if (!score.signals().isEmpty()) {
            sb.append("## Key Signals\n\n");
            
            var positiveSignals = score.signals().stream()
                .filter(HealthSignal::positive)
                .toList();
            var negativeSignals = score.signals().stream()
                .filter(s -> !s.positive())
                .toList();
            
            if (!positiveSignals.isEmpty()) {
                sb.append("### ✅ Positive\n\n");
                for (HealthSignal signal : positiveSignals) {
                    sb.append("- **").append(signal.message()).append("**");
                    if (signal.details() != null) {
                        sb.append(": ").append(signal.details());
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
            
            if (!negativeSignals.isEmpty()) {
                sb.append("### ⚠️ Negative\n\n");
                for (HealthSignal signal : negativeSignals) {
                    sb.append("- **").append(signal.message()).append("**");
                    if (signal.details() != null) {
                        sb.append(": ").append(signal.details());
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
        }
        
        // Contributors
        if (!report.metrics().contributors().isEmpty()) {
            sb.append("## Top Contributors\n\n");
            sb.append("| Rank | User | Contributions | Share |\n");
            sb.append("|------|------|--------------|-------|\n");
            
            int count = Math.min(10, report.metrics().contributors().size());
            for (int i = 0; i < count; i++) {
                var contributor = report.metrics().contributors().get(i);
                sb.append(String.format("| %d | [%s](%s) | %,d | %.1f%% |\n",
                    i + 1,
                    contributor.login(),
                    contributor.htmlUrl() != null ? contributor.htmlUrl() : "https://github.com/" + contributor.login(),
                    contributor.contributions(),
                    contributor.percentage()));
            }
            sb.append("\n");
        }
        
        // Release Info
        if (report.metrics().releaseInfo().latestVersion() != null) {
            sb.append("## Releases\n\n");
            sb.append(String.format("- **Latest**: %s\n", report.metrics().releaseInfo().latestVersion()));
            if (report.metrics().releaseInfo().latestDate() != null) {
                sb.append(String.format("- **Released**: %s\n",
                    report.metrics().releaseInfo().latestDate()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime().format(DATE_FORMAT)));
            }
            sb.append(String.format("- **Releases (1yr)**: %d\n", 
                report.metrics().releaseInfo().releasesLastYear()));
            sb.append("\n");
        }
        
        // Documentation
        DocumentationInfo docInfo = report.metrics().documentationInfo();
        sb.append("## Documentation\n\n");
        sb.append("| Item | Status |\n");
        sb.append("|------|--------|\n");
        sb.append(String.format("| README | %s |\n", boolIcon(docInfo.hasReadme())));
        sb.append(String.format("| CONTRIBUTING | %s |\n", boolIcon(docInfo.hasContributing())));
        sb.append(String.format("| CODE_OF_CONDUCT | %s |\n", boolIcon(docInfo.hasCodeOfConduct())));
        sb.append(String.format("| Issue Template | %s |\n", boolIcon(docInfo.hasIssueTemplate())));
        sb.append(String.format("| PR Template | %s |\n", boolIcon(docInfo.hasPRTemplate())));
        sb.append(String.format("| Changelog | %s |\n", boolIcon(docInfo.hasChangelog())));
        sb.append(String.format("| License | %s |\n", boolIcon(docInfo.hasLicense())));
        sb.append("\n");
        
        // Footer
        sb.append("---\n\n");
        sb.append(String.format("*Generated by GitHub Health Analyzer on %s*\n",
            report.generatedAt().atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime().format(DATE_FORMAT)));
        sb.append("\n---\n");
        sb.append("**Developed by:** [xEdi](https://github.com/xedi1)  \n");
        sb.append("**LinkedIn:** [linkedin.com/in/hadi-gholipour](https://linkedin.com/in/hadi-gholipour)\n");
        
        return sb.toString();
    }

    @Override
    public String getFormatName() {
        return "markdown";
    }

    private String getStatusBadge(String status) {
        return switch (status) {
            case "ACTIVE" -> "![Active](https://img.shields.io/badge/Health-Active-brightgreen)";
            case "MAINTAINED" -> "![Maintained](https://img.shields.io/badge/Health-Maintained-yellow)";
            case "SLOW" -> "![Slow](https://img.shields.io/badge/Health-Slow-orange)";
            case "DEAD" -> "![Dead](https://img.shields.io/badge/Health-Dead-red)";
            default -> "";
        };
    }

    private String boolIcon(boolean value) {
        return value ? "✅" : "❌";
    }
}
