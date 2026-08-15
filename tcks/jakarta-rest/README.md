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

## Current status (2026-07-24)

| Metric   | Count |
|----------|-------|
| Total    | 2755  |
| Pass     | 2470  |
| Error    | 6     |
| Failure  | 4     |
| Skipped  | 275   |

**275 skipped** = old TCK exclusions carried forward via `ExecutionCondition` +
`@Tag("se_bootstrap")`/`@Tag("servlet")` excluded via `excludedGroups` + signature test.

**10 remaining failures** across 5 test classes, with 5 distinct root causes:

### SSE 503 + Retry-After reconnection — missing feature (2 tests)

`jaxrs21.ee.sse.sseeventsource.JAXRSClientIT`:
- `wait2Seconds`
- `defaultWaiting1s`

These are **not** flaky timing tests. `SseEventSourceImpl.connect()` does not
implement HTTP 503 + `Retry-After` automatic reconnection at all. When the server
returns 503, the client treats it as a generic non-successful response, fires the
error handler, and calls `notifyCompletion()` — it never reads `Retry-After` or
schedules a reconnect.

The tests set up a `ServiceUnavailableResource` that returns 503 with
`Retry-After: N` on the first request and sends an SSE event on the retry.
Since Quarkus never retries, the message is never received.

**Fix location**: `SseEventSourceImpl.connect()` in
`independent-projects/resteasy-reactive/client/runtime/…/client/impl/SseEventSourceImpl.java`
(lines ~100-110). Needs to check for status 503, parse the `Retry-After` header,
and schedule a one-time reconnect with that delay.

### Multipart return type restriction (1 error — build-time)

`jaxrs31.ee.multipart.MultipartSupportIT`:

Quarkus augmentation rejects the TCK's `Response`-returning multipart endpoint:
*"Endpoints that produce a Multipart result cannot return
`jakarta.ws.rs.core.Response` — consider returning `RestResponse` instead."*
The TCK uses the standard JAX-RS `Response` type, which Quarkus does not allow.

### Feature/DynamicFeature registration reporting (2 failures)

`jaxrs31.spec.extensions.JAXRSClientIT`:
- `featureIsRegisteredTest`
- `dynamicFeatureIsRegisteredTest`

`Configuration.isRegistered()` returns `false` for Feature/DynamicFeature
instances that are actually registered. The features work, but the registration
query does not report them.

### Provider visibility — no no-arg constructor (4 errors)

`spec.provider.visibility.JAXRSClientIT`:
- `bodyWriterTest`
- `bodyReaderTest`
- `contextResolverTest`
- `exceptionMapperTest`

TCK provider classes (`DummyWriter`, `StringReader`, `HolderResolver`,
`VisibilityExceptionMapper`) have only `@Context`-parameter constructors.
ArC's `BeanFactory` requires a no-arg constructor and fails with
`NoSuchMethodException`.

### Resource constructor visibility (1 error)

`spec.resourceconstructor.JAXRSClientIT#visibleTest`:

`GET /resource/mostAttributes` returns HTTP 406 instead of 200. Quarkus ignores
non-public resource methods, so the expected constructor/method is not selected.

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
