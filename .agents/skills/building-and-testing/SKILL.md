---
name: building-and-testing
description: >
  How to build and test Quarkus: Maven commands, build flags, incremental
  builds, justfile aliases, and important build rules.
---

# Build System

Quarkus uses Maven. The full build is large; know how to build selectively.
Ensure `MAVEN_OPTS` includes at least `-Xmx4g` for builds (do not override the user's existing `MAVEN_OPTS`).

## Quick Build (First Time)

```bash
./mvnw -Dquickly
```

This skips tests, ITs, docs, native, and validation.

## Building a Specific Extension

```bash
# All modules of an extension
./mvnw install -f extensions/<name>/

# A single module
./mvnw install -f extensions/<name>/deployment
```

## Building Core

```bash
./mvnw install -f core/ -DskipTests
```

## Running Tests

```bash
# Run tests for an extension
./mvnw verify -f extensions/<name>/

# Run a single test class
./mvnw test -f integration-tests/<name>/ -Dtest=MyTest

# Run a single test method
./mvnw verify -Dtest=fully.qualified.ClassName#methodName

# Native integration tests
./mvnw verify -f integration-tests/<name>/ -Dnative
```

## Incremental Build

```bash
# Build only changed modules and their downstream dependencies
./mvnw install -Dincremental
```

## Key Maven Flags

| Flag | Purpose |
|------|---------|
| `-Dquickly` | Skip tests, ITs, docs, native, validation |
| `-DquicklyDocs` | Like `-Dquickly` but includes docs |
| `-Dnative` | Build and test native image |
| `-Dno-format` | Skip code formatting check |
| `-DskipTests` / `-DskipITs` | Skip unit / integration tests |
| `-Dno-test-modules` | Skip building test modules |
| `-Dincremental` | Only build changed modules |

## Justfile

A justfile (`.justfile` in the repo root) provides convenience aliases. Run `just -l` to
see them. By default uses `./mvnw -T0.8C` (parallel build). Set `QMVNCMD`
to override the Maven command (e.g., `QMVNCMD=mvnd` for Maven Daemon).

## Important Build Rules

- Always `install` (not just `compile`) when building modules that other modules depend on
- If you change a runtime module, rebuild its deployment module too
- If you change something in `core/`, downstream extensions may need rebuilding
- Use `./mvnw` (the wrapper) to ensure the correct Maven version
- After adding/removing/renaming an extension, run `update-extension-dependencies.sh`
