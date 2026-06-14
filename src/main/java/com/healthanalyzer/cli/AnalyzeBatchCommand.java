package com.healthanalyzer.cli;

import com.healthanalyzer.config.AppConfig;
import com.healthanalyzer.config.OutputFormat;
import com.healthanalyzer.model.HealthReport;
import com.healthanalyzer.report.ReportService;
import com.healthanalyzer.service.HealthAnalysisService;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static com.healthanalyzer.cli.Main.*;

@Command(
    name = "analyze-batch",
    description = "Analyze multiple GitHub repositories from a file",
    mixinStandardHelpOptions = true
)
public class AnalyzeBatchCommand implements Callable<Integer> {

    @Option(names = {"-t", "--token"}, description = "GitHub API token")
    private String token;

    @Option(names = {"-f", "--format"}, description = "Output format (table, json, markdown)")
    private String format = "table";

    @Option(names = {"-o", "--output"}, description = "Output file path")
    private String outputFile;

    @Option(names = {"-v", "--verbose"}, description = "Verbose logging")
    private boolean verbose;

    @Parameters(paramLabel = "FILE", description = "File containing repository list (one per line)")
    private String filePath;

    @Override
    public Integer call() throws Exception {
        System.out.println(CYAN + "\n🔍 " + BOLD + "Starting Batch Health Analysis..." + RESET);
        
        // Validate input
        if (filePath == null || filePath.isEmpty()) {
            System.out.println(RED + "❌ " + BOLD + "Error: Input file is required" + RESET);
            System.out.println(WHITE + "   Usage: analyze-batch FILE" + RESET);
            return 1;
        }

        // Read repositories from file
        List<String> repos = readReposFromFile(filePath);
        if (repos.isEmpty()) {
            System.out.println(RED + "❌ " + BOLD + "Error: No repositories found in file" + RESET);
            return 1;
        }

        System.out.println(GREEN + "📋 " + WHITE + "Found " + CYAN + repos.size() + WHITE + " repositories to analyze" + RESET);

        // Get token from env if not provided
        String githubToken = token;
        if (githubToken == null || githubToken.isEmpty()) {
            githubToken = System.getenv("GITHUB_TOKEN");
            if (githubToken != null && !githubToken.isEmpty()) {
                System.out.println(GREEN + "✓ " + WHITE + "Using GitHub token from environment" + RESET);
            }
        }

        // Create config
        AppConfig config = AppConfig.builder()
            .githubToken(githubToken)
            .verboseLogging(verbose)
            .defaultFormat(OutputFormat.fromString(format))
            .build();

        // Create services
        HealthAnalysisService service = new HealthAnalysisService(config);
        ReportService reportService = new ReportService();

        try {
            // Analyze all repos
            List<HealthReport> reports = new ArrayList<>();
            int success = 0;
            int failed = 0;

            for (String repoPath : repos) {
                repoPath = repoPath.trim();
                if (repoPath.isEmpty() || repoPath.startsWith("#")) {
                    continue; // Skip empty lines and comments
                }

                try {
                    String[] parts = repoPath.split("/");
                    if (parts.length >= 2) {
                        String owner = parts[parts.length - 2];
                        String repo = parts[parts.length - 1].replace(".git", "");
                        
                        System.out.println(YELLOW + "\n⏳ " + WHITE + "Analyzing: " + CYAN + repoPath + RESET);
                        HealthReport report = service.analyze(owner, repo);
                        reports.add(report);
                        success++;
                        
                        // Print summary with color
                        int score = report.healthScore().totalScore();
                        String statusLabel = report.healthScore().status().label();
                        String scoreColor = score >= 80 ? GREEN : (score >= 60 ? YELLOW : (score >= 35 ? MAGENTA : RED));
                        
                        System.out.println(GREEN + "  ✓ " + WHITE + "Score: " + scoreColor + score + WHITE + "/100 " + WHITE + "[" + statusLabel + "]" + RESET);
                    }
                } catch (Exception e) {
                    System.out.println(RED + "  ✗ " + WHITE + "Failed: " + e.getMessage() + RESET);
                    failed++;
                }
            }

            // Summary
            System.out.println("\n" + BLUE + "═".repeat(60) + RESET);
            System.out.println(CYAN + BOLD + "  📊 Batch Analysis Complete" + RESET);
            System.out.println(BLUE + "═".repeat(60) + RESET);
            System.out.println(GREEN + "  ✅ Success: " + WHITE + success + RESET);
            System.out.println(RED + "  ❌ Failed: " + WHITE + failed + RESET);
            System.out.println(YELLOW + "  📦 Total: " + WHITE + repos.size() + RESET);
            System.out.println(BLUE + "═".repeat(60) + RESET);

            // Generate report
            if (!reports.isEmpty()) {
                OutputFormat outputFormat = OutputFormat.fromString(format);
                String content = reportService.generateBatchReport(reports, outputFormat);

                if (outputFile != null && !outputFile.isEmpty()) {
                    reportService.saveReport(content, outputFile);
                    System.out.println(GREEN + "\n✅ " + BOLD + "Report saved to: " + WHITE + outputFile + RESET);
                } else {
                    reportService.printReport(content);
                }
            }
            
            System.out.println(GREEN + "\n✅ " + BOLD + "All Done!" + RESET);
            System.out.println(MAGENTA + "   Developed by: xEdi | https://github.com/xedi1" + RESET);
            System.out.println(MAGENTA + "   LinkedIn: linkedin.com/in/hadi-gholipour" + RESET);

            return failed > 0 ? 1 : 0;
        } catch (Exception e) {
            System.out.println(RED + "\n❌ " + BOLD + "Error: " + WHITE + e.getMessage() + RESET);
            if (verbose) {
                e.printStackTrace();
            }
            return 1;
        } finally {
            service.close();
        }
    }

    private List<String> readReposFromFile(String filePath) throws IOException {
        List<String> repos = new ArrayList<>();
        Path path = Path.of(filePath);
        
        if (!Files.exists(path)) {
            throw new IOException("File not found: " + filePath);
        }

        List<String> lines = Files.readAllLines(path);
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                repos.add(trimmed);
            }
        }
        
        return repos;
    }
}
