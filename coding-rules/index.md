# Quarkus Coding Rules

This directory contains coding conventions and rules for AI coding assistants (and humans)
working on the Quarkus codebase.

These rules are referenced by tool-specific configuration files in the repository root
(`CLAUDE.md`, `AGENTS.md`, `.cursor/rules/`, etc.) so that any AI coding tool can
understand Quarkus conventions.

Also read `CONTRIBUTING.md` in the project root for the full contributor guide, and
`adr/` for Architecture Decision Records that describe key design decisions.

## General Principles

- **Always update documentation.** When making code changes that affect user-facing
  behavior, configuration, or APIs, update the relevant `.adoc` files in
  `docs/src/main/asciidoc/`. Do not leave documentation out of sync with the code.
- **Always add or update tests.** Code changes should include appropriate test coverage.
  Bug fixes should include a test that reproduces the bug. New features need tests.
  Test in both JVM and native mode for non-trivial changes.
- **You are responsible for what you submit.** Understand and validate all changes.
  Do not submit AI-generated code without human oversight (see LLM Usage Policy
  in `CONTRIBUTING.md`).

## Rule Files

- [project-structure.md](project-structure.md) - Repository layout and module organization
- [build-system.md](build-system.md) - Maven build, profiles, and how to build/test
- [classloading.md](classloading.md) - Quarkus classloading model and restrictions
- [build-items.md](build-items.md) - Build steps, build items, and the extension framework
- [coding-style.md](coding-style.md) - Code style, visibility, naming conventions
- [testing.md](testing.md) - Testing patterns and test framework usage
- [configuration.md](configuration.md) - Configuration approach and conventions

## Contributing

Add new rule files as markdown to this directory and link them from this index.
Rules should be concise and actionable — tell the AI what to do and what to avoid.
Keep rules focused on things that AI agents commonly get wrong or need to know
that aren't obvious from reading the code.
