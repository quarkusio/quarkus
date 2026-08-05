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

## Recorders

To execute code at runtime from a build step, use a `@Recorder`. The recorder
class lives in the **runtime** module. Its methods are called from deployment
build steps, but the actual execution happens at application startup — the
recorder generates bytecode that replays those method calls at runtime.

```java
@BuildStep
@Record(ExecutionTime.RUNTIME_INIT)
void setupAtRuntime(MyRecorder recorder, SomeBuildItem item) {
    recorder.initialize(item.getValue());
}
```

### Execution Times

- **`STATIC_INIT`** — Runs during static initialization (before `main()`).
  Safe for native image. Prefer this when possible as it allows more work to
  be done at build time in native mode.
- **`RUNTIME_INIT`** — Runs at application startup (during `main()`). Use when
  the code needs runtime-only resources (e.g., network, threads).

### Recorder Constraints

- Recorder methods must only use types available in the **runtime** module
- Do not pass deployment-only types to recorder methods
- Do not use build-time-only libraries inside recorder implementations
- Recorder method parameters must be recordable (the bytecode recorder writes
  code that reconstructs the object, similar to how `ObjectMapper` would
  serialize it — but to bytecode, not JSON). Use simple value types, collections,
  or types with appropriate constructors/setters. This is NOT Java serialization
