# Build System

Quarkus uses Maven. The full build is large; know how to build selectively.
Set `MAVEN_OPTS="-Xmx4g"` for builds.

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

See [testing.md](testing.md) for test commands and conventions.

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

A [justfile](../.justfile) provides convenience aliases. Run `just -l` to see them.
By default uses `./mvnw -T0.8C` (parallel build). Set `QMVNCMD` to override the
Maven command (e.g., `QMVNCMD=mvnd` for Maven Daemon).

## Formatting

See [coding-style.md](coding-style.md) for formatting rules and commands.

## Important Build Rules

- Always `install` (not just `compile`) when building modules that other modules depend on
- If you change a runtime module, rebuild its deployment module too
- If you change something in `core/`, downstream extensions may need rebuilding
- Use `./mvnw` (the wrapper) to ensure the correct Maven version
- After adding/removing/renaming an extension, run `update-extension-dependencies.sh`
