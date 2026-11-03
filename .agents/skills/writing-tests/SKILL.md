---
name: writing-tests
description: >
  Testing patterns for Quarkus extensions: test annotations, test locations,
  QuarkusExtensionTest patterns, and how to run tests.
---

# Writing Tests

Quarkus uses JUnit 5 with custom extensions. Tests and documentation are
mandatory for contributions.

## Test Annotations

- **`@QuarkusTest`** — Starts a full Quarkus application. Use for integration
  tests in `integration-tests/`.
- **`@QuarkusIntegrationTest`** — Tests against a built artifact (JAR or native
  binary). In the main repo, most run only with `-Dnative`.

## Test Extensions (used with `@RegisterExtension`)

- **`QuarkusExtensionTest`** — Used in **deployment module** tests. Creates a
  synthetic application defined in the test. This is the primary way to test
  build-time behavior. Replaces the deprecated `QuarkusUnitTest`.
   - Never add a new `QuarkusUnitTest`. When you edit a test that still
     uses it, move it to `QuarkusExtensionTest` in the same change.
   - When writing these tests, if Quarkus configuration needs to be specified, prefer the `overrideConfigKey` and `overrideRuntimeConfigKey` methods of `QuarkusExtensionTest`   
- **`QuarkusDevModeTest`** — Tests hot reload / dev mode behavior.

## Test Location

- **Extension tests** for deployment logic: `extensions/<name>/deployment/src/test/`
- **Integration tests** needing a running app: `integration-tests/`
- Deployment module tests use `QuarkusExtensionTest`, NOT `@QuarkusTest`

## QuarkusExtensionTest Pattern

```java
@RegisterExtension
static final QuarkusExtensionTest config = new QuarkusExtensionTest()
    .withApplicationRoot((jar) -> jar
        .addClasses(MyResource.class, MyService.class));

@Test
void testFeature() {
    // test with RestAssured or similar
}
```

If the tests have to set Quarkus configuration, then using `QuarkusExtensionTest#overrideConfigKey` for build time configuration and `QuarkusExtensionTest#overrideRuntimeConfigKey` for runtime configuration is preferable. 

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

## Native Test Registration

Native tests are split into parallel categories for CI performance. Each new
integration test module **must** be registered in `.github/native-tests.json`
to have its native tests run in CI. Without this, `-Dnative` tests will not
execute for the module.

Note: `@QuarkusIntegrationTest` tests in the main repo only run when
`-Dnative` is passed — even `verify` with `-DskipITs=false` will not
trigger them.

## MicroProfile TCK Tests

The `tcks/` module contains MicroProfile TCK tests (Config, JWT, Fault
Tolerance, Health, Metrics, OpenAPI, Telemetry, REST Client, Reactive
Messaging, Context Propagation). If your work touches any of these areas,
run the TCKs:

```bash
# Run all TCKs
./mvnw verify -f tcks/ -Ptcks

# Run a specific TCK
./mvnw verify -f tcks/<area>/ -Ptcks
```

## Assertions

- **Prefer AssertJ** (`org.assertj.core.api.Assertions.assertThat`) over JUnit 5
  assertions (`org.junit.jupiter.api.Assertions`). AssertJ provides fluent,
  readable assertions and better failure messages.
- If the module has no AssertJ on its test classpath, you can add the
  dependency. `build-parent` manages the version, so the dependency needs no
  `<version>`:

  ```xml
  <dependency>
      <groupId>org.assertj</groupId>
      <artifactId>assertj-core</artifactId>
      <scope>test</scope>
  </dependency>
  ```
- Use the assertion that names the check, because its failure message shows
  the values: `containsEntry(key, value)` instead of `get(key)` and
  `isEqualTo`, `hasSize(2)` instead of `size()` and `isEqualTo(2)`,
  `assertThatThrownBy(...)` or `assertThatExceptionOfType(...)` instead of
  `assertThrows`.
- A bare `isTrue()` fails with "expected true". Assert the value itself, or
  describe the check with `as("...")`.
