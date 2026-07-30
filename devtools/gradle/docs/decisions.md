# Durable Gradle application-plugin decisions

## Audience and scope

This page records rejected or superseded approaches whose failure explains a
current architectural constraint. It is for maintainers reviewing future
changes, not a chronological project history.

The adopted invariants describe current implementation policy, not an
irrevocable compatibility promise. Each decision names a concrete trigger
that would justify revisiting it.

## Legacy-plugin retrofit

**Context.** The legacy application plugin was designed around mutable Gradle
project state and task actions that assemble Quarkus inputs at execution time.

**Rejected approach.** Incrementally retrofit all legacy tasks for
configuration cache and Isolated Projects while retaining their object graph
and implicit configuration behavior.

**Why it failed.** Legacy task actions retain mutable Gradle model objects and
cross-project configuration behavior that cannot be serialized or isolated
reliably.

**Adopted invariant.** The standalone plugin uses provider-backed normalized
inputs, worker isolation, explicit outputs, and named builds. Coexistence is a
migration boundary; it does not transfer the standalone guarantees to legacy
tasks.

**Revisit trigger.** A separately designed legacy implementation can prove the
same CC/IP behavior without sharing mutable application-plugin state.

**Current owners.** `PluginInternal` and the legacy `QuarkusPlugin`;
`QuarkusApplicationPluginFunctionalTest`,
`QuarkusApplicationPluginCoexistenceFunctionalTest`,
`QuarkusApplicationPublicDslBoundaryTest`, and
`ToolingModelRegistrationTest`.

## Live Gradle model capture and task-time repository resolution

**Context.** Application models and effective POM closure need information
available from Gradle dependency resolution.

**Rejected approach.** Inject `DependencyHandler`, configurations, repository
resolvers, or `Project` into a task action and resolve missing metadata there.

**Why it failed.** Those live Gradle services are not valid isolated task
state, and execution-time resolution hides inputs from configuration-cache
serialization and task fingerprinting.

**Adopted invariant.** Configuration creates artifact views and normalized
providers. Task inputs contain serialized coordinates, files, project
descriptors, and a precomputed POM closure. Actions consume only declared
inputs.

**Revisit trigger.** Gradle exposes a documented, isolatable execution-time
dependency-resolution service that is compatible with configuration cache and
Isolated Projects.

**Current owners.** `StrictApplicationModelTaskConfigurator`,
`ApplicationModelTaskRegistration`, `PomClosureInputCalculator`, and
`GeneratePomClosureTask`; `QuarkusApplicationModelResolutionTest`,
`QuarkusApplicationLocalOutputResolutionTest`,
`QuarkusApplicationConditionalDependencyResolutionTest`,
`QuarkusApplicationPluginTest` for late platform-dependency mirroring,
`PomClosureResultCodecTest`, and
`QuarkusApplicationOfflinePreparationFunctionalTest`.

## Invocation-dependent task graphs

**Context.** Optional work must be avoided when not selected, but Gradle needs
a stable graph for caching and tooling.

**Rejected approach.** Inspect requested task names or dry-run state during
configuration to decide which tasks/configurations exist.

**Why it failed.** Invocation-dependent configuration produces unstable models
for Tooling API clients and invalidates configuration-cache reuse across
otherwise equivalent requests.

**Adopted invariant.** Register task families lazily and make execution
selection flow through providers, `onlyIf` where semantically appropriate,
and ordinary dependencies. Task existence is independent of invocation.

**Revisit trigger.** Gradle introduces a supported declarative mechanism for
invocation-scoped model elements that remains visible to tooling and cache
keys.

**Current owners.** `TaskRegistration`, `DslLifecycleCoordinator`, and the
specialized registration classes under
`io.quarkus.gradle.application.internal.plugin`;
`DslLifecycleCoordinatorTest`, `QuarkusApplicationPluginFunctionalTest`, and
`QuarkusApplicationNamedBuildRegistrationTest`.

## Custom deployment classpath variant

**Context.** Local extension runtime modules need to identify their deployment
project and dependency declarations across project/composite boundaries.

**Rejected approach.** Publish or consume one custom variant containing a
fully assembled deployment classpath.

**Why it failed.** A preassembled classpath loses component identity and
cannot represent runtime/deployment relationships consistently across
same-build and included-build substitution.

**Adopted invariant.** Publish narrow component-aware variants: deployment
project dependency, conditional declarations, runtime role, and deployment
marker. Application model resolution constructs the classpath from these
facts.

**Revisit trigger.** Gradle gains a standard extension runtime/deployment
component model that preserves the same identities and lazy resolution.

**Current owners.** `QuarkusExtensionPlugin`,
`QuarkusExtensionDeploymentPlugin`, `ExtensionVariantConstants`, and
`ApplicationModelResolutionViews`; `QuarkusExtensionPluginTest`,
`QuarkusExtensionDeploymentPluginTest`, and
`QuarkusApplicationToolingLocalExtensionTest`.

