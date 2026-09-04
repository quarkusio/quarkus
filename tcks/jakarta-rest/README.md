# Jakarta REST TCK 4.0.1 Runner for Quarkus REST

This module runs the official [Jakarta REST TCK 4.0.1](https://github.com/jakartaee/rest/tree/4.0.1/jaxrs-tck)
against Quarkus REST using `quarkus-arquillian`.

## Running

The TCK is not part of the default build. Activate it with:

```shell
mvn verify -pl tcks/jakarta-rest -Drun-jakarta-rest-tck
```

## Architecture

- **quarkus-arquillian** restarts Quarkus for every test class via Arquillian's
  `deploy()`/`undeploy()` lifecycle.
- **Ephemeral ports** (`quarkus.http.test-port=0`) avoid port conflicts between
  test classes. The actual port is read from `ProtocolMetaData` after deploy and
  set as `System.setProperty("webServerPort", ...)`.
- **JUnit 5 `ExecutionCondition`** (`QuarkusRestTckDisabledTests`) disables tests
  that were already excluded in the old forked TCK runner (resteasy-reactive-testsuite).
  The `DisableReason` enum mirrors the old `QuarkusRest.java` classification.

## Current status (2026-07-31)

| Metric   | Count |
|----------|-------|
| Total    | 2755  |
| Pass     | 2477  |
| Error    | 0     |
| Failure  | 0     |
| Skipped  | 278   |

**278 skipped** = old TCK exclusions carried forward via `ExecutionCondition` +
`@Tag("se_bootstrap")`/`@Tag("servlet")` excluded via `excludedGroups` + signature test.

## Files

| File | Purpose |
|------|---------|
| `pom.xml` | TCK dependency, failsafe configuration, profile activation |
| `QuarkusRestTckArchiveProcessor.java` | Arquillian observer: injects `application.properties`, reads ephemeral port |
| `QuarkusRestTckDisabledTests.java` | JUnit 5 `ExecutionCondition` — disables old TCK exclusions |
| `DisableReason.java` | Enum of exclusion reason categories (mirrors old `QuarkusRest.java`) |
| `META-INF/services/org.junit.jupiter.api.extension.Extension` | Service loader for auto-detection |
| `junit-platform.properties` | Enables JUnit 5 extension auto-detection |
| `arquillian.xml` | Arquillian configuration for Quarkus |
