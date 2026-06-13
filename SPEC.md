# 📋 GitHub Health Analyzer - Technical Documentation

*This file is for developers who want to understand how the tool works internally.*

---

### 👨‍💻 Developer Info
| | |
|---|---|
| **Developer** | xEdi |
| **GitHub** | [github.com/xedi1](https://github.com/xedi1) |
| **LinkedIn** | [linkedin.com/in/hadi-gholipour](https://linkedin.com/in/hadi-gholipour) |

---

## 🎯 What is this document?

This file explains **how** the GitHub Health Analyzer works under the hood. It contains technical details that regular users don't need to worry about.

If you just want to **use** the tool, please see README.md instead!

---

## 🏗️ How the Tool Works

The tool works in 4 main steps:

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  1. Fetch   │ →  │  2. Parse   │ →  │  3. Score   │ →  │  4. Report  │
│  Data from  │    │  Data into  │    │  Calculate  │    │  Generate   │
│  GitHub     │    │  Models     │    │  Health      │    │  Output     │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
```

### Step 1: Fetch Data
The tool connects to GitHub API and collects information about a project:
- Repository details (stars, forks, etc.)
- List of contributors
- Recent commits
- Issues and pull requests
- Releases

### Step 2: Parse Data
The collected data is organized into clean data structures:
- RepoInfo (basic info)
- Contributors (list of people)
- IssueStats (bug tracking)
- ReleaseInfo (version history)

### Step 3: Calculate Health Score
The scoring engine analyzes the data and gives points:
- Activity: 25 points
- Contributors: 20 points
- Issues: 15 points
- Documentation: 15 points
- Releases: 10 points
- Popularity: 15 points
- **Total: 100 points**

### Step 4: Generate Report
The report is created in the format you requested:
- Table (default)
- JSON (for developers)
- Markdown (for documentation)

---

## 📁 Project Structure

```
github-health-analyzer/
├── src/main/java/com/healthanalyzer/
│   ├── api/                    ← Communication with GitHub
│   │   ├── GitHubClient.java   ← HTTP client for GitHub
│   │   └── RepoDataFetcher.java← Collects all data
│   ├── model/                  ← Data containers
│   │   ├── RepoInfo.java       ← Repository info
│   │   ├── Contributor.java    ← Contributor details
│   │   ├── HealthScore.java     ← Health calculation result
│   │   └── HealthReport.java   ← Final report
│   ├── scoring/                ← Health calculation
│   │   └── ScoringEngine.java  ← Calculates scores
│   ├── report/                 ← Output generation
│   │   ├── TableReportFormatter.java
│   │   ├── JsonReportFormatter.java
│   │   └── MarkdownReportFormatter.java
│   └── cli/                    ← Command line interface
│       ├── Main.java           ← Entry point
│       ├── AnalyzeCommand.java  ← Single repo analysis
│       └── AnalyzeBatchCommand.java← Multiple repos
├── src/test/                   ← Unit tests
├── pom.xml                     ← Maven configuration
└── README.md                   ← User guide
```

---

## 🔧 Technical Details

### Java Version
- **Required:** Java 21 or higher
- Uses modern Java features like records, streams, and pattern matching

### Dependencies
| Library | Purpose |
|---------|---------|
| OkHttp | HTTP client for GitHub API |
| Gson | JSON parsing |
| Picocli | Command-line interface |
| Logback | Logging |
| JUnit 5 | Unit testing |

### GitHub API
- Uses GitHub REST API v3
- Rate limiting: 60 requests/hour (unauthenticated), 5,000/hour (authenticated)
- Automatic pagination for large result sets

---

## 📊 Scoring System Details

### Activity Score (25 points max)

| Condition | Points |
|-----------|--------|
| Push within 7 days | +10 |
| Push within 30 days | +7 |
| Push within 90 days | +4 |
| 20+ commits in 30 days | +8 |
| High contributor diversity | +4 |

### Contributor Score (20 points max)

| Condition | Points |
|-----------|--------|
| 10+ contributors | +8 |
| 5+ contributors | +5 |
| Bus factor (top < 50%) | +6 |
| Bus factor (top > 80%) | -8 |

### Issue Score (15 points max)

| Condition | Points |
|-----------|--------|
| Good closing rate | +3 |
| Many stale issues | -6 |
| High open ratio | -4 |

### Documentation Score (15 points max)

| File | Points |
|------|--------|
| README.md | +4 |
| CONTRIBUTING.md | +3 |
| CODE_OF_CONDUCT.md | +2 |
| Issue Template | +2 |
| PR Template | +2 |
| License | +2 |

### Release Score (10 points max)

| Condition | Points |
|-----------|--------|
| Release within 90 days | +4 |
| Regular cadence | +4 |
| Prerelease warning | -2 |

### Popularity Score (15 points max)

| Metric | Points |
|--------|--------|
| 1000+ stars | +6 |
| 100+ forks | +4 |
| Popular but dead | -10 |

---

## 🔒 Status Classification

The final status is determined by the total score:

| Score Range | Status | Description |
|-------------|--------|-------------|
| 80-100 | ACTIVE | Excellent health |
| 60-79 | MAINTAINED | Good with concerns |
| 35-59 | SLOW | Declining but alive |
| 0-34 | DEAD | Likely abandoned |

---

## 🧪 Testing

The project includes comprehensive unit tests:

- **ModelTest.java**: Tests all data models
- **ScoringEngineTest.java**: Tests the scoring algorithm
- **GitHubClientTest.java**: Tests the API client

Run tests with:
```bash
mvn test
```

---

## 🔨 Building from Source

If you want to modify and rebuild the project:

1. Install Java 21
2. Install Maven
3. Run:
   ```bash
   mvn clean package
   ```

This creates the executable JAR in `target/` folder.

---

## 📝 API Endpoints Used

The tool uses these GitHub API endpoints:

| Endpoint | Purpose |
|----------|---------|
| GET /repos/{owner}/{repo} | Repository info |
| GET /repos/{owner}/{repo}/contributors | Contributor list |
| GET /repos/{owner}/{repo}/commits | Recent commits |
| GET /repos/{owner}/{repo}/releases | Release history |
| GET /repos/{owner}/{repo}/issues | Issue list |
| GET /repos/{owner}/{repo}/pulls | Pull request list |
| GET /repos/{owner}/{repo}/contents | File existence check |

---

## 🚀 Future Features

Planned improvements:
- [ ] HTML report with charts
- [ ] SQLite database for historical tracking
- [ ] Watch mode with automatic updates
- [ ] GitHub Actions integration
- [ ] Email/Slack notifications
- [ ] Dependency vulnerability scanning

---

## 📄 License

MIT License - Free to use, modify, and distribute.

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests: `mvn test`
5. Submit a pull request

---

## 📞 Support

For questions or issues, please open an issue on GitHub.

---

## 👨‍💻 Developer

| | |
|---|---|
| **Name** | xEdi |
| **GitHub** | [github.com/xedi1](https://github.com/xedi1) |
| **LinkedIn** | [linkedin.com/in/hadi-gholipour](https://linkedin.com/in/hadi-gholipour) |
