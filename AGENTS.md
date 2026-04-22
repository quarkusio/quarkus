# Quarkus Coding Conventions

Essential rules for working on the Quarkus codebase. Detailed guidance for
specific tasks is available in `.agents/skills/` — consult the relevant skill
when performing that type of work.

## General Principles

- **Update documentation.** When changes affect user-facing behavior, config, or
  APIs, update the relevant `.adoc` files in `docs/src/main/asciidoc/`.
- **Add or update tests.** Bug fixes need a reproducer test. New features need
  tests. Test in both JVM and native mode for non-trivial changes.
- **You are responsible for what you submit.** Validate all changes. Do not
  submit AI-generated code without human oversight.

## Project Structure

Quarkus is a large multi-module Maven project:

- `core/` — Core framework (builder, deployment, runtime, launcher)
- `extensions/` — 200+ extensions (each has `runtime/` and `deployment/` modules)
- `devtools/` — Maven/Gradle plugins, CLI
- `independent-projects/` — Standalone sub-projects (ArC CDI, Qute, RESTEasy Reactive)
- `integration-tests/` — Integration tests
- `test-framework/` — JUnit 5 test utilities
- `docs/` — Asciidoc documentation
- `adr/` — Architecture Decision Records

### Extension Module Layout

Every extension lives under `extensions/<name>/` with at minimum:

- `runtime/` — Runtime classes, recorders, beans (`quarkus-<name>`)
- `deployment/` — `@BuildStep` processors (`quarkus-<name>-deployment`)

Optional modules: `runtime-api/`, `deployment-spi/`, `runtime-dev/`, `codegen/`, `cli/`, `codestart/`.

**Deployment depends on runtime, NEVER the reverse.**

## Classloading (Critical)

Quarkus has a split classloading model — the #1 source of mistakes:

- **Runtime code MUST NOT reference deployment classes.** Deployment modules are
  not on the classpath at runtime. Violations cause `ClassNotFoundException`.
- **Deployment code CAN reference runtime classes.**
- **Recorders bridge the gap.** A `@Recorder` lives in the runtime module but is
  invoked from deployment build steps — it generates bytecode that runs at runtime.

## Build Commands

```bash
./mvnw -Dquickly                           # Quick full build (skip tests/docs/native)
./mvnw install -f extensions/<name>/       # Build one extension
./mvnw install -f core/ -DskipTests        # Build core
./mvnw verify -f extensions/<name>/        # Run extension tests
./mvnw test -Dtest=MyTest                  # Run single test
./mvnw verify -f integration-tests/<name>/ -Dnative  # Native tests
```

Set `MAVEN_OPTS="-Xmx4g"`. Always use `install` (not just `compile`).
If you change a runtime module, rebuild its deployment module too.

| Flag | Purpose |
|------|---------|
| `-Dquickly` | Skip tests, ITs, docs, native, validation |
| `-Dnative` | Build and test native image |
| `-Dno-format` | Skip formatting check |
| `-DskipTests` | Skip unit tests |
| `-Dincremental` | Only build changed modules |

## Coding Style Essentials

- 4-space indentation, enforced by `formatter-maven-plugin` — run
  `./mvnw process-sources` to auto-format
- Use **JBoss Logging** (`org.jboss.logging.Logger`), not SLF4J/JUL/Log4j
- No `@author` tags, no wildcard imports
- Naming: `<Feature>Processor.java`, `<Feature>Recorder.java`,
  `<Description>BuildItem.java`, `<Feature>Config.java`
- Package root: `io.quarkus.<extension-name>` (hyphens become underscores)
- Use `@ConfigMapping` interfaces — legacy `@ConfigRoot` classes with
  `@ConfigItem` are removed. New `quarkus.*` properties must always be
  registered in a `@ConfigMapping`, even if read programmatically. See the
  `working-with-config` skill for details.

## On-Demand Skills

Detailed guidance is available in `.agents/skills/` for specific tasks.
Consult the relevant skill when you are about to do that type of work:

| Skill | When to use |
|-------|-------------|
| `writing-build-steps` | Creating or modifying `@BuildStep` methods, build items, or recorders |
| `writing-tests` | Creating or modifying tests for Quarkus extensions |
| `working-with-config` | Creating or modifying `@ConfigMapping` configuration interfaces |
| `classloading-and-runtime-dev` | Working with runtime-dev modules, conditional dependencies, or debugging classloading |
| `creating-extensions` | Creating a new extension or understanding the full module layout |
| `coding-style` | Code formatting, visibility, naming conventions, and logging |
| `building-and-testing` | Maven build commands, flags, incremental builds, and build rules |
| `pull-requests` | PR title/description conventions, commit hygiene, labels, and contribution rules |
