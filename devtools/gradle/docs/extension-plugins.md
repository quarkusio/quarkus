# Extension plugins

## Audience and scope

This page is for contributors changing Gradle support for Quarkus extension
runtime and deployment modules. It documents the responsibilities of
`io.quarkus.extension` and `io.quarkus.extension.deployment`, their published
variants, and their relationship to application-model resolution. It does not
duplicate the user-facing migration recipe; see
[Migrate a Gradle-built extension to Quarkus 4](../../../docs/src/main/asciidoc/gradle-extension-plugin-migration.adoc)
for the required build-script changes.

Plugin IDs, the runtime-module DSL, and consumed outgoing variants are
user/build contracts. Marker files, generated model paths, and configuration
implementation types are internal unless explicitly exposed.

## Runtime-module plugin

`io.quarkus.extension` is applied to the extension runtime module. It applies
the Java plugin and owns:

- the `quarkusExtension` configuration;
- extension descriptor generation;
- extension validation;
- Quarkus annotation-processor registration;
- runtime-module role attributes;
- local deployment-project dependency publication;
- conditional and conditional-development dependency publication; and
- deployment-marker resolution used to validate a local deployment module.

`extensionDescriptor` produces both descriptors under
`build/generated/resources/quarkus-extension/main`. Its entire output
directory belongs to that task. The Java `processResources` task consumes the
generated directory and is the only task that writes
`build/resources/main`; the raw source descriptors are inputs to generation,
not independent copy inputs. This prevents overlapping task outputs and keeps
repeated resource processing incrementally correct.

Normal `build` and `jar` invocations include the descriptors without extra
wiring. A direct `extensionDescriptor` invocation stops after generating its
declared output; consumers that require the conventional processed-resource
tree should depend on `processResources` or a normal Java lifecycle task.
Descriptor generation is ordered after `validateExtension` and before Java
compilation/resource processing. `validateExtension` checks
runtime/deployment structure. Tests use JUnit Platform.

The runtime module defaults its sibling deployment module to `deployment`.
An explicit `deploymentArtifact` selects external coordinates and disables
validation that requires a local deployment project. `disableValidation`
remains a managed Boolean property exposed through the existing
`setDisableValidation` and `isValidationDisabled` accessors.

The extension plugin does not own application native-image arguments and does
not translate a legacy `nativeArguments` value. Native application
configuration belongs to the application plugin/task that performs the build.

## Deployment-module plugin

`io.quarkus.extension.deployment` is applied to the extension deployment
module. It owns:

- strict test application-model generation;
- deployment-classpath configuration for test mode;
- annotation-processor registration;
- injection of the serialized test application model into every Gradle
  `Test` task; and
- the deployment-module marker variant.

`quarkusExtensionDeploymentMarker` produces a marker artifact. The consumable
`quarkusExtensionDeploymentMarkerElements` configuration carries the marker
category and Boolean deployment attribute. Application and runtime-module
resolution use variant reselection to identify the deployment component.

`SerializedTestApplicationModelArgumentProvider` declares the generated model
as an input and contributes the bootstrap system property lazily. Test tasks
depend on the model producer; configuration does not resolve the application
model eagerly.

## Variant handoff

The runtime plugin publishes a dependency on its deployment project through a
capability-bearing variant instead of a custom resolved deployment classpath
artifact. It also publishes conditional dependency coordinate sets as
dependencies on dedicated variants.

The deployment plugin publishes identity, not deployment classes. The
application model then resolves the correct runtime and deployment
configurations using shared `gradle-model` attributes:

```text
extension runtime module
  |-- runtime role
  |-- deployment project dependency
  |-- conditional dependency declarations
  `-- conditional dev dependency declarations

extension deployment module
  `-- deployment marker
```

This arrangement works across ordinary multi-project builds and composite
build substitution because ownership travels through Gradle component and
variant identity rather than project callbacks.

## Publication and project boundaries

Published Quarkus extension metadata remains the runtime module's
responsibility. The deployment project is not automatically published as an
application package. The marker and local dependency variants are primarily
build-graph contracts; Maven extension metadata remains the external
distribution contract.

The extension deployment plugin is independent from the standalone
application plugin. It can produce the strict test application model needed
by an extension module without applying either application plugin.

See [application model and variants](application-model-and-variants.md) for
how an application consumes the extension variants and
[tooling model](tooling-model.md) for composite-build output correlation.

## Compatibility qualification

DSL properties and documented plugin IDs are user-facing. Configuration names
and attributes consumed across these Quarkus plugins are internal shared
contracts that must evolve atomically. Public task/configuration helper types
needed for Gradle decoration do not imply supported direct construction or
subclassing.

## Platform and environment qualifications

The plugins require the generated minimum Gradle version and the Java plugin.
Composite-build coverage depends on Gradle component identity and variant
reselection. The serialized test model contains local paths and is valid for
the current build/test execution, not for publication.

## Source and test ownership

Primary source owners:

- `devtools/gradle/gradle-extension-plugin`
- `devtools/gradle/gradle-extension-deployment-plugin`
- `io.quarkus.gradle.model.config.ExtensionVariantConstants`
- shared strict model tasks in `devtools/gradle/gradle-model`

Primary test owners:

- `QuarkusExtensionPluginTest`
- `QuarkusExtensionDeploymentPluginTest`
- extension model and local-extension tests in `gradle-app-plugin`
- `QuarkusApplicationExtensionModelCompositeTest`
- `ExtensionUnitTestTest` in `integration-tests/gradle`
