# Testing

## Test Framework

Quarkus uses JUnit 5 with custom extensions for integration testing.
Tests and documentation are **mandatory** for contributions.

## Test Annotations

- `@QuarkusTest` - Starts a full Quarkus application for the test. Use for integration tests.
- `@QuarkusIntegrationTest` - Tests against the built artifact (JAR or native binary).
  In the Quarkus main repository, most of these are configured to run only with `-Dnative`,
  but the annotation itself supports testing against any packaged artifact.

## Test Extensions (used with `@RegisterExtension`)

- `QuarkusUnitTest` - Used in deployment module tests. Deploys a synthetic application
  defined in the test and verifies build-time and runtime behavior.
- `QuarkusDevModeTest` - Tests hot reload / dev mode behavior.

## Test Location

- **Unit tests** for deployment logic go in `extensions/<name>/deployment/src/test/`
- **Integration tests** that need a running application go in `integration-tests/`
- Tests in the deployment module typically use `@QuarkusUnitTest` to create
  a synthetic application archive

## QuarkusUnitTest Pattern

```java
@RegisterExtension
static final QuarkusUnitTest config = new QuarkusUnitTest()
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

## Key Rules

- Do NOT use `@QuarkusTest` in deployment module tests — use `@QuarkusUnitTest`
- Integration tests should be in `integration-tests/`, not in the extension modules
- Use RestAssured for HTTP endpoint testing
- Test in both JVM and native mode for non-trivial changes
- New native tests must be registered in `.github/native-tests.json`
- Always verify that build-time errors produce clear, actionable error messages
- Container engine (Docker/Podman) is needed for dev-services tests
- Parallel test execution is not supported
