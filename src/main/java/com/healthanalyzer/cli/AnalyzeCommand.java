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

import java.io.File;
import java.util.concurrent.Callable;

import static com.healthanalyzer.cli.Main.*;

@Command(
    name = "analyze",
    description = "Analyze a GitHub repository health",
    mixinStandardHelpOptions = true
)
public class AnalyzeCommand implements Callable<Integer> {

    @Option(names = {"-t", "--token"}, description = "GitHub API token")
    private String token;

    @Option(names = {"-f", "--format"}, description = "Output format (table, json, markdown)")
    private String format = "table";

    @Option(names = {"-o", "--output"}, description = "Output file path")
    private String outputFile;

    @Option(names = {"-v", "--verbose"}, description = "Verbose logging")
    private boolean verbose;

    @Parameters(paramLabel = "OWNER/REPO", description = "Repository in format 'owner/repo'")
    private String repoPath;

    @Override
    public Integer call() throws Exception {
        System.out.println(CYAN + "\n🔍 " + BOLD + "Starting Health Analysis..." + RESET);
        
        // Validate input
        if (repoPath == null || repoPath.isEmpty()) {
            System.out.println(RED + "❌ " + BOLD + "Error: Repository path is required" + RESET);
            System.out.println(WHITE + "   Usage: analyze OWNER/REPO" + RESET);
            return 1;
        }

        // Parse repo path
        String[] parts = repoPath.split("/");
        if (parts.length != 2) {
            System.out.println(RED + "❌ " + BOLD + "Error: Invalid repository format. Use 'owner/repo'" + RESET);
            return 1;
        }
        String owner = parts[0];
        String repo = parts[1].replace(".git", "");

        System.out.println(YELLOW + "📦 Repository: " + CYAN + owner + "/" + BOLD + repo + RESET);

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
            // Analyze
            System.out.println(CYAN + "⏳ " + WHITE + "Fetching data from GitHub..." + RESET);
            HealthReport report = service.analyze(owner, repo);

            // Generate output
            OutputFormat outputFormat = OutputFormat.fromString(format);
            String content = reportService.generateReport(report, outputFormat);

            // Output
            if (outputFile != null && !outputFile.isEmpty()) {
                reportService.saveReport(content, outputFile);
                System.out.println(GREEN + "\n✅ " + BOLD + "Report saved to: " + WHITE + outputFile + RESET);
            } else {
                System.out.println("\n" + GREEN + "═".repeat(60) + RESET);
                reportService.printReport(content);
            }
            
            System.out.println(GREEN + "\n✅ " + BOLD + "Analysis Complete!" + RESET);
            System.out.println(MAGENTA + "   Developed by: xEdi | https://github.com/xedi1" + RESET);

            return 0;
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
}
