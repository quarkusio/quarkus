---
name: pull-requests
description: >
  Rules for preparing pull requests and commits in the Quarkus project:
  title conventions, description format, commit hygiene, labels, and
  contribution policies.
---

# Pull Requests

## PR Title

Write a clear, natural-language sentence that describes the change:

- **Start with an uppercase letter**
- Do **not** prefix with `feat:`, `fix:`, `chore:`, `docs:`, `refactor:`, or
  any other Conventional Commits type — the Quarkus bot will flag this
- Keep it concise but descriptive
- Do not end with a period

### Examples

```
Add multipart upload support to RESTEasy Reactive
Fix NPE when config property is unset
Update OIDC provider configuration guide
Simplify bean resolution logic in ArC
```

## PR Description

- Explain **why** the change is needed, not what changed (the diff shows that)
- Keep it concise — a short paragraph is usually enough
- Do **not** include a "Test plan" or "Summary" section
- Do **not** include "Generated with Claude Code" or similar footers
- Do **not** include `Co-Authored-By` trailers referencing an AI tool

### Noteworthy features

If your PR introduces a noteworthy feature, include a single line at the end of
the description prefixed with `Release note:` that can be used verbatim in the
release notes. Keep it user-facing — focus on the benefit, not the implementation:

```
Release note: RESTEasy Reactive now supports multipart file uploads with
progress tracking via the new `@PartProgress` annotation.
```

### Breaking changes

If the change is breaking, explain in the description:
1. What breaks
2. Why it was necessary
3. How users should migrate

## Commits

- Commits must be **atomic and semantic** — each commit should represent one
  logical change
- Properly **squash** fixup commits before the PR is ready for merge. Fixup
  commits are acceptable during review, but must be squashed at the end
- Write commit messages as clear, natural-language sentences starting with an
  uppercase letter — same style as the PR title
- Do **not** add `Co-Authored-By` trailers referencing AI tools (e.g.
  `Co-Authored-By: Claude`, `Co-Authored-By: Copilot`)

## Labels

Apply these labels when relevant:

| Label | When to apply |
|-------|---------------|
| `release/noteworthy-feature` | The PR introduces a significant user-facing feature that should appear in release notes |
| `release/breaking-change` | The PR contains a breaking change (API removal, behavior change, config rename) |

## Before Submitting

1. Run `./mvnw process-sources` on changed modules to fix formatting
2. Run tests in Java mode (`./mvnw verify -f extensions/<name>/`)
3. For non-trivial changes, test in native mode (`-Dnative`)
4. If adding a new integration test module, register it in
   `.github/native-tests.json`
5. Update documentation in `docs/src/main/asciidoc/` when the change affects
   user-facing behavior, config, or APIs

## LLM / Agent Policy

- You are responsible for validating every change you submit — AI-assisted or not
- Do not submit code, tests, or descriptions copied directly from an LLM without
  human review
- AI-generated tests must validate actual behavior, not just exercise code paths
- Do not use bots or agents to open PRs without human authorship and responsibility
