# Build Steps and Build Items

Quarkus extensions perform work at build time through build steps that produce and
consume build items. This is the core extension mechanism.

## Build Steps

A build step is a method annotated with `@BuildStep` in a class called a "processor"
(by convention, `*Processor.java` in the deployment module).

```java
@BuildStep
SomeBuildItem produce(AnotherBuildItem input) {
    // Build-time logic
    return new SomeBuildItem(...);
}
```

## Build Items

Build items are the data objects passed between build steps. They extend `BuildItem`
(or one of its subclasses).

### Types of Build Items

- **`SimpleBuildItem`** - Exactly one instance is produced. Use when there is a single
  value to communicate.
- **`MultiBuildItem`** - Multiple instances can be produced. Use when collecting
  contributions from multiple extensions (e.g., registering beans, routes, etc.).

## Key Rules

- Build steps are ordered by their input/output dependencies, NOT by declaration order
- If a build step consumes a `BuildItem`, it will run after all producers of that item
- Use `BuildProducer<T>` for producing multiple items in a single step
- Use `Optional<T>` for optional consumption of build items
- Use `List<T>` to consume all instances of a `MultiBuildItem`
- Build steps run during augmentation (build time), NOT at runtime
- Avoid unnecessary I/O or computation in build steps. Leverage existing build items
  and annotation processors where appropriate rather than duplicating work

## Recorders

To execute code at runtime from a build step, use a `@Recorder`:

```java
@BuildStep
@Record(ExecutionTime.RUNTIME_INIT)
void setupAtRuntime(MyRecorder recorder, SomeBuildItem item) {
    recorder.initialize(item.getValue());
}
```

- `STATIC_INIT` - Runs during static initialization (native image friendly)
- `RUNTIME_INIT` - Runs at runtime startup
- Prefer `STATIC_INIT` when possible for better native image support
