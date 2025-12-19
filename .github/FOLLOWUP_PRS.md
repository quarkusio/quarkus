# Follow-up PR Identification Tool

This tool helps identify pull requests that should be backported together with a source PR when backporting to LTS branches.

## Overview

When backporting a PR to an LTS branch, it's important to also backport any follow-up PRs that:
- Fix bugs introduced by the original PR
- Make improvements to the same code
- Reference the same issues
- Modify the same files

This tool automates the process of finding these follow-up PRs by analyzing:
1. **File changes**: PRs that modify the same files
2. **Issue references**: PRs that reference the same issues
3. **Direct references**: PRs that explicitly reference the source PR
4. **Area labels**: PRs affecting the same components
5. **Bug fixes**: Bug fixes in the same area shortly after the source PR
6. **Keywords**: PRs with follow-up or fix-related keywords
7. **Time proximity**: PRs merged shortly after the source PR

**Note**: The tool only considers PRs that were merged to the `main` branch. PRs merged to other branches (e.g., maintenance branches, feature branches) are automatically excluded.

## Installation

### Prerequisites

- Python 3.7 or higher
- `requests` library

Install dependencies:

```bash
pip install requests
```

### GitHub Token (Optional but Recommended)

For higher API rate limits, create a GitHub personal access token:

1. Go to https://github.com/settings/tokens
2. Generate a new token with `public_repo` scope
3. Set it as an environment variable:

```bash
export GITHUB_TOKEN=your_token_here
```

Or pass it directly to the script using `--github-token` flag.

## Usage

### Basic Usage

Find follow-up PRs for a given PR:

```bash
cd .github
python find-followup-prs.py --pr 50799
```

### With Verbose Output

Show detailed reasoning for each identified follow-up:

```bash
python find-followup-prs.py --pr 50799 --verbose
```

### JSON Output

Output results as JSON for programmatic processing:

```bash
python find-followup-prs.py --pr 50799 --json > followups.json
```

### Specify GitHub Token

```bash
python find-followup-prs.py --pr 50799 --github-token YOUR_TOKEN
```

### All Options

```bash
python find-followup-prs.py \
  --pr 50799 \
  --repo quarkusio/quarkus \
  --github-token YOUR_TOKEN \
  --max-results 50 \
  --verbose \
  --json
```

## Command Line Options

- `--pr NUMBER` (required): Source PR number to analyze
- `--repo OWNER/REPO`: Repository in format owner/repo (default: quarkusio/quarkus)
- `--github-token TOKEN`: GitHub API token (or set GITHUB_TOKEN env var)
- `--max-results N`: Maximum number of follow-up PRs to return (default: 50)
- `--verbose`, `-v`: Show detailed reasoning for each follow-up
- `--json`: Output results as JSON

## Scoring Algorithm

The tool assigns a relevance score to each potential follow-up PR based on several factors:

| Factor | Weight | Description |
|--------|--------|-------------|
| Same files modified | 10 pts per file | PRs that modify the same files as the source PR |
| Same issues referenced | 8 pts per issue | PRs that reference the same GitHub issues |
| References source PR | 15 pts | PR explicitly mentions the source PR number |
| Same area labels | 3 pts per label | PRs with matching `area/*` labels |
| Follow-up keywords | 2 pts | Contains keywords like "follow-up", "regression", etc. |
| Bug fix in same component | 5 pts | Bug fix that affects the same files/areas |
| Time proximity | 0-5 pts | PRs merged within 30 days get higher scores |

Higher scores indicate stronger relevance as a follow-up PR.

## Example Output

```
================================================================================
Follow-up PRs for #50799
================================================================================

Found 3 potential follow-up PRs:

1. PR #50850 (score: 45.2)
   URL: https://github.com/quarkusio/quarkus/pull/50850
   - Modifies same files: StreamingOutputMessageBodyWriter.java
   - Same area labels: area/rest
   - Merged 5 days after source PR

2. PR #50901 (score: 28.0)
   URL: https://github.com/quarkusio/quarkus/pull/50901
   - Modifies same files: StreamingOutputMessageBodyWriter.java
   - Bug fix in same component
   - Merged 12 days after source PR

3. PR #51023 (score: 15.5)
   URL: https://github.com/quarkusio/quarkus/pull/51023
   - References same issues: #50754
   - Same area labels: area/rest
   - Merged 18 days after source PR
```

## Example: PR #50799

PR #50799 fixed StreamingOutput connection reset on mid-stream errors. To find its follow-ups:

```bash
python find-followup-prs.py --pr 50799 --verbose
```

This will identify any subsequent PRs that:
- Modified `StreamingOutputMessageBodyWriter.java`
- Fixed related bugs in the RESTEasy Reactive streaming output handling
- Referenced issue #50754
- Had the `area/rest` or `area/security` labels

## Integration with Backporting Workflow

This tool can be integrated into your backporting process:

1. **Before backporting**: Run the tool to identify all related PRs
   ```bash
   python find-followup-prs.py --pr 12345 --json > followups.json
   ```

2. **Review the results**: Check each identified PR to determine if it should be backported

3. **Backport together**: Create backport PRs for all relevant changes to maintain consistency

4. **Document**: Include the list of backported PRs in the backport PR description

## Troubleshooting

### Rate Limiting

If you hit GitHub API rate limits:
- Use a GitHub token with `--github-token` or set the `GITHUB_TOKEN` environment variable
- Authenticated requests have much higher rate limits (5000/hour vs 60/hour)

### No Results Found

If no follow-ups are found:
- The source PR might be very recent
- There may genuinely be no follow-up PRs
- Try checking manually for PRs merged after the source PR that modified the same files

### False Positives

The tool may identify some PRs that aren't true follow-ups. Always review the results manually:
- Check the PR descriptions and changes
- Consider the context and purpose of each PR
- Use the `--verbose` flag to understand why each PR was identified

## Contributing

To improve the scoring algorithm or add new detection methods, edit `.github/find-followup-prs.py`.

Key areas for enhancement:
- Add more follow-up detection patterns
- Improve scoring weights based on feedback
- Add filtering options (e.g., by label, author, etc.)
- Implement caching to speed up repeated queries
