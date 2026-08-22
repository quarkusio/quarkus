# Quarkus Gradle developer reference

## Audience and scope

This reference is for contributors maintaining Quarkus Gradle plugins, the
shared Gradle application model, or framework services used by those plugins.
It describes the current implementation contracts and the boundaries that
must survive refactoring. It is not an end-user guide or a migration guide.

The standalone `io.quarkus.application` plugin is the primary subject. The
legacy `io.quarkus` application plugin appears only where coexistence or
tooling ownership requires a precise boundary. User-facing setup belongs in
the [standalone Gradle application plugin guide](https://quarkus.io/guides/gradle-application-plugin).
User-facing setup for the `io.quarkus` plugin remains in the
[Quarkus Gradle guide](https://quarkus.io/guides/gradle-tooling).

Unless a page explicitly says otherwise, a documented implementation shape is
not a source- or binary-compatibility promise. Public DSL, registered task
properties and options, serialized schemas, and consumed Gradle variants have
stronger contracts than package-private coordinators and receipt files.

## Index

- [Application plugin architecture](application-plugin-architecture.md)
- [Application model and variants](application-model-and-variants.md)
- [Tasks, outputs, and receipts](task-output-and-receipt-model.md)
- [Development and remote lifecycle](development-and-remote-lifecycle.md)
- [External build-output protocol](external-build-output-protocol.md)
- [Tooling model](tooling-model.md)
- [Extension plugins](extension-plugins.md)
- [Startup archives and images](startup-archive-and-images.md)
- [Testing](testing.md)
- [Durable design decisions](decisions.md)

## Ownership map

| Area | Primary module or package |
| --- | --- |
| Standalone plugin, DSL, named builds, tasks | `devtools/gradle/gradle-app-plugin` |
| Strict normalized Gradle model and POM closure | `devtools/gradle/gradle-model` |
| Legacy application plugin | `devtools/gradle/gradle-application-plugin` |
| Extension runtime-module plugin | `devtools/gradle/gradle-extension-plugin` |
| Extension deployment-module plugin | `devtools/gradle/gradle-extension-deployment-plugin` |
| Application-model sidecar schema | `independent-projects/bootstrap/app-model` |
| Tooling API paired-model action and routing | `independent-projects/bootstrap/gradle-resolver` and `independent-projects/bootstrap/core` |
| External build-output transport and HTTP remote-dev client | `core/deployment` |
| HTTP remote-dev runtime endpoint | `extensions/vertx-http/runtime` |
| Startup-archive provider integration | `core/deployment`, `extensions/container-image`, and `test-framework` |
| New-plugin integration journeys | `integration-tests/gradle` |

## Maintenance rules

- Update the owning page when a task graph, variant, schema, lifecycle, or
  cross-module responsibility changes.
- Link to one canonical explanation instead of copying a contract between
  pages. In particular, model semantics, tooling correlation, session
  lifecycle, and wire protocol each have one owner.
- Describe final behavior. Record failed approaches only when their constraint
  remains useful in the decisions page.
- Keep temporary investigations, local paths, review identifiers, and
  CI-specific diagnostics out of this directory.
- Run `./gradlew --no-parallel verifyDeveloperDocs` from `devtools/gradle`
  after editing these pages. The verifier is intentionally explicit rather
  than attached to every subproject's `check` task.

## Platform qualifications

The plugins enforce the generated minimum supported Gradle version. Build
logic must remain compatible with Gradle configuration cache and Isolated
Projects. Continuous-build behavior additionally depends on Gradle's
continuous execution facilities and currently cannot assume configuration
cache reuse between iterations. Native-image, container, startup-archive, and
platform-specific tests are gated by the required toolchain or runtime.

## Source and test ownership

The index and its link contract are verified by
`devtools/gradle/build-logic/src/main/kotlin/io/quarkus/devtools/docs/VerifyDeveloperDocs.kt`.
The verifier's fixtures live under
`devtools/gradle/build-logic/src/test/resources/developer-docs`.
Behavioral source and test owners are listed on each topic page.
