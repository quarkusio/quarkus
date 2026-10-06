# Application model and variants

## Audience and scope

This page is for contributors changing dependency resolution, normalized
application-model inputs, workspace outputs, code-generation inputs, POM
closure, or application/extension variants. It owns the semantics of the
application, workspace, and logical-output model. Sidecar schema, correlation,
and Tooling API routing belong to the [tooling model](tooling-model.md).

The serialized Quarkus `ApplicationModel`, consumed Gradle variant attributes,
and exact dependency distinctions are contracts between modules. The Gradle
configurations and artifact views used to produce them are implementation
details, but must retain lazy resolution and deterministic results.

## Strict normalized inputs

The standalone plugin builds separate resolution views for normal,
development, test, and continuous-test modes. Each mode distinguishes:

- original/runtime classpath;
- Quarkus deployment classpath;
- compile-only classpath;
- platform properties and platform constraints;
- local project class/resource outputs;
- main sources used by local-extension development; and
- deployment-module marker variants.

Model tasks consume Gradle properties, file collections, artifact records,
project descriptors, and serialized coordinates. They do not inspect a live
`Project` during task execution. Task input fingerprints retain the
configuration identity and resolved artifact data needed to make changes
observable to Gradle.

`StrictApplicationDeploymentClasspathBuilder` and
`StrictApplicationModelTaskConfigurator` are shared machinery. The standalone
plugin adds application-specific collection and assembly without weakening
those strict inputs.

## Dependency semantics

The model keeps these distinctions even when their files overlap:

- runtime versus deployment dependencies;
- direct versus transitive dependencies;
- compile-only dependencies;
- conditional and conditional-development dependencies;
- extension runtime versus extension deployment modules;
- same-build project components versus included-build substitutions; and
- main, test, and continuous-test launch modes.

Local project outputs are selected through component-aware artifact views.
Directory paths alone are not identities: a component may expose multiple
logical class or resource outputs, and two projects may use similarly named
output directories.

The dependency graph and output records are ordered deterministically before
serialization. Model consumers must not depend on Gradle iteration order.

## POM closure and offline preparation

Resolved artifacts do not by themselves describe the Maven metadata needed
for an offline build. The POM closure includes:

- direct module POMs;
- recursively referenced parent POMs;
- dependency-management BOMs;
- BOMs imported from another BOM or POM; and
- metadata needed to associate the effective model with resolved modules.

Gradle artifact views supply known POM artifacts without task-time repository
access. `MavenEffectiveModelResolver` and the POM task input recursively
calculate the remaining effective-model closure from declared, normalized
inputs. `GeneratePomClosureTask` only writes that already calculated result;
it does not obtain an injected `DependencyHandler` or resolve repositories in
its action.

`quarkusApplicationPrepareOffline` aggregates the base application and
code-generation inputs. Each named build may contribute its packaging or
provider-specific inputs. Resolving every configuration is not a substitute
for the effective POM closure.

## Workspace and logical outputs

The Quarkus application model may associate one workspace module with more
than one classes or resources output. The model therefore treats each output
as an independently identifiable logical occurrence rather than collapsing
all paths into one module directory.

Reloadability is selected per resolved dependency and output association.
Removing a module from the reloadable set does not remove it from the
dependency graph. Tests and tooling consumers must preserve application,
workspace dependency, extension runtime, and extension deployment roles.

The sidecar adds Gradle build-tree identity and correlation facts to this
model; it does not redefine the application model. See
[tooling model](tooling-model.md).

## Code-generation model ownership

Normal code generation uses a model that does not depend on already compiled
application classes. Development generation depends on normal generation.
Test generation has `mustRunAfter` constraints for normal and development
generation, which order those tasks only when they are already scheduled; the
constraints do not add task dependencies.

Generated Java, Kotlin, KAPT, IDE, and Jandex inputs are wired through
providers. IDE requests may execute the model/code-generation tasks needed to
materialize generated sources, but model construction remains identical to
command-line construction.

## Extension variants

The extension runtime plugin publishes component-aware variants for:

- the deployment project dependency;
- conditional runtime dependencies;
- conditional development dependencies; and
- the runtime-module role.

The extension deployment plugin publishes a marker variant. Application model
resolution reselects these variants to find local deployment modules without
reaching into another project. See
[extension plugins](extension-plugins.md) for producer ownership.

## Application package variants

Each named JAR build exposes two deliberately different outgoing contracts:

| Contract | Shape | Portability |
| --- | --- | --- |
| Package elements | Complete package output directory | Relocatable as a set |
| Launcher elements | Primary launcher JAR at its producer location | May require sibling package files |

Both variants carry a Quarkus category, library-elements value, build-name
attribute, and build-type attribute. The package artifact is a directory. The
launcher artifact is useful for producer-local execution, but must never be
described as a complete fast-JAR or mutable-JAR package.

Archive format is not imposed on package consumers. A project that needs ZIP,
ZIP64, TAR, or multiple publication formats can attach a user-defined archive
task to the package-directory variant. Task and receipt ownership is described
in [tasks, outputs, and receipts](task-output-and-receipt-model.md).

## Platform and environment qualifications

POM paths may refer to Gradle's dependency cache and are intentionally not
portable build outputs. Offline completeness depends on the repositories and
metadata visible during preparation. Composite-build identity relies on the
Gradle version's component model; tests must cover same-build and
included-build cases.

## Source and test ownership

Primary source owners:

- `devtools/gradle/gradle-model/src/main/java/io/quarkus/gradle/model`
- `io.quarkus.gradle.application.internal.modelgen`
- `ApplicationModelTaskRegistration` and
  `ApplicationCodegenTaskRegistration`
- `io.quarkus.bootstrap.model.ApplicationModelBuilder`
- `io.quarkus.bootstrap.workspace.DefaultWorkspaceModule`

Primary test owners:

- `ApplicationModelAndCodegenRegistrationTest`
- `QuarkusApplicationModelResolutionTest`
- `QuarkusApplicationLocalOutputResolutionTest`
- `QuarkusApplicationConditionalDependencyResolutionTest`
- `QuarkusApplicationPluginTest` for late platform-dependency mirroring
- `QuarkusApplicationToolingLocalExtensionTest`
- `QuarkusApplicationKotlinGeneratedSourcesTest`
- POM closure tests in `gradle-model`
- workspace-model tests in `independent-projects/bootstrap/app-model`
