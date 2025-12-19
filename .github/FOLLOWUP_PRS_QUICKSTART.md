# Quick Start: Finding Follow-up PRs for Backporting

This is a quick reference guide for using the follow-up PR identification tool.

## TL;DR

```bash
# Install dependencies (if needed)
pip install requests

# Find follow-up PRs for a specific PR
cd .github
python3 find-followup-prs.py --pr 50799

# With your GitHub token for higher rate limits
export GITHUB_TOKEN=your_token_here
python3 find-followup-prs.py --pr 50799 --verbose

# Get JSON output
python3 find-followup-prs.py --pr 50799 --json > followups.json
```

## What This Tool Does

Given a PR number (e.g., #50799), the tool:

1. **Analyzes the source PR**: Extracts changed files, issue references, labels, and merge date
2. **Searches for candidates**: Finds all PRs merged after the source PR to the `main` branch
3. **Scores relevance**: Assigns scores based on multiple factors
4. **Returns results**: Shows top follow-up PRs that should likely be backported together

**Note**: Only PRs merged to the `main` branch are considered. PRs to other branches are automatically filtered out.

## When to Use This Tool

Use this tool when:
- ✅ Backporting a PR to an LTS branch
- ✅ Preparing a maintenance release
- ✅ Investigating which related changes were made after a specific PR
- ✅ Understanding the history and follow-up work for a feature or bugfix

## Scoring Guide

| Score Range | Meaning | Action |
|-------------|---------|--------|
| > 40 | High confidence follow-up | Almost certainly should be backported |
| 20-40 | Medium confidence | Review carefully to decide |
| < 20 | Low confidence | Less likely to need backporting |

## Common Use Cases

### Case 1: Bug Fix with Follow-up Fixes

A bug fix PR (#12345) was merged, and a few days later another PR (#12400) fixed an edge case in the same code.

```bash
python3 find-followup-prs.py --pr 12345
# Will likely show #12400 with a high score (same files + time proximity)
```

### Case 2: Feature with Improvements

A feature PR was merged, followed by performance improvements and documentation updates.

```bash
python3 find-followup-prs.py --pr 23456 --verbose
# Shows all related improvements with detailed reasoning
```

### Case 3: Complex Area with Multiple Changes

Multiple PRs touched the same subsystem over time.

```bash
python3 find-followup-prs.py --pr 34567 --max-results 100
# Get more results to see the full picture
```

## Understanding the Output

### Example Output

```
1. PR #50850 (score: 52.3)
   URL: https://github.com/quarkusio/quarkus/pull/50850
   - Modifies same files: StreamingOutputMessageBodyWriter.java
   - Same area labels: area/rest
   - Merged 3 days after source PR
```

**What this tells you:**
- **PR #50850** is highly relevant (score > 50)
- It modified the **exact same file** as the source PR
- It's in the **same component** (area/rest)
- It was merged **soon after** the source PR (3 days)
- **Decision**: This should almost certainly be backported together

## Tips

1. **Always use --verbose** for your first run to understand why PRs were matched
2. **Review the actual PRs** - Don't backport blindly based on scores
3. **Check dependencies** - Some follow-ups may require other changes not in the list
4. **Test together** - When backporting multiple PRs, test them as a unit
5. **Document the backport** - List all backported PRs in your backport PR description

## Limitations

The tool **cannot**:
- ❌ Guarantee 100% accuracy
- ❌ Understand complex business logic relationships
- ❌ Know about runtime dependencies
- ❌ Detect follow-ups that don't modify files or reference issues

**Always apply human judgment!**

## Getting Help

- Full documentation: [FOLLOWUP_PRS.md](FOLLOWUP_PRS.md)
- Example walkthrough: [FOLLOWUP_PRS_EXAMPLE.md](FOLLOWUP_PRS_EXAMPLE.md)
- Script source: [find-followup-prs.py](find-followup-prs.py)

## Troubleshooting

### "403 Forbidden" error
→ You hit the rate limit. Use a GitHub token:
```bash
export GITHUB_TOKEN=your_token_here
```

### "No follow-up PRs found"
→ Either there are genuinely no follow-ups, or the PR is very recent. Try checking manually.

### Too many/few results
→ Adjust `--max-results` or filter the output yourself.

### False positives
→ Normal! Use `--verbose` to understand why PRs were matched, then apply judgment.

## Example: Real Workflow

```bash
# Step 1: You need to backport PR #50799 to branch 3.15
git checkout 3.15

# Step 2: Find follow-ups
cd .github
python3 find-followup-prs.py --pr 50799 --verbose > /tmp/followups.txt

# Step 3: Review the list (open in your editor)
cat /tmp/followups.txt

# Step 4: Cherry-pick the original + follow-ups
git cherry-pick <commit-of-50799>
git cherry-pick <commit-of-50850>  # High-score follow-up
git cherry-pick <commit-of-50912>  # Another follow-up

# Step 5: Test everything together
./mvnw verify

# Step 6: Create backport PR
git push origin HEAD:backport-50799-to-3.15

# In your PR description, list all backported changes:
# Backport of:
# - #50799 - Fix StreamingOutput connection reset
# - #50850 - Additional error handling fix
# - #50912 - Improve test coverage
```

## Quick Reference: Command Options

| Option | Description |
|--------|-------------|
| `--pr NUMBER` | **(Required)** Source PR to analyze |
| `--repo OWNER/REPO` | Repository (default: quarkusio/quarkus) |
| `--github-token TOKEN` | GitHub token for authentication |
| `--max-results N` | Max results to show (default: 50) |
| `--verbose` or `-v` | Show detailed reasoning |
| `--json` | Output as JSON |
| `--help` or `-h` | Show help message |

---

**Ready to backport?** Start with the examples above and adapt to your needs!
