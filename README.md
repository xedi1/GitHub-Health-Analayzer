# 🔍 GitHub Health Analyzer

**Check if a GitHub project is healthy, active, and safe to use!**

---

### 👨‍💻 Developer Info
| | |
|---|---|
| **Developer** | xEdi |
| **GitHub** | [github.com/xedi1](https://github.com/xedi1) |
| **LinkedIn** | [linkedin.com/in/hadi-gholipour](https://linkedin.com/in/hadi-gholipour) |

---

## 🤔 What is this?

Have you ever wanted to use a library or tool from GitHub, but worried it might be abandoned or unsafe? This tool helps you check the "health" of any GitHub project.

### Think of it like a doctor checkup for projects!

Just like a doctor checks:
- Your heart rate 💓
- Your blood pressure 💉
- Your energy levels ⚡

This tool checks:
- Is the project still active? 📊
- Do many people help maintain it? 👥
- Are issues being fixed? 🐛
- Is the documentation good? 📖
- Are releases coming out regularly? 📦

---

## 🎯 What can this tell me?

This tool gives every project a **Health Score** from 0 to 100:

| Score | Status | Meaning |
|-------|--------|---------|
| 80-100 | 🟢 **ACTIVE** | Very healthy, well-maintained |
| 60-79 | 🟡 **MAINTAINED** | Good, but might have some issues |
| 35-59 | 🟠 **SLOW** | Getting slower, but not abandoned |
| 0-34 | 🔴 **DEAD** | Likely abandoned, don't rely on it! |

### Special Warnings:
- ⚠️ **Zombie Repo**: Popular but dead (high stars, no activity)
- ⚠️ **Single Maintainer Risk**: Only one person does all the work
- ⚠️ **Stale Issues**: Many bugs left unanswered

---

## 🚀 Quick Start Guide

### Step 1: Download the Project

1. Download the file `github-health-analyzer.zip` from the project folder
2. Extract (unzip) the file to a folder on your computer

### Step 2: Check if Java is Installed

Open a terminal (or Command Prompt on Windows) and type:

```bash
java -version
```

If you see something like `java version "21.x.x"`, you're good! ✅

**If not**, install Java:
- **Windows/Mac**: https://adoptium.net/ (click "Download JDK")
- **Linux**: Open terminal and type: `sudo apt install openjdk-21-jdk`

### Step 3: Get a GitHub Token (Optional but Recommended)

**Why?** Without a token, you can only check 60 projects per hour. With a token, you can check 5,000!

**How to get a token:**
1. Go to: https://github.com/settings/tokens
2. Click **"Generate new token"** (blue button)
3. Give it a name (like "Health Analyzer")
4. Select the checkbox: `repo` (full control)
5. Click **"Generate token"**
6. **Copy the token** (it's a long string like `ghp_xxxxxxxxxxxx`)

### Step 4: Run the Tool!

**Basic command:**
```bash
# Replace "owner/repo" with the project you want to check
# Example: openai/whisper, microsoft/vscode, facebook/react

java -jar target/github-health-analyzer-1.0.0-SNAPSHOT.jar analyze owner/repo
```

**Example:**
```bash
java -jar target/github-health-analyzer-1.0.0-SNAPSHOT.jar analyze openai/whisper
```

**With your token:**
```bash
export GITHUB_TOKEN="ghp_your_token_here"
java -jar target/github-health-analyzer-1.0.0-SNAPSHOT.jar analyze microsoft/vscode
```

---

## 📝 Common Commands

### Check a single project
```bash
java -jar target/github-health-analyzer-1.0.0-SNAPSHOT.jar analyze owner/repo
```

### Get JSON output (for developers)
```bash
java -jar target/github-health-analyzer-1.0.0-SNAPSHOT.jar analyze owner/repo --format json
```

### Get Markdown output (for README files)
```bash
java -jar target/github-health-analyzer-1.0.0-SNAPSHOT.jar analyze owner/repo --format markdown
```

### Save output to a file
```bash
java -jar target/github-health-analyzer-1.0.0-SNAPSHOT.jar analyze owner/repo --output my-report.md
```

### Check multiple projects at once
```bash
java -jar target/github-health-analyzer-1.0.0-SNAPSHOT.jar analyze-batch my-repos.txt
```

---

## 📂 How to check multiple projects?

1. Create a text file (like `repos.txt`)
2. Add one project per line:
   ```
   openai/whisper
   microsoft/vscode
   google/guava
   facebook/react
   ```
3. Run:
   ```bash
   java -jar target/github-health-analyzer-1.0.0-SNAPSHOT.jar analyze-batch repos.txt
   ```

---

## 🔎 Example Output

When you run the tool, you'll see something like this:

```
════════════════════════════════════════════════════════════════════════════════
 GitHub Repository Health Report
════════════════════════════════════════════════════════════════════════════════

 Repository: microsoft/vscode
 Description: Visual Studio Code - Open Source IDE

 Statistics
─────────────────────────────────────────────────────────────────────────────────
 Stars:      155,234
 Forks:      28,456
 Watchers:   3,245
 Open Issues: 12,345
 Closed Issues: 89,234
 Last Push: 2024-01-15

 Health Analysis
─────────────────────────────────────────────────────────────────────────────────

 Overall Score: 92/100 [Active]
 Healthy project with 8 positive and 1 negative signals

 Score Breakdown:
   Activity:        24/25  ✓ Recent commits
   Contributors:    18/20  ✓ Good diversity
   Issues:          13/15  ✓ Good response
   Documentation:   14/15  ✓ Well documented
   Releases:        9/10   ✓ Regular releases
   Popularity:      14/15  ✓ Very popular
```

---

## ❓ Frequently Asked Questions

### Q: Do I need to install anything?
**A:** You need Java 21. Everything else is included in the ZIP file.

### Q: Is this free?
**A:** Yes! It's completely free and open source.

### Q: What does "Health Score" mean?
**A:** It's a number from 0-100 that tells you how healthy a project is. Higher is better!

### Q: I got an error "Rate limit exceeded"
**A:** This means GitHub is limiting how many requests you can make. Get a GitHub token (Step 3 above) to fix this.

### Q: Can I check private repositories?
**A:** Yes, but you need a GitHub token with `repo` permission.

### Q: What if a project has 0 stars?
**A:** New projects might have 0 stars but still be healthy. Check the activity and issue response instead.

---

## 🏗️ Project Structure

```
github-health-analyzer/
├── target/
│   └── github-health-analyzer-1.0.0-SNAPSHOT.jar  ← The executable file
├── src/                      ← Source code (for developers)
├── pom.xml                   ← Build configuration
├── README.md                 ← This file
└── SPEC.md                   ← Technical details
```

---

## 🎓 What does the Health Score mean?

### Activity (25 points)
- How recently were commits made?
- How often do they push new code?
- Is the project improving or slowing down?

### Contributors (20 points)
- How many people help?
- Is the work split fairly?
- What happens if the main person leaves? (Bus Factor)

### Issues (15 points)
- Are bugs being fixed?
- How fast do they respond to issues?
- Are there old, unanswered issues?

### Documentation (15 points)
- Is there a README file?
- Is there a CONTRIBUTING guide?
- Is there a LICENSE file?

### Releases (10 points)
- Do they publish new versions?
- How often do they release?
- Are the releases stable?

### Popularity (15 points)
- How many stars?
- How many forks?
- Does popularity match activity?

---

## 🐛 Troubleshooting

### "java: command not found"
```bash
# Install Java first!
# Windows/Mac: https://adoptium.net/
# Linux: sudo apt install openjdk-21-jdk
```

### "Error: Repository not found"
- Check the spelling of "owner/repo"
- Example: `openai/whisper` not `openai whisper`

### "Error: Rate limit exceeded"
```bash
# Get a token from: https://github.com/settings/tokens
export GITHUB_TOKEN="your_token_here"
```

### "Error: Authentication required"
- The repository might be private
- Use a token with `repo` permission

---

## 📜 License

This project is free to use and modify. See LICENSE file for details.

---

## 🙏 Thank You!

If you find this useful, please:
- ⭐ Star the project on GitHub
- 🐛 Report bugs you find
- 💡 Suggest improvements

---

## 👨‍💻 Developer

| | |
|---|---|
| **Name** | xEdi |
| **GitHub** | [github.com/xedi1](https://github.com/xedi1) |
| **LinkedIn** | [linkedin.com/in/hadi-gholipour](https://linkedin.com/in/hadi-gholipour) |

---

**Questions?** Open an issue on GitHub or check the SPEC.md file for technical details.
