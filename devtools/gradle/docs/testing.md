# Testing the Gradle plugins

## Audience and scope

This page is for contributors selecting or adding tests for the standalone
application plugin, shared Gradle model, extension plugins, or their Quarkus
integration. It defines test-tier and expensive-fixture ownership. It is not a
list of every test command supported by the repository.

Test class names and fixture layout are implementation details. The behaviors
that guard CC/IP, cacheability, model/variant contracts, process cleanup, and
external protocols are durable quality gates.

## Test tiers

| Tier | Appropriate ownership |
| --- | --- |
| Plain unit | planners, codecs, DTO validation, state transitions, path rules |
| ProjectBuilder | task/configuration/extension registration without nested Gradle |
| TestKit | real Gradle DSL, task graph, configuration cache, Isolated Projects, build cache |
| Tooling API | parameterized models, sidecar pairing, IDE/code-generation requests |
| Integration test | real Quarkus augmentation, launch, remote dev, provider, composite build |
| Environment-gated | native-image, container, startup archive, operating-system behavior |

Put a behavior at the lowest tier that proves it, then retain one
integration-level journey for cross-module wiring. Do not replace a
protocol/state-machine unit test with a slow application build.

## Configuration cache, Isolated Projects, and build cache

Every TestKit scenario owned by the standalone plugin should enable
configuration cache and Isolated Projects unless it is specifically proving a
Gradle incompatibility. Cacheable task scenarios also execute a build-cache
reuse path.

Continuous build is special: Gradle currently does not promise configuration
cache reuse between iterations. Its fixture still verifies that configuration
is compatible and that no task action captures live Gradle state.

Tests should fail on configuration-cache problems rather than filtering them
from output. A second invocation proves reuse for finite tasks.

## Scenario and fixture ownership

Each test owns the application, process, server, temporary directory, and
cleanup needed by its assertion. Never share a live Quarkus application or
rely on JUnit method ordering.

Reusable fixtures are appropriate for:

- immutable build-script/source templates;
- TestKit plugin-classpath calculation;
- bounded Gradle/process launch helpers;
- synthetic Maven repository setup with explicit close; and
- assertion helpers that do not hide another Gradle invocation.

Every nested Gradle build, Tooling API connection, Quarkus process, HTTP
server, and container launch must be visible in the scenario's ownership. A
class split must not duplicate expensive setup just to shorten a source file.

## Runtime ownership

Plugin tests are designed to keep runtime bounded:

- registration and validation stay in unit/ProjectBuilder tests;
- a TestKit fixture combines compatible assertions within one nested build;
- Tooling API tests reuse immutable source setup, not live connections;
- integration journeys share build preparation only when the Maven harness
  already owns that lifecycle; and
- native/container/startup scenarios run only behind their established
  environment gates.

When changing fixtures, measure the same forced, no-build-cache command before
and after. Record task count, test count, elapsed range, and median. A faster
single run is not evidence if it removed coverage or relied on warm undeclared
state.

The aggregate under `devtools/gradle` covers:

- `gradle-model`;
- legacy `gradle-application-plugin`;
- `gradle-extension-plugin`;
- `gradle-extension-deployment-plugin`; and
- standalone `gradle-app-plugin`.

Run those test modules sequentially. The Quarkus build and test suite does not
support concurrent test-module invocations because live tests may collide on
ports.

## Standalone plugin behavioral owners

The standalone suite should retain focused owners for:

- plugin application, coexistence, task-name collision, and public DSL reach;
- strict model/POM/classpath and local/composite extension resolution;
- Java/Kotlin/IDE code generation;
- package formats, package/launcher variants, and manifest behavior;
- run, finite tests, integration tests, and named native tests;
- image references, build/push, deployments, and startup archives;
- dev/continuous-test failure, recovery, cancellation, Dev UI, and replay;
- HTTP remote-dev snapshot, reconnect, authentication, and containment; and
- worker classloader/process isolation.

Mocked native TestKit scenarios lock in task wiring and execution behavior
without requiring a native image for every plugin test. A real
environment-gated native integration journey proves the end-to-end provider.

## Integration journeys

New-plugin journeys under `integration-tests/gradle` cover:

- composite library and extension builds;
- extension deployment test-model handoff;
- IDE code generation and Tooling API sidecar correlation;
- multi-module uber JAR and package formats;
- package-directory consumption by another project;
- run and remote-development lifecycle;
- host and Jib startup-archive training;
- named native build/test execution; and
- package and tooling behavior across multi-project and composite builds.

Integration tests should validate framework wiring that cannot be proven in
TestKit. They should not repeat every DSL validation already owned by plugin
tests.

## Failure, timeout, and cleanup assertions

Long-running tests use event-driven waits with explicit timeouts. Arbitrary
sleeps are not acceptable synchronization. Failure messages must identify the
awaited state and retain bounded child output.

Every scenario that launches state must verify closure:

- continuous cancellation releases the output tree;
- dev/remote deployment handles write close receipts;
- TCP and HTTP workers terminate;
- container or native helper processes are stopped; and
- temporary server executors are closed.

Windows assertions compare canonical/normalized paths and do not assume a
Unix root or separator.

## Platform and environment qualifications

macOS, Windows, and Linux differ in filesystem semantics and process timing.
Native and container tests require the matching toolchain. OpenJDK AOT,
AppCDS, and OpenJ9 SCC tests require the corresponding JVM. A gated test must
report a skip reason rather than silently pass.

## Source and test ownership

Primary fixture owners:

- `devtools/gradle/gradle-app-plugin/src/test`
- shared `BaseGradleTest` fixtures in the Gradle plugin modules
- `integration-tests/gradle/src/test/java/io/quarkus/gradle/application`
- protocol and remote-dev tests under `core/deployment/src/test`
- bootstrap model/resolver tests under `independent-projects/bootstrap`

Aggregate and module commands are maintained in the repository build scripts
and the root contributor build guidance. Documentation link-check fixtures
belong to `devtools/gradle/build-logic`.
