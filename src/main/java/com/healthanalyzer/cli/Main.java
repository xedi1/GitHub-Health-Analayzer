package com.healthanalyzer.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Spec;
import picocli.CommandLine.HelpCommand;

import java.util.Arrays;

@Command(
    name = "gha",
    description = "GitHub Health Analyzer - Analyze repository health and sustainability",
    version = "1.0.0",
    subcommands = {
        AnalyzeCommand.class,
        AnalyzeBatchCommand.class
    },
    mixinStandardHelpOptions = true
)
public class Main implements Runnable {

    // ANSI Color codes for console output
    public static final String RESET = "\033[0m";
    public static final String BOLD = "\033[1m";
    public static final String RED = "\033[91m";
    public static final String GREEN = "\033[92m";
    public static final String YELLOW = "\033[93m";
    public static final String BLUE = "\033[94m";
    public static final String MAGENTA = "\033[95m";
    public static final String CYAN = "\033[96m";
    public static final String WHITE = "\033[97m";
    public static final String BG_GREEN = "\033[42m";
    public static final String BG_YELLOW = "\033[43m";
    public static final String BG_RED = "\033[41m";
    public static final String BG_BLUE = "\033[44m";

    public static void printBanner() {
        System.out.println(CYAN + BOLD);
        System.out.println("  ███████╗██████╗ ██╗");
        System.out.println("  ██╔════╝██╔══██╗██║");
        System.out.println("  █████╗  ██║  ██║██║");
        System.out.println("  ██╔══╝  ██║  ██║██║");
        System.out.println("  ███████╗██████╔╝██║");
        System.out.println("  ╚══════╝╚═════╝ ╚═╝");
        System.out.println(RESET);
        System.out.println(GREEN + "  " + BOLD + "🔍 GitHub Project Health Analyzer" + RESET);
        System.out.println(MAGENTA + "  Developed by: xEdi | https://github.com/xedi1" + RESET);
        System.out.println(BLUE + "  ─────────────────────────────────────────────────────────" + RESET);
        System.out.println();
    }

    @Override
    public void run() {
        printBanner();
        CommandLine.usage(this, System.out);
        
        System.out.println(GREEN + "\n📌 Available commands:" + RESET);
        System.out.println(CYAN + "  analyze        " + WHITE + "- Analyze a single repository");
        System.out.println(CYAN + "  analyze-batch  " + WHITE + "- Analyze multiple repositories from a file");
        System.out.println();
        System.out.println(YELLOW + "💡 Examples:" + RESET);
        System.out.println(WHITE + "  gha analyze owner/repo");
        System.out.println(WHITE + "  gha analyze owner/repo --format json");
        System.out.println(WHITE + "  gha analyze-batch repos.txt");
        System.out.println();
    }

    public static void main(String[] args) {
        printBanner();
        CommandLine cmd = new CommandLine(new Main());
        cmd.setExecutionStrategy(new CommandLine.RunAll());
        
        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }
}
