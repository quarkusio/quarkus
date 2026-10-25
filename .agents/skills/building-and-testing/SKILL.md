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
./mvnw verify -f extensions/<name>/ -Dstart-containers -Dtest-containers

# Run a single test class
./mvnw test -f integration-tests/<name>/ -Dstart-containers -Dtest-containers -Dtest=MyTest

# Run a single test method
./mvnw verify -Dstart-containers -Dtest-containers -Dtest=fully.qualified.ClassName#methodName

# Native integration tests
./mvnw verify -f integration-tests/<name>/ -Dstart-containers -Dtest-containers -Dnative
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
| `-Dno-build-cache` | Run the build instead of replaying the Develocity build cache |
| `-DskipTests=false` | Force the tests of modules that Scalpel marks as unchanged |

## Confirm that the tests ran

A green build is not proof that the tests ran. The Develocity build cache
replays a cached result, and Scalpel skips the tests of modules it
considers unchanged. When a test result matters, for example before and
after a change:

```bash
./mvnw verify -f extensions/<name>/ -Dno-build-cache -DskipTests=false -Dstart-containers -Dtest-containers
```

Then look for a `Tests run: N` line for the module in the output, or count
the tests in `target/surefire-reports/TEST-*.xml`. Report the count, not
"the build passed".

## Read a CI failure before acting on it

The `quarkus-bot` status comment on a pull request lists the failing jobs
and the failing tests with a `History` link. Before changing code:

1. Open the `History` link. A test that fails on main and on unrelated
   pull requests is a flake. The change did not break it.
2. Compare with the latest run on main. If the same test fails on main,
   say so in the pull request and do not rework the change.
3. A module that the change does not touch, and that no changed module
   depends on, cannot fail because of the change. Say that too.

Do not push an empty commit to rerun CI. Rebase on main instead, so the
new run includes the fixes that landed since.

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
