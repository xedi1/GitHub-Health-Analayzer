package com.healthanalyzer.report;

import com.healthanalyzer.model.HealthReport;
import com.healthanalyzer.model.HealthScore;
import com.healthanalyzer.model.HealthSignal;

import java.time.format.DateTimeFormatter;

public class TableReportFormatter implements ReportFormatter {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int WIDTH = 70;
    
    // ANSI Colors
    private static final String RESET = "\033[0m";
    private static final String BOLD = "\033[1m";
    private static final String RED = "\033[91m";
    private static final String GREEN = "\033[92m";
    private static final String YELLOW = "\033[93m";
    private static final String BLUE = "\033[94m";
    private static final String MAGENTA = "\033[95m";
    private static final String CYAN = "\033[96m";
    private static final String WHITE = "\033[97m";

    @Override
    public String format(HealthReport report) {
        StringBuilder sb = new StringBuilder();
        
        HealthScore score = report.healthScore();
        
        // Colorful Header
        sb.append(GREEN).append("╔").append("═".repeat(WIDTH)).append("╗\n");
        sb.append(GREEN).append("║").append(BOLD).append(CYAN).append("  🔍 GitHub Repository Health Report").append(RESET);
        sb.append(GREEN).append(" ".repeat(WIDTH - 37)).append("║\n");
        sb.append(GREEN).append("╚").append("═".repeat(WIDTH)).append("╝").append(RESET);
        sb.append("\n");
        
        // Repository Info with colors
        sb.append(BLUE).append("  📦 Repository: ").append(RESET);
        sb.append(WHITE).append(BOLD).append(report.repoInfo().fullName()).append(RESET).append("\n");
        
        if (report.repoInfo().description() != null) {
            sb.append(MAGENTA).append("  📝 Description: ").append(RESET);
            sb.append(truncate(report.repoInfo().description(), 50)).append("\n");
        }
        
        if (report.repoInfo().language() != null) {
            sb.append(CYAN).append("  💻 Language: ").append(RESET);
            sb.append(report.repoInfo().language()).append("\n");
        }
        sb.append("\n");
        
        // Statistics with colors
        sb.append(GREEN).append("  ╔════════════════════════════════════════╗").append(RESET).append("\n");
        sb.append(GREEN).append("  ║").append(BOLD).append(YELLOW).append("  📊 Statistics").append(RESET);
        sb.append(GREEN).append(" ".repeat(28)).append("║\n");
        sb.append(GREEN).append("  ╚════════════════════════════════════════╝").append(RESET).append("\n");
        
        sb.append(WHITE).append("    ⭐ Stars:         ").append(YELLOW).append(formatNumber(report.repoInfo().stars())).append(RESET).append("\n");
        sb.append(WHITE).append("    🍴 Forks:         ").append(YELLOW).append(formatNumber(report.repoInfo().forks())).append(RESET).append("\n");
        sb.append(WHITE).append("    👁 Watchers:      ").append(YELLOW).append(formatNumber(report.repoInfo().watchers())).append(RESET).append("\n");
        sb.append(WHITE).append("    📂 Open Issues:   ").append(CYAN).append(formatNumber(report.repoInfo().openIssues())).append(RESET).append("\n");
        sb.append(WHITE).append("    ✅ Closed Issues: ").append(GREEN).append(formatNumber(report.repoInfo().closedIssues())).append(RESET).append("\n");
        
        if (report.repoInfo().pushedAt() != null) {
            sb.append(WHITE).append("    🕐 Last Push:     ").append(MAGENTA).append(
                report.repoInfo().pushedAt().atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime().format(DATE_FORMAT)).append(RESET).append("\n");
        }
        sb.append("\n");
        
        // Health Score with colors
        sb.append(GREEN).append("  ╔════════════════════════════════════════╗").append(RESET).append("\n");
        sb.append(GREEN).append("  ║").append(BOLD).append(YELLOW).append("  🏥 Health Analysis").append(RESET);
        sb.append(GREEN).append(" ".repeat(26)).append("║\n");
        sb.append(GREEN).append("  ╚════════════════════════════════════════╝").append(RESET).append("\n");
        
        // Score display with color based on value
        String scoreColor = score.totalScore() >= 80 ? GREEN : (score.totalScore() >= 60 ? YELLOW : (score.totalScore() >= 35 ? MAGENTA : RED));
        String statusColor = score.status().label().equals("Active") ? GREEN : 
                             (score.status().label().equals("Maintained") ? YELLOW : 
                             (score.status().label().equals("Slow") ? MAGENTA : RED));
        
        sb.append("\n");
        sb.append(WHITE).append("    Overall Score: ").append(BOLD).append(scoreColor).append(score.totalScore()).append(RESET);
        sb.append(WHITE).append("/100 ").append(RESET);
        sb.append(" [").append(statusColor).append(BOLD).append(score.status().label()).append(RESET).append("]").append("\n");
        sb.append("\n");
        
        // Score Breakdown with progress bars
        sb.append(WHITE).append("  Score Breakdown:").append("\n");
        sb.append(String.format("    " + GREEN + "▓".repeat(20) + RESET + " Activity:     %2d/25\n", score.activityScore()));
        sb.append(String.format("    " + CYAN + "▓".repeat(20) + RESET + " Contributors: %2d/20\n", score.contributorScore()));
        sb.append(String.format("    " + YELLOW + "▓".repeat(20) + RESET + " Issues:       %2d/15\n", score.issueScore()));
        sb.append(String.format("    " + MAGENTA + "▓".repeat(20) + RESET + " Docs:         %2d/15\n", score.documentationScore()));
        sb.append(String.format("    " + BLUE + "▓".repeat(20) + RESET + " Releases:     %2d/10\n", score.releaseScore()));
        sb.append(String.format("    " + CYAN + "▓".repeat(20) + RESET + " Popularity:   %2d/15\n", score.popularityScore()));
        sb.append("\n");
        
        // Signals
        if (!score.signals().isEmpty()) {
            sb.append(GREEN).append("  ╔════════════════════════════════════════╗").append(RESET).append("\n");
            sb.append(GREEN).append("  ║").append(BOLD).append(CYAN).append("  🚨 Key Signals").append(RESET);
            sb.append(GREEN).append(" ".repeat(28)).append("║\n");
            sb.append(GREEN).append("  ╚════════════════════════════════════════╝").append(RESET).append("\n");
            
            for (HealthSignal signal : score.signals()) {
                String prefix = signal.positive() ? GREEN + "✅" : RED + "⚠️";
                sb.append(prefix).append(" ").append(WHITE).append(signal.message()).append(RESET).append("\n");
                if (signal.details() != null) {
                    sb.append("   ").append(BLUE).append("→ ").append(RESET).append(signal.details()).append("\n");
                }
            }
            sb.append("\n");
        }
        
        // Contributors Summary
        if (!report.metrics().contributors().isEmpty()) {
            sb.append(GREEN).append("  ╔════════════════════════════════════════╗").append(RESET).append("\n");
            sb.append(GREEN).append("  ║").append(BOLD).append(MAGENTA).append("  👥 Top Contributors").append(RESET);
            sb.append(GREEN).append(" ".repeat(23)).append("║\n");
            sb.append(GREEN).append("  ╚════════════════════════════════════════╝").append(RESET).append("\n");
            
            int count = Math.min(5, report.metrics().contributors().size());
            for (int i = 0; i < count; i++) {
                var contributor = report.metrics().contributors().get(i);
                sb.append(String.format("    %d. " + CYAN + "%s" + RESET + " (%" + YELLOW + "d" + RESET + " contributions, %.1f%%)\n",
                    i + 1, contributor.login(), contributor.contributions(), contributor.percentage()));
            }
            sb.append("\n");
        }
        
        // Footer with developer credit
        sb.append(GREEN).append("╔").append("═".repeat(WIDTH)).append("╗\n").append(RESET);
        sb.append(GREEN).append("║").append(RESET);
        sb.append(String.format(" Generated: %s", 
            report.generatedAt().atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        sb.append(" ".repeat(WIDTH - 50));
        sb.append(GREEN).append("║\n").append(RESET);
        
        sb.append(GREEN).append("║").append(RESET);
        sb.append(MAGENTA).append(" Developed by: xEdi | https://github.com/xedi1").append(RESET);
        sb.append(" ".repeat(WIDTH - 48));
        sb.append(GREEN).append("║\n").append(RESET);
        
        sb.append(GREEN).append("║").append(RESET);
        sb.append(CYAN).append(" LinkedIn: linkedin.com/in/hadi-gholipour").append(RESET);
        sb.append(" ".repeat(WIDTH - 45));
        sb.append(GREEN).append("║\n").append(RESET);
        
        sb.append(GREEN).append("╚").append("═".repeat(WIDTH)).append("╝").append(RESET);
        
        return sb.toString();
    }

    @Override
    public String getFormatName() {
        return "table";
    }

    private String formatNumber(int number) {
        if (number >= 1000000) {
            return String.format("%.1fM", number / 1000000.0);
        } else if (number >= 1000) {
            return String.format("%.1fK", number / 1000.0);
        }
        return String.valueOf(number);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "N/A";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}
