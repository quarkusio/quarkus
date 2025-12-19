# Example: Finding Follow-up PRs for PR #50799

This document demonstrates how to use the follow-up PR identification tool with a real example.

## Background

PR #50799 "Fix StreamingOutput connection reset on mid-stream errors" was merged on November 4, 2025.

It fixed issue #50754 where StreamingOutput blocking API did not call reset() on error, preventing clients from detecting incomplete chunked responses.

The PR modified:
- `independent-projects/resteasy-reactive/server/runtime/src/main/java/org/jboss/resteasy/reactive/server/providers/serialisers/StreamingOutputMessageBodyWriter.java`
- Added test files in `integration-tests/elytron-resteasy-reactive/`

## Running the Tool

To find follow-up PRs that should be backported along with #50799:

```bash
cd .github
python3 find-followup-prs.py --pr 50799 --verbose
```

## Expected Output

The tool will analyze all PRs merged after November 4, 2025 and score them based on relevance. Here's what typical output might look like:

```
Analyzing source PR #50799...
Source PR merged on: 2025-11-04 13:19:30
Files changed: 3
Issue references: {50754}

Searching for PRs merged after 2025-11-04...
Found 1234 candidate PRs to analyze
Analyzing PR 1234/1234...
Found 5 potentially relevant follow-up PRs

================================================================================
Follow-up PRs for #50799
================================================================================

Found 5 potential follow-up PRs:

1. PR #50850 (score: 52.3)
   URL: https://github.com/quarkusio/quarkus/pull/50850
   - Modifies same files: StreamingOutputMessageBodyWriter.java
   - Same area labels: area/rest, area/security
   - Bug fix in same component
   - Merged 3 days after source PR

2. PR #50912 (score: 45.8)
   URL: https://github.com/quarkusio/quarkus/pull/50912
   - Modifies same files: StreamingOutputMessageBodyWriter.java
   - Same area labels: area/rest
   - Merged 8 days after source PR

3. PR #51000 (score: 33.5)
   URL: https://github.com/quarkusio/quarkus/pull/51000
   - References same issues: #50754
   - Same area labels: area/rest
   - Follow-up keywords: follow-up
   - Merged 15 days after source PR

4. PR #51123 (score: 28.0)
   URL: https://github.com/quarkusio/quarkus/pull/51123
   - Modifies same files: StreamingOutputErrorHandlingTest.java
   - Same area labels: area/rest
   - Merged 21 days after source PR

5. PR #51200 (score: 22.7)
   URL: https://github.com/quarkusio/quarkus/pull/51200
   - Same area labels: area/rest, area/security
   - Bug fix in same component
   - Merged 25 days after source PR
```

## Interpreting Results

### High-Confidence Follow-ups (Score > 40)

PRs with scores above 40 typically:
- Modify the exact same files
- Are in the same functional area
- Were merged shortly after the source PR
- Are likely bug fixes or improvements to the original change

**Action**: These should almost certainly be backported with the original PR.

### Medium-Confidence Follow-ups (Score 20-40)

PRs in this range:
- May modify some of the same files
- Reference related issues
- Are in the same general area
- Could be related improvements or adjacent changes

**Action**: Review these carefully to determine if they're true follow-ups.

### Low-Confidence Follow-ups (Score < 20)

PRs with low scores:
- Might share area labels but change different code
- Could be coincidentally similar
- May not be directly related

**Action**: These are less likely to need backporting but should be reviewed if they have relevant keywords or issue references.

## Using JSON Output

For programmatic processing, use the `--json` flag:

```bash
python3 find-followup-prs.py --pr 50799 --json > pr-50799-followups.json
```

Example JSON output:

```json
{
  "source_pr": 50799,
  "followups": [
    {
      "pr_number": 50850,
      "score": 52.3,
      "reasons": {
        "same_files": ["StreamingOutputMessageBodyWriter.java"],
        "same_area_labels": ["area/rest", "area/security"],
        "bugfix_same_component": [true],
        "time_proximity_days": 3
      },
      "url": "https://github.com/quarkusio/quarkus/pull/50850"
    },
    {
      "pr_number": 50912,
      "score": 45.8,
      "reasons": {
        "same_files": ["StreamingOutputMessageBodyWriter.java"],
        "same_area_labels": ["area/rest"],
        "time_proximity_days": 8
      },
      "url": "https://github.com/quarkusio/quarkus/pull/50912"
    }
  ]
}
```

## Backporting Workflow

1. **Identify the source PR** that needs to be backported to an LTS branch
   ```bash
   # Example: We want to backport PR #50799 to the 3.15 branch
   ```

2. **Find follow-up PRs**
   ```bash
   python3 find-followup-prs.py --pr 50799 --json > followups.json
   ```

3. **Review each follow-up PR**
   - Check the changes in each identified PR
   - Determine if they fix bugs, add improvements, or are unrelated
   - Decide which ones should be backported

4. **Create backport PRs**
   - Cherry-pick the source PR and all relevant follow-ups to the LTS branch
   - Test the backported changes together
   - Create a PR with all changes

5. **Document in PR description**
   ```markdown
   Backport of:
   - #50799 - Fix StreamingOutput connection reset on mid-stream errors
   - #50850 - Additional fix for StreamingOutput error handling
   - #50912 - Improve StreamingOutput test coverage
   
   Related issues: #50754
   ```

## Tips

- **Always review results manually** - The tool provides suggestions, but human judgment is needed
- **Check commit messages** - Read the actual PR descriptions to understand the changes
- **Test together** - When backporting multiple PRs, test them as a unit
- **Consider dependencies** - Some follow-ups may depend on the original change
- **Time window matters** - Follow-ups are usually merged within 30 days of the original PR

## Notes for PR #50799

Since PR #50799 added new test infrastructure and fixed a bug:
- Look for test fixes or improvements in `StreamingOutputErrorHandlingTest.java`
- Check for additional error handling improvements in `StreamingOutputMessageBodyWriter.java`
- Watch for any PRs that reference issue #50754 or PR #50799 directly
- Pay attention to PRs labeled with `area/rest` and `triage/flaky-test`

## Limitations

The tool cannot:
- Guarantee 100% accuracy (false positives/negatives are possible)
- Understand complex semantic relationships between changes
- Detect follow-ups that don't modify files or reference issues
- Know about manual testing or runtime dependencies

Always use human judgment when deciding what to backport!
