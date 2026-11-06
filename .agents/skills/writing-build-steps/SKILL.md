---
name: writing-build-steps
description: >
  Patterns for creating and modifying Quarkus @BuildStep methods, build items,
  and recorders in extension deployment modules.
---

# Writing Build Steps

Quarkus extensions perform work at build time through build steps that produce
and consume build items. This is the core extension mechanism.

## Build Steps

A build step is a method annotated with `@BuildStep` in a processor class
(by convention `<Feature>Processor.java` in the deployment module).

```java
@BuildStep
SomeBuildItem produce(AnotherBuildItem input) {
    // Build-time logic
    return new SomeBuildItem(...);
}
```

## Build Items

Build items extend `BuildItem` (or a subclass) and are the data objects passed
between build steps.

- **`SimpleBuildItem`** — Exactly one instance. Use for single values.
- **`MultiBuildItem`** — Multiple instances. Use when collecting contributions
  from multiple extensions (e.g., registering beans, routes).
- Build items should be `final` classes.

## Key Rules

- Build steps are ordered by input/output dependencies, NOT declaration order
- If a step consumes a `BuildItem`, it runs after all producers of that item
- **Prefer producing a single build item per step.** A step that produces
  multiple different item types is a sign it should be split. Use
  `BuildProducer<T>` when you need to produce multiple instances of the
  *same* item type or produce items conditionally
- Use `Optional<T>` for optional consumption of build items
- Use `List<T>` to consume all instances of a `MultiBuildItem`
- Build steps run during augmentation (build time), NOT at runtime
- Avoid unnecessary I/O or computation — leverage existing build items rather
  than duplicating work

## Config Mappings in Build Steps

`@ConfigMapping`/`@ConfigRoot` interfaces are only registered as CDI beans when
they appear as a parameter in at least one `@BuildStep`. If a runtime bean
injects a config mapping, you **must** reference that config mapping in a build
step — otherwise the injection will fail with `UnsatisfiedResolutionException`.
See the `working-with-config` skill for details.

## Services

To execute code at runtime from a build step, register a service using
`ServiceRegistrar`. Services are lightweight startup tasks that form a
dependency graph. The service lambda is defined in the deployment build step
but executes at runtime.

```java
@BuildStep
void setupAtRuntime(ServiceRegistrar reg, SomeBuildItem item) {
    reg.forService("io.quarkus.my-feature.setup")
       .onStart(ctx -> {
           // This lambda runs at runtime
           MyFeature.initialize(item.getValue());
       });
}
```

### Service Types

- **Typed services** produce a value: `reg.forService(MyService.class)`
- **Void services** produce no value, identified by name: `reg.forService("io.quarkus.feature.setup")`

Use typed services when other services need to depend on the value produced.
Use void services for side effects like initialization tasks.

### Phases

Services execute in ordered phases. The default is `Phase.APPLICATION`. Use
`.atPhase(Phase)` to customize:

- **`STATIC_INIT`** — Runs during static initialization (before `main()`).
  Results are baked into the native image heap when building a native image.
- **`CONFIG`** — Registers configuration property classes.
- **`LOGGING`** — Reconfigures the log manager.
- **`INFRASTRUCTURE`** — Creates foundational infrastructure (executors, event loops).
- **`BINDING`** — Binds server sockets.
- **`DATA`** — Establishes database connections.
- **`INIT`** — Performs one-time initialization (migrations, validation).
- **`APPLICATION`** — Registers request-handling logic (default).
- **`SERVING`** — Starts accepting requests.

### Dependencies

Declare all dependencies explicitly on the service itself:

- `.require(SomeService.class)` — depends on and consumes a service value
- `.request(SomeService.class)` — optional dependency, injected as `Optional<T>`
- `.after(SomeService.class)` — ordering-only dependency
- `.before(SomeService.class)` — this service must start before the specified service

Build step graph dependencies have zero influence on service execution order.

### Lambda Capture Constraints

Service lambda bodies run at runtime and can only reference runtime classes:

- **Supported captures**: primitives, `String`, `Class`, enum values, immutable
  collections, `BUILD_AND_RUN_TIME_FIXED` config, `Constable` values, records
- **Unsupported**: mutable collections, `BUILD_TIME` config, `RUN_TIME` config
  (use `.require()` instead), arbitrary runtime objects (use `.require()`)
- **Never reference deployment classes** in the lambda body — this causes
  `ClassNotFoundException` at runtime
