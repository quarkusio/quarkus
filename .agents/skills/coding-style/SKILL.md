---
name: coding-style
description: >
  Quarkus code style conventions: formatting, visibility, naming, logging,
  and general style rules for the Quarkus codebase.
---

# Coding Style

## Formatting

- Quarkus uses 4-space indentation (no tabs)
- The project enforces formatting via `formatter-maven-plugin` and `impsort-maven-plugin`,
  let the formatting plugins do their work, never use `-Dno-format`
- Formatter config: `independent-projects/ide-config/src/main/resources/eclipse-format.xml`
- Run `./mvnw process-sources` on your module to auto-format before committing
- The build **will fail** on formatting violations
- Imports are sorted and organized automatically; do not manually organize imports

## Visibility

- Prefer package-private (default) visibility for internal implementation classes
- Use `public` for user-facing API and for classes/methods that need to be accessed
  across packages (e.g., processors, recorders, build items). Prefer package-private
  for purely internal implementation details
- Use `protected` when designing for extension/subclassing
- Internal classes should go in `.internal` or `.impl` sub-packages
  (e.g., `io.quarkus.<ext>.runtime.internal`)

## Naming

- Deployment processors: `<Feature>Processor.java`
- Recorders: `<Feature>Recorder.java`
- Build items: `<Description>BuildItem.java`
- Config interfaces: `<Feature>Config.java` or `<Feature>BuildTimeConfig.java` / `<Feature>RuntimeConfig.java`
- Artifact IDs: `quarkus-<name>` (runtime), `quarkus-<name>-deployment` (deployment)
- Package root: `io.quarkus.<extension-name>` (hyphens become underscores)

## General Style

- **No `@author` tags** in Javadoc — Git history tracks authorship
- No wildcard imports
- Avoid `static` imports except for well-known patterns (e.g., test assertions)
- **Minimize lambdas and streams in runtime code** — reduces memory footprint for native
- Prefer descriptive method and variable names over comments
- Update existing code comments if your changes make them invalid
- Never remove existing code comments that are still valid
- Keep methods focused and short; extract when complexity warrants it
- Use `Optional` for API return types that may be absent. In internal hot runtime
  code paths, direct null checks are acceptable for performance
- Commits should be atomic and semantic — properly squash before submitting PRs.
  This helps during bisects and makes it easier to revert changes when needed

## Logging

- Use JBoss Logging (`org.jboss.logging.Logger`) as the logging API
- Do NOT use `System.out.println` or `System.err.println`
- Do NOT use SLF4J, java.util.logging, or Log4j directly in Quarkus core/extension code

## Extension Descriptions

Extension descriptions (in `runtime/pom.xml` or `quarkus-extension.yaml`) should:
- Be short (no hover required)
- Describe the function, not the technology
- Start with an action verb, unconjugated (`Connect foo`, not `Connects foo`)
- Not mention "Quarkus" or "extension"
- Not repeat the extension name