- Use RestAssured for HTTP endpoint testing. Its Hamcrest matchers in
  `body(...)` are fine.

## What a Test Covers

- When a fix depends on configuration, test each combination of the
  properties that it reads, and the case where none is set. For example, a
  fix that reads a TLS configuration name and credentials needs four tests:
  both set, the name only, the credentials only, and neither.
- Also test the error paths, for example the status code when the request
  body is missing.
- Assert what the user sees: the response body, the content type or the
  message text. A request that does not fail does not prove the fix.
- A streaming test sends more data than the heap of the test JVM. With a
  small payload, the test also passes when the code buffers all the data in
  memory.

## Test Utilities and Resources

- Before you write a test helper, look in the module's test sources and in the
  test utilities of the extension, for example `io.quarkus.micrometer.test.Util`.
  Reuse them.
- Do not use `java.net.http.HttpClient` or a raw `Socket`. Use RestAssured,
  the client of the module such as the WebSockets Next `WSClient`, or the
  Vert.x client.
- Do not start a `com.sun.net.httpserver.HttpServer` as a test server. Serve
  the endpoint from the test application: a Jakarta REST resource or a route
  that observes the Vert.x `Router`. A Vert.x HTTP server is also fine.
- Use Mockito mocks only as a last resort. A test that mocks the Quarkus code
  under test does not prove that the feature works. Test the real code in an
  application instead.
- To test a client of a remote service, mock the remote server with WireMock,
  `org.wiremock:wiremock-standalone`. `build-parent` manages its version.
  WireMock replaces the server, not the Quarkus code.
- Do not use `Thread.sleep` to wait for a result. Wait for the condition with
  Awaitility: `await().atMost(...).untilAsserted(...)`.
- Close every client and server that a test creates. Use try-with-resources
  when the type is `AutoCloseable`. Otherwise close it in `@AfterEach` or
  `@AfterAll`, in the same lifecycle that created it. An open client keeps
  its Vert.x instance and its threads.

## Key Rules

- Do NOT use `@QuarkusTest` in deployment module tests — use `QuarkusExtensionTest`
- Integration tests belong in `integration-tests/`, not in extension modules
- Use RestAssured for HTTP endpoint testing
- Test in both JVM and native mode for non-trivial changes
- New native tests must be registered in `.github/native-tests.json`
- Verify that build-time errors produce clear, actionable error messages
- Container engine (Docker/Podman) is needed for dev-services tests
- Parallel test execution is not supported
- When writing a test that verifies a bug has been fixed, ensure that the test has run against the code that does not contain the fix and verify this test fails. This provides some confidence that the test does actually verify the fix; in other words, this addresses the `never trust a test you haven't seen fail` adage
- A fix must not change the behavior of paths it does not target. Test
  those paths too, and assert the behavior of `main`. Run the test class
  on `main` and with the fix: only the tests of the fixed path may differ.

## HTTP Clients Other Than RestAssured

RestAssured uses Apache HttpClient 4, which has no HTTP/2 support.
rest-assured/rest-assured#1696 tracks the move to HttpClient 5. Until
RestAssured supports HTTP/2, use the Mutiny Vert.x web client for HTTP/2,
h2c or other protocol-level tests:

- Add `io.smallrye.reactive:smallrye-mutiny-vertx-web-client` with
  `test` scope. The Quarkus BOM manages its version.
- Inject the managed `io.vertx.mutiny.core.Vertx`. Do not create a Vert.x
  instance in the test.
- Wait with `await().atMost(timeout)` on every request. Do not use
  `CompletableFuture` or `UniHelper`.
- Create the `WebClient` in the test that uses it, and close it at the end
  of that test, in a `finally` block or in `@AfterEach`. The Mutiny
  `WebClient` is not `AutoCloseable`. The protocol is an option of the
  `WebClient`, not of a request. A test of another protocol creates its own
  client.
- In Vert.x 5, the Mutiny web client uses `io.vertx.core.buffer.Buffer`.
