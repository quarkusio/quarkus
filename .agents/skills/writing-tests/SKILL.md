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
        .addClasses(MyResource.class, MyService.class)
        .addAsResource("application.properties"));

@Test
void testFeature() {
    // test with RestAssured or similar
}
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
- Use RestAssured for HTTP endpoint testing.

## Key Rules

- Do NOT use `@QuarkusTest` in deployment module tests — use `QuarkusExtensionTest`
- Integration tests belong in `integration-tests/`, not in extension modules
- Use RestAssured for HTTP endpoint testing
- Test in both JVM and native mode for non-trivial changes
- New native tests must be registered in `.github/native-tests.json`
- Verify that build-time errors produce clear, actionable error messages
- Container engine (Docker/Podman) is needed for dev-services tests
- Parallel test execution is not supported
