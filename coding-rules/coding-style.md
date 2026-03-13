# Coding Style

## Formatting

- Quarkus uses 4-space indentation (no tabs)
- The project enforces formatting via `formatter-maven-plugin` and `impsort-maven-plugin`
- Formatter config: `independent-projects/ide-config/src/main/resources/eclipse-format.xml`
- Run `./mvnw process-sources` on your module to auto-format before committing
- The build **will fail** on formatting violations
- Imports are sorted and organized automatically; do not manually organize imports

## Visibility

- Prefer package-private (default) visibility for internal implementation classes
- Use `public` only for classes and methods that are part of the user-facing API
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
- Keep methods focused and short; extract when complexity warrants it
- Avoid `null` where possible; use `Optional` for return types that may be absent.
  In hot runtime code paths, prefer direct null checks for performance
- Commits should be atomic and semantic — properly squash before submitting PRs

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
