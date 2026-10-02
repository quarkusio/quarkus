# Application plugin architecture

## Audience and scope

This page is for contributors changing plugin application, DSL ownership,
Gradle configuration, task registration, worker execution, or coexistence
with the legacy application plugin. It describes the current architecture of
`io.quarkus.application`; it does not document the full DSL syntax.

The public plugin and DSL are supported user-facing contracts. The internal
registrars, planners, codecs, and receipt locations are implementation
details unless another page identifies a consumed boundary.

## Module boundary

`gradle-app-plugin` contains the standalone application plugin. Applying it:

1. checks the minimum Gradle version;
2. applies the Java plugin;
3. creates the `quarkusApplication` extension;
4. creates provider-backed classpath and model inputs;
5. registers fixed task families and reacts lazily to named DSL objects; and
6. registers Tooling API models when the standalone plugin owns them.

The shared `gradle-model` module owns strict dependency and application-model
machinery used by the standalone and extension deployment plugins. It must not
depend on the standalone DSL. Framework execution remains in Quarkus core and
is invoked through worker-backed, serialized requests.

See [application model and variants](application-model-and-variants.md) for
model semantics and [tasks, outputs, and receipts](task-output-and-receipt-model.md)
for the registered graph.

## Configuration and execution data flow

```text
Gradle DSL and dependency declarations
        |
        v
provider-backed configurations and normalized task inputs
        |
        v
application-model/code-generation and named-build tasks
        |
        v
serialized worker request -- isolated worker classpath -- Quarkus operation
        |
        v
typed result receipt and declared output
```

Configuration code may register providers, artifact views, attributes, and
task dependencies. It must not resolve configurations eagerly or capture a
`Project`, task, configuration, or mutable DSL object in a task action or
serialized callback. Execution code receives declared Gradle properties and
plain immutable values.

Code generation has separate normal, development, and test model/task
families. The model registration facade owns ordering: model providers are
created before code-generation tasks consume the providers they need. The
model registrar itself has no dependency on code-generation registration.

Named-build registration validates all derived names and then registers
package, run, image, integration-test, startup-optimized image, and deployment
work in a deterministic order. DSL lifecycle callbacks defer optional work
without using `afterEvaluate`.

## Configuration cache and Isolated Projects

Configuration cache and Isolated Projects are architectural inputs, not
post-implementation optimizations:

- task actions operate on declared inputs and services rather than `Project`;
- task/configuration registration remains lazy;
- provider callbacks do not retain mutable coordinators;
- artifact views carry resolved file facts into normalized task inputs;
- cross-project extension discovery uses published variants and component
  identity instead of mutable project access; and
- worker requests contain serializable values, never live Gradle models.

Configuration-cache reuse is tested for representative task families.
Isolated-Projects tests use Gradle's feature flag because the feature remains
Gradle-version dependent.

## Worker and process boundary

Package, image, deployment, and code-generation operations use Gradle worker
isolation. The build-tool side selects inputs and output locations; Quarkus
core performs augmentation or provider work. The worker boundary prevents
Quarkus deployment dependencies from becoming part of the Gradle daemon's
long-lived plugin classloader.

Run, development, continuous testing, and remote development are deliberately
long-lived or foreground operations and are not cacheable. Their process and
session ownership is described in
[development and remote lifecycle](development-and-remote-lifecycle.md).

## Coexistence with the legacy plugin

Applying both application plugins is supported only as a migration shape when
legacy `io.quarkus` is applied first. The legacy plugin owns existing Gradle
`Test` instrumentation in that mode. The standalone plugin does not register
its Tooling API model builders when it observes the legacy owner, avoiding two
providers for the same model name.

Coexistence does not make legacy tasks configuration-cache or
Isolated-Projects compatible, and it does not merge both DSLs. New work must
not infer ownership from whichever plugin callback happens to run first.

## Compatibility qualification

The plugin ID, extension name, public DSL types, documented named task
properties/options, and consumed variants are user-facing. Public task classes
may also need public visibility for Gradle decoration; that visibility does
not promise support for direct construction, additional registration, or
subclassing. Internal receipt formats and task names used only to connect
plugin-owned tasks may change together.

## Platform and environment qualifications

The generated Gradle-version support class defines the actual supported
range. Java toolchains selected for application compilation are not
automatically the same JVM as the Gradle daemon or worker process. Container,
native-image, and startup-archive work requires its corresponding external
tool or JVM implementation.

## Source and test ownership

Primary source owners:

- `io.quarkus.gradle.application.QuarkusApplicationPlugin`
- `io.quarkus.gradle.application.internal.plugin.PluginInternal`
- `io.quarkus.gradle.application.internal.plugin.TaskRegistration`
- `io.quarkus.gradle.application.internal.plugin.DslLifecycleCoordinator`
- `io.quarkus.gradle.application.internal.execution.worker`

Primary test owners:

- `QuarkusApplicationPluginTest`
- `QuarkusApplicationPluginFunctionalTest`
- `QuarkusApplicationPluginDevOptionsFunctionalTest`
- `QuarkusApplicationPluginCoexistenceFunctionalTest`
- `QuarkusApplicationPluginVariantConsumptionFunctionalTest`
- `QuarkusApplicationWorkerIsolationFunctionalTest`
- the configuration-cache and Isolated-Projects cases throughout
  `gradle-app-plugin/src/test`