## Automatic participation in `assemble`

**Context.** One project can declare multiple named JAR/native outputs, some
expensive or platform-specific.

**Rejected approach.** Make every named build a dependency of `assemble`.

**Why it failed.** It makes unrelated, expensive, or toolchain-specific
outputs run merely because the project lifecycle was requested.

**Adopted invariant.** Each named build opts in independently through a
provider-backed `participatesInAssemble` property.

**Revisit trigger.** A future top-level application publication model defines
one unambiguous primary output and a compatible lifecycle contract.

**Current owners.** `QuarkusApplicationBuild` and
`NamedBuildTaskRegistration`; assemble scenarios in
`QuarkusApplicationNamedBuildRegistrationTest` and
`QuarkusApplicationPluginFunctionalTest`.

## Global manifest map

**Context.** Different named package outputs can require different main
attributes and sections.

**Rejected approach.** Keep one plugin-global map of manifest attributes and
copy it into every package.

**Why it failed.** It cannot model per-build attributes and sections without
order-dependent copying and conflicting overrides.

**Adopted invariant.** Each named build owns a typed Quarkus manifest with
main attributes and named sections. Packaging converts that immutable model at
the worker boundary.

**Revisit trigger.** A shared manifest convention is introduced as an explicit
parent convention while preserving per-build override and section semantics.

**Current owners.** `QuarkusApplicationManifest`,
`QuarkusApplicationManifestSections`, and `ManifestConfigProperties`;
`ManifestConfigPropertiesTest`, `QuarkusApplicationManifestFunctionalTest`,
and `QuarkusApplicationPackageTaskTest`.

## Plugin-owned universal archive

**Context.** A complete package directory is useful for cross-project
consumption and publication, but users need different archive formats.

**Rejected approach.** Make the plugin always ZIP every package or treat the
launcher JAR as the complete package.

**Why it failed.** A launcher may depend on sibling files, while one mandatory
archive format cannot satisfy publication choices such as ZIP64 or TAR.

**Adopted invariant.** Publish a complete directory variant and a separate
producer-local launcher variant. Users or publication plugins choose ZIP,
ZIP64, TAR, or multiple archives.

**Revisit trigger.** Quarkus defines an archive format with compatibility and
distribution semantics beyond ordinary Gradle archive tasks.

**Current owners.** `NamedPackageVariantRegistration` and
`QuarkusApplicationVariantAttributes`;
`QuarkusApplicationCompositePackagingFunctionalTest` and
`QuarkusApplicationCompositePackagingTest`.

## Quarkus-owned continuous compilation

**Context.** Dev mode needs compiled class/resource updates and continuous
testing.

**Rejected approach.** Let the Quarkus child monitor source files or stdin and
invoke compilation independently of Gradle.

**Why it failed.** It bypasses the user's Gradle compilation, code-generation,
and resource graph and cannot reproduce its incremental or failure semantics.

**Adopted invariant.** Gradle continuous build owns code generation,
compilation, resources, failure reporting, and output snapshots. Quarkus owns
runtime application and continuous-test execution. A build-output protocol
connects the two.

**Revisit trigger.** Gradle offers a supported long-lived compilation service
with an explicit external consumer contract that replaces continuous tasks.

**Current owners.** `DevTaskRegistration`, `QuarkusApplicationDevTask`,
`RuntimeUpdatesProcessor`, and `TestSupport`;
`QuarkusApplicationDevContinuousBuildTest`,
`QuarkusApplicationContinuousTestingFailureRecoveryTest`,
`QuarkusApplicationDevUiContinuousBuildTest`,
`QuarkusApplicationContinuousTestingParityTest`, and
`RuntimeUpdatesProcessorBuildOutputChangesTest`.

## Transport and process lifecycle

**Context.** A long-lived Gradle task and Quarkus child need request results,
asynchronous live-reload state, recovery, and bounded shutdown.

**Rejected approach.** Synchronous reads in `send`, callbacks while holding the
socket monitor, uncorrelated replies, or an unbounded child process with no
generation owner.

**Why it failed.** Response reads and callbacks contend with connection state,
allow reentrant deadlocks or miscorrelation, and leave no bounded owner for a
failed child generation.

**Adopted invariant.** One receiver correlates request IDs and asynchronous
state; callbacks run outside connection locks. A deployment supervisor owns
one generation, bounded recovery, cleanup, and termination.

**Revisit trigger.** A Gradle public build-session deployment API or a shared
transport library provides equivalent correlation and lifecycle guarantees.

**Current owners.** `BuildOutputChangesTcpServer`,
`BuildOutputChangesTcpClient`, and
`QuarkusApplicationDevDeploymentHandle`; `BuildOutputChangesTcpClientTest`,
`BuildOutputChangesTransportsTest`, and
`QuarkusApplicationDevDeploymentHandleTest`.

