# Tooling model

## Audience and scope

This page is for contributors changing Tooling API model providers, the Gradle
application-model sidecar, model correlation/validation, or standalone versus
legacy provider routing. It owns sidecar schema and routing. The represented
application/workspace/output semantics belong to
[application model and variants](application-model-and-variants.md).

The sidecar is a serialized cross-classloader contract. Its validator protects
the paired model's schema, request identity, and canonical dependency graph.
The concrete Gradle collector and builders are implementation details.

## Paired model

The standalone plugin registers two parameterized Tooling API models:

- the Quarkus `ApplicationModel`; and
- `GradleApplicationModelSidecar`.

Both are built for the same requested launch mode and from the same resolution
views. The sidecar does not replace the application model. It supplies Gradle
identity and observation facts that the build-tool-neutral application model
cannot express.

The paired build action asks for both models and returns
`QuarkusToolingModelResult`. Its provider kind distinguishes:

- `STANDALONE_APPLICATION`, which requires a sidecar; and
- `UNMARKED_COMPATIBILITY`, used for legacy/unmarked providers and forbidden
  from returning a sidecar.

This explicit marker avoids routing based on model class name alone.

## Sidecar schema

The sidecar contains:

- schema version and requested mode;
- target Gradle build-tree path;
- canonical application-model graph facts;
- target and dependency project identities;
- component roles and classpath associations;
- direct/transitive/workspace relationships;
- logical class and processed-resource outputs;
- observed source directories; and
- model associations connecting an observed output to an application-model
  artifact.

Project identity uses Gradle build-tree paths so included builds cannot collide
with same-named projects in the main build. Logical-output identity includes
component, output kind, selected artifact identity, and normalized path.
Output paths are observations, not sufficient identity by themselves.

Unknown scope, materialization, producer category, and source role values
allow the producer to report an observation without inventing a stronger
semantic claim.

## Correlation and validation

`GradleModelCorrelationSupport` creates deterministic graph facts from the
application model. The validator checks:

1. current schema version;
2. requested mode;
3. target build-tree path;
4. canonical graph facts;
5. graph coordinates, flags, resolved paths, and workspace edges.

Validation fails closed with a dimension-specific mismatch rather than
silently combining models from different requests, projects, or classloader
proxies. Canonical collection order makes equivalent models validate
independently of Gradle iteration order.

The current validator does not independently validate component,
logical-output, source-observation, or model-association payloads. Consumers
must therefore treat those fields as collector observations rather than
revalidated application-model facts.

`UNKNOWN` scope, materialization, producer-category, and source-role values
are valid serialized observations. The validator neither rejects nor promotes
them. In particular, an association with unknown semantics is not eligible to
replace a more precise association during overlay.

## Provider and request routing

`QuarkusToolingModelBuildAction` is the transport-neutral Tooling API request.
`QuarkusGradleModelFactory.createPaired*` executes it and keeps the result
paired.

`BuildToolHelper` probes provider kind before choosing prerequisite tasks.
For the standalone plugin's test model it requests standard `classes` and
`testClasses`; legacy routing retains its integration-test source-set
requirements. Production and development helpers continue to request only the
tasks their consumer needs.

When both application plugins are present in supported migration order, the
standalone plugin leaves Tooling API model ownership to legacy. There must
never be two active providers for the same model type in one project.

## Collector boundary

`GradleApplicationModelSidecarCollector` may inspect Gradle resolution results
while a Tooling API model is being built. It copies those facts immediately
into serializable bootstrap model types. Nothing in the returned sidecar
retains `Project`, `Configuration`, `ResolvedComponentResult`, or another
Gradle implementation object.

The collector uses variant reselection for local class/resource/source outputs
and extension deployment markers. When it cannot establish one precise
output-to-model association, it publishes the observation with unknown
semantics instead of inventing a stronger relationship.

## Compatibility qualification

The schema version is an explicit serialization boundary. Consumers must
validate it and must not deserialize a newer schema as though missing fields
were harmless. This reference describes current Quarkus producer/consumer
behavior; it does not promise that the sidecar is a stable third-party API
across Quarkus versions.

## Platform and environment qualifications

Tooling API model actions execute against the target build's Gradle version
and plugin classpath. Serialized interfaces and implementation values must
survive Gradle's classloader/proxy boundary. Absolute output paths are local
observations and are not portable to another checkout.

## Source and test ownership

Primary source owners:

- `io.quarkus.bootstrap.model.gradle` and its `impl` package
- `io.quarkus.gradle.application.internal.tooling`
- `io.quarkus.bootstrap.resolver.QuarkusToolingModelBuildAction`
- `QuarkusToolingModelResult` and `QuarkusGradleModelFactory`
- `io.quarkus.bootstrap.utils.BuildToolHelper`

Primary test owners:

- `GradleApplicationModelSidecarTest`
- `QuarkusToolingModelBuildActionTest`
- `QuarkusApplicationToolingApiTest`
- `QuarkusApplicationToolingLocalExtensionTest`
- `QuarkusApplicationToolingFallbackTest`
