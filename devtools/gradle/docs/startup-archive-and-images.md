# Startup archives and images

## Audience and scope

This page is for contributors changing JVM startup optimizer archives,
training through integration tests, container-provider handoff, or
startup-optimized images. It covers the shared Quarkus types and provider
boundaries consumed by the standalone Gradle plugin.

Archive type, filesystem shape, and provider request/result objects are typed
contracts. Concrete temporary Dockerfile context, process command, and Gradle
receipt locations are implementation details.

## Archive types and shapes

Quarkus supports three JVM startup optimizer archive types:

| Type | JVM | Shape | Default name |
| --- | --- | --- | --- |
| AppCDS | OpenJDK | file | `app-cds.jsa` |
| AOT cache | OpenJDK | file | `app.aot` |
| SCC | OpenJ9/Semeru | directory | `app-scc` |

The historical AOT-JAR package name describes the package layout; it does not
imply that the selected archive must be an OpenJDK AOT cache.
`JvmStartupOptimizerArchiveType` owns the JVM option and default name, while
`JvmStartupOptimizerArchiveKind` prevents code from treating a directory SCC
as a file.

The build items that request and return an archive carry both type and path.
Container image requests additionally carry the original image and working
directory. Compatibility accessors that use historical AOT terminology return
the same typed archive but must not erase SCC/AppCDS semantics.

## Sources of an archive

One named AOT-JAR build may obtain its archive from exactly one source:

- package-time generation through `fromPackageBuild()`;
- a user-supplied file or directory matching the selected type; or
- integration-test training.

The DSL keeps file and directory properties separate so Gradle tracks the
correct shape without inspecting an untyped path. Selecting
`fromPackageBuild()` is deferred through the DSL lifecycle and becomes
mutually exclusive with supplied locations and test training.

The package operation receives forced archive type/phase properties only for
that named build. There is no global manifest or startup-archive map shared
between builds.

## Integration-test training

A configured JVM integration-test suite can train the selected named build:

- `HOST_JVM` launches the package on the host JVM and writes to a host
  destination;
- `BASE_IMAGE` launches through a container image and maps an absolute,
  normalized container directory to the host destination.

The integration-test metadata includes archive type, absolute destination,
execution target, and optional container directory. Values are all-or-nothing
and validated before launch. OpenJDK AOT training uses an intermediate
configuration and final cache; SCC training prepares a directory. AppCDS is
not supported by the integration-test training contract.

Preparation removes stale output, creates the correct shape, and makes
container-mounted paths accessible when necessary. Validation requires a
non-empty file or non-empty directory. Cleanup remains owned by the suite even
when the test fails.

## Startup-optimized image flow

```text
named AOT-JAR package
       + selected startup archive
       + normal built-image receipt
                 |
                 v
startup-optimized image request
                 |
                 v
Docker/Podman/Jib provider
                 |
                 v
typed optimized-image result
```

Image-reference preflight claims the normal and optimized references before
side effects. The optimized task consumes the actual provider-observed normal
image reference, not merely a configured guess. Image build and push remain
separate operations and produce separate receipts.

Docker and Podman providers generate a bounded temporary build context and
Dockerfile plan. Jib adds the archive as a typed layer and adjusts JVM options
without invoking an external Dockerfile build. Provider code must render the
type-specific runtime option and place the archive where the target JVM can
read it.

## Process ownership

Host and container training may launch helper JVMs or container processes.
`BoundedProcessRunner` drains output, applies a timeout, destroys a process
that exceeds the bound, and reports exit status plus captured diagnostics.
Because the complete output is retained in memory, callers should use it only
for helpers with reasonably bounded output. Callers own temporary inputs and
archive cleanup; the process runner owns only the process it launches.

Provider plans that implement `AutoCloseable` own their temporary directory.
Close must remain safe after partial setup and on provider failure.

## Portability

Startup archives are generally tied to:

- JVM implementation and version;
- application bytecode and dependency set;
- JVM options used during training;
- target architecture and sometimes operating system; and
- container base image when trained there.

An archive produced on the host must not be assumed valid in an unrelated
base image. `BASE_IMAGE` training exists to align the training and runtime JVM
environment. A startup-optimized container image is the portable delivery
unit only to the extent guaranteed by its image platform.

See [tasks, outputs, and receipts](task-output-and-receipt-model.md) for named
task ownership and [testing](testing.md) for environment-gated evidence.

## Compatibility qualification

The typed archive enums/build items and provider request/result handoff are
shared code contracts. Specific Dockerfile text, layer ordering where not
semantically required, temporary paths, and receipt layout are current
implementation details.

## Platform and environment qualifications

OpenJDK AOT requires a supporting OpenJDK release; SCC requires OpenJ9/Semeru;
AppCDS support depends on the selected JDK. Base-image training requires an
available container runtime and compatible image. Native-image is a separate
packaging path and does not consume these JVM startup archives.

## Source and test ownership

Primary source owners:

- startup archive build items and steps in `core/deployment`
- `io.quarkus.test.common.JvmStartupArchiveTraining`
- jar/container launcher providers in `test-framework`
- Docker/Podman/Jib container-image deployment modules
- standalone startup archive DSL, tasks, and worker requests in
  `gradle-app-plugin`

Primary test owners:

- `JvmStartupOptimizerArchiveContractTest`
- `JvmStartupOptimizerArchiveBuildStepTest`
- `JvmStartupArchiveTrainingTest`
- `StartupArchiveDockerfileTest` and `StartupArchiveLayerPlanTest`
- `QuarkusApplicationStartupArchiveFunctionalTest`
- host and Jib startup-archive journeys in `integration-tests/gradle`
