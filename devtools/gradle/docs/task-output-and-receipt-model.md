# Tasks, outputs, and receipts

## Audience and scope

This page is for contributors changing registered tasks, named-build
derivation, output locations, outgoing package variants, or the files used to
hand results between tasks. It documents current task graph and data contracts,
not end-user DSL recipes.

Plugin-registered task names, documented options, and user-configurable
properties are supported entry points. Direct construction, separate
registration, and subclassing of task classes are not supported extension
points merely because Gradle requires those classes to be public.

## Fixed task families

The standalone plugin registers application-wide tasks for:

- application, development, test, continuous-test, and code-generation models;
- normal, development, and test code generation;
- model/effective-config diagnostics;
- offline dependency preparation;
- `quarkusApplicationDev`;
- `quarkusApplicationContinuousTest`;
- `quarkusApplicationRemoteDev`; and
- internal failure, replay, reconnect, metadata, and preflight work.

Generated helper and preflight task names are implementation wiring unless
their type or task help explicitly documents user configuration.

## Named-build task graph

A build named `main` derives a `quarkusMain` prefix. Depending on its type and
configured features, the graph can include:

```text
quarkusMainBuild
quarkusMainShowEffectiveConfig
quarkusMainRun
quarkusMainImageBuild
quarkusMainImagePush
quarkusMainNativeTest
quarkusMainStartupArchiveValidation
quarkusMainStartupOptimizedImageBuild
quarkusMainStartupOptimizedImagePush
quarkusMainDeployTo<DeploymentName>
```

The exact set depends on package type. Run tasks require a JAR layout;
native-test tasks require a native executable; startup-archive and
startup-optimized image work requires an AOT-JAR build and its corresponding
DSL configuration.

Build and deployment names are normalized into Gradle task-name segments.
Case-insensitive collisions, collisions with existing tasks, and collisions
with legacy application task names fail during configuration with an
actionable message.

Named builds do not automatically join `assemble`. The
`participatesInAssemble` property is an opt-in per build, and the provider of
the selected package task is attached lazily to `assemble`.

## Package and launcher outputs

Every build owns a configurable output-directory property whose convention is
`build/quarkus-builds/<build-name>/package`. Consumers use its provider rather
than reconstructing the conventional path. A package result identifies the
build, package type, output root, primary artifact, launch shape, and
mutability needed by downstream plugin tasks.

JAR package builds publish:

- a complete directory variant for consumers that need every package file;
  and
- a primary launcher-JAR variant for producer-local execution.

The second contract is not portable for layouts whose launcher needs sibling
files. See [application model and variants](application-model-and-variants.md)
for the attributes and component identity.

Native builds expose the executable through task inputs and integration-test
metadata rather than pretending it is a JAR package variant.

## Receipts

Receipts make execution boundaries explicit and keep Gradle task dependencies
file-based. Important families include:

- augmentation and package results;
- image reference resolution and built-image results;
- deployment results;
- integration-test launch metadata;
- startup-archive training and validation metadata;
- development/remote-development iteration outcomes; and
- orderly session-close markers.

A receipt is owned by its producing task or long-lived session and consumed
only after the producer's declared dependency succeeds. Codecs validate
required fields and shape; consumers must not infer an output from a
directory scan when a typed result exists.

Receipts under `build/` are implementation data, not a user publication
format. They may contain absolute paths and are not portable between machines
or build directories.

## Cacheability and side effects

Model, code-generation, package, and finite metadata tasks declare inputs and
outputs appropriate to their behavior. A task is cacheable only when its
result is reproducible independently of process/session state and undeclared
external services.

Run, dev, continuous-test, remote-dev, image push, and deployment operations
are side-effecting or long-lived and therefore always execute. Image
reference preflight resolves and claims the final reference before a provider
side effect begins. Startup-optimized image tasks consume both normal-image
and startup-archive results.

Configuration-cache compatibility does not imply build-cache eligibility.

## Integration and publication ownership

Gradle JVM test suites consume package, native executable, image, and
startup-training metadata through task providers. The suite registration
selects one build source and rejects incompatible combinations before
execution.

The plugin publishes a directory rather than choosing a universal archive.
Users or publishing plugins own archive task type, compression, classifier,
and Maven publication attachment. This preserves the complete-package
contract without making ZIP a Quarkus format.

## Platform and environment qualifications

Image, deployment, native, and startup tasks require their provider/toolchain.
Receipt paths use the producer operating system's representation. Test code
must compare normalized paths rather than assume Unix separators or a
case-sensitive filesystem.

## Source and test ownership

Primary source owners:

- `io.quarkus.gradle.application.internal.plugin.NamedBuildTaskRegistration`
- `io.quarkus.gradle.application.internal.planning.TaskNamePlanner`
- `io.quarkus.gradle.application.tasks`
- `io.quarkus.gradle.application.internal.packaging`
- result codecs under `internal.image`, `internal.deployment`, and
  `internal.execution`

Primary test owners:

- named-build registration and package-variant tests in
  `gradle-app-plugin/src/test`
- package, image, deployment, and receipt task unit tests
- `QuarkusApplicationCompositePackagingTest`
- selected packaging and native journeys in `integration-tests/gradle`