## Portable startup-archive assumption

**Context.** AppCDS, OpenJDK AOT, and OpenJ9 SCC artifacts depend on the JVM
and training environment.

**Rejected approach.** Train any archive on the host and inject it into any
container image as a portable file.

**Why it failed.** JVM startup archives encode runtime, path, and training
environment assumptions and may also differ between file and directory
shapes.

**Adopted invariant.** Preserve archive type and file/directory shape. Support
host training and base-image training as distinct targets, and treat the
archive as environment-specific.

**Revisit trigger.** A JVM vendor defines a portable archive format with
verifiable compatibility metadata across the targeted environments.

**Current owners.** `JvmStartupOptimizerArchiveType`,
`JvmStartupOptimizerArchiveKind`, and `JvmStartupArchiveTraining`;
`JvmStartupOptimizerArchiveContractTest`,
`QuarkusApplicationStartupArchiveFunctionalTest`,
`QuarkusApplicationStartupArchiveHostTest`, and
`QuarkusApplicationStartupArchiveJibTest`.

## First-applied plugin ownership

**Context.** Legacy and standalone application plugins can coexist during
migration, and both could otherwise register test or Tooling API hooks.

**Rejected approach.** Let whichever asynchronous callback happens first own
each shared facility.

**Why it failed.** Callback timing makes ownership nondeterministic and can
register duplicate Tooling API builders or test instrumentation.

**Adopted invariant.** Supported coexistence requires legacy first. Legacy
owns existing test instrumentation and Tooling API models; standalone observes
that ownership and does not duplicate it.

**Revisit trigger.** A formal shared ownership service coordinates both
plugins without application-order ambiguity.

**Current owners.** `PluginInternal`, `TestOwnership`, and the legacy
`QuarkusPlugin`; coexistence scenarios in
`QuarkusApplicationPluginCoexistenceFunctionalTest` and
`ToolingModelRegistrationTest`.

## Colliding Kotlin helper

**Context.** Kotlin DSL convenience methods can have JVM signatures that
collide with Java/Groovy-visible methods or generated accessors.

**Rejected approach.** Add same-erasure Kotlin extension helpers beside the
Java DSL and rely on source-language overload selection.

**Why it failed.** JVM erasure makes the helpers collide in bytecode even
though Kotlin source overload resolution appears unambiguous.

**Adopted invariant.** Kotlin helpers use a distinct JVM name or are omitted
when the Java action method is already idiomatic. Java, Groovy, and Kotlin
fixtures compile against the same public model.

**Revisit trigger.** The DSL moves to a Kotlin-first binary surface with an
explicit compatibility plan for Java and Groovy.

**Current owners.** `QuarkusApplicationJvmTestSuite`,
`QuarkusApplicationJvmTestSuiteDsl.kt`, and
`QuarkusApplicationTestRegistrationTest`; Kotlin fixture coverage in
`QuarkusApplicationPublicDslBoundaryTest`.

## Ordered mega-integration fixture

**Context.** Packaging, run, dev, remote, native, and startup tests are
expensive and appear to offer reusable application builds.

**Rejected approach.** Put all journeys in one ordered JUnit class that shares
a mutable Gradle build, server, or Quarkus process.

**Why it failed.** Ordered shared state couples otherwise independent failures,
prevents selective execution, and makes process and filesystem cleanup
dependent on earlier tests.

**Adopted invariant.** Each scenario owns live state and cleanup. Share only
immutable sources or harness-owned build preparation, and keep independently
selectable failures. Optimize duplicate invocations within those boundaries.

**Revisit trigger.** The integration harness provides a declared immutable
artifact fixture with isolated per-test runtime state and independent
selection.

**Current owners.** `QuarkusApplicationGradleTestBase`,
`ContinuousBuildTestSupport`, and the application integration-test classes
under `integration-tests/gradle`; `QuarkusApplicationRunTest`,
`QuarkusApplicationRemoteDevTest`, and
`QuarkusApplicationDevContinuousBuildTest`,
`QuarkusApplicationContinuousTestingFailureRecoveryTest`, and
`QuarkusApplicationDevUiContinuousBuildTest`.

## Platform and environment qualifications

Some evidence is conditional on Gradle version, operating system, Java
implementation, native-image toolchain, or container runtime. A platform-gated
test supports only the environment it actually exercised. The adopted
invariants still require deterministic cleanup and explicit failure when an
available environment violates them.

## Source and test ownership

The implementation owners and tests named in each decision are the durable
evidence. Topic details live in
[application plugin architecture](application-plugin-architecture.md),
[application model and variants](application-model-and-variants.md),
[development and remote lifecycle](development-and-remote-lifecycle.md), and
[testing](testing.md).

This page is reviewed when one of those owners changes an adopted invariant.
