---
name: converting-recorders-to-services
description: >
  Step-by-step guide for converting Quarkus extensions from the legacy
  @Record/@Recorder pattern to the ActionBuilder service system.
---

# Converting Recorders to Services

## Architecture

The `ActionBuilder` service system replaces `@Recorder` with typed service
actions. Services declare dependencies explicitly and execute in dependency
order, not build step order. The system is designed for incremental
migration — converted and unconverted extensions coexist.

### Key principles

1. **Explicit dependencies only.** Services get NO implicit ordering from
   the build step graph. All ordering comes from `require()`, `after()`,
   `before()`, or `afterBuildItem()`.
2. **Services are independent from recorders in the same step.** Even if
   a build step method contains both a `@Record` call and an `ActionBuilder`
   call, the service and recorder are separate nodes with no implicit
   ordering between them.
3. **Values flow through the dependency graph.** Service values are passed
   from producer to consumer via indexed dependency access on `ServiceNode`,
   not through the `StartupContext` maps (except for recorder proxy bridges).

### Node types in the service graph

| Kind | Description | Gets step-graph deps? |
|------|-------------|-----------------------|
| `LEGACY_RECORDER` | Recorder chunk from a `@Record` step | Yes |
| `ALIAS` | Bridges a recorder value to a service key | Yes |
| `SERVICE` | ActionBuilder service with a lambda body | No |
| `RV_WRAPPER` | Wraps a service value in `RuntimeValue` for recorders | No |
| `CROSS_PHASE_PROXY` | Bridges a static-init service to the runtime graph | No |

Legacy recorders and aliases use step-graph ordering for compatibility.
Service nodes use only explicit declarations.

## Prerequisites

Before converting, verify that all runtime dependencies are available as
services:

| Dependency Type | Availability |
|-----------------|-------------|
| `ArcContainer` | Service in `ArcProcessor#initializeContainer` |
| `BeanContainer` | Service in `ArcProcessor#createBeanContainer` |
| `ScheduledExecutorService` | Aliased by `ThreadPoolSetup` |
| `@ConfigMapping` (`RUN_TIME`) | Auto-registered by `ConfigServiceRegistrationStep` |
| `@ConfigMapping` (`BUILD_AND_RUN_TIME_FIXED`) | Auto-registered as static-init |
| `@ConfigMapping` (`BUILD_TIME`) | **Cannot be a service** — extract values into locals |
| Other runtime objects | Search for `aliasRecorderValue` or `forService` |

## Conversion steps

### 1. Analyze the recorder

For each `@Recorder` method, determine:

- **Return type** — becomes the service type. `RuntimeValue<X>` unwraps
  to `X`. `void` becomes `Void.class` with a descriptive name.
- **Parameters** — `RuntimeValue<X>` params become `require(X.class)`.
  Simple values (String, int, boolean, enum) can be captured. Immutable
  collections (`List.of()`, `Set.of()`, etc.) can be captured.
- **Instance fields** — config `RuntimeValue`s injected via constructor
  become `require()` dependencies.
- **Trivial methods** — inline directly into the lambda.

### 2. Convert the recorder class

- **Trivial methods** — delete; inline into the lambda.
- **Complex methods** — remove `@Recorder`, make methods `static`,
  unwrap `RuntimeValue` parameters and return types.
- If all methods are converted, delete the class. Otherwise keep
  `@Recorder` for remaining methods.

### 3. Convert the build step

**Before:**
```java
@BuildStep
@Record(ExecutionTime.RUNTIME_INIT)
ServiceStartBuildItem setup(MyRecorder recorder, SomeBuildItem item) {
    recorder.initialize(item.getValue());
    return new ServiceStartBuildItem("my-feature");
}
```

**After:**
```java
@BuildStep
ServiceStartBuildItem setup(ActionBuilder action, SomeBuildItem item) {
    action
        .forService("io.quarkus.my-feature.setup")
        .action(ctx -> MyRecorder.initialize());
    return new ServiceStartBuildItem("my-feature");
}
```

Key changes:
- Remove `@Record(ExecutionTime.*)` annotation.
- Replace recorder parameter with `ActionBuilder action`.
- Use `.atPhase(Phase.STATIC_INIT)` for static-init services (default
  is runtime-init / `Phase.APPLICATION`).
- For void services, use hierarchical dot-separated names:
  `"io.quarkus.<extension>.<purpose>"`.

### 4. Declare dependencies

**Every dependency must be explicit.** There is no implicit ordering
from the build step graph.

| Old Pattern | New Pattern |
|------------|-------------|
| `recorder.method(beanContainer.getValue())` | `.require(BeanContainer.class)` |
| `recorder.method(someRuntimeValue)` | `.require(SomeType.class)` |
| `recorder.method(config.maxSize())` | Capture `int maxSize = config.maxSize()` before lambda |
| `@Consume(SyntheticBeansRuntimeInitBuildItem.class)` on step | `.afterBuildItem(SyntheticBeansRuntimeInitBuildItem.class)` on service |

#### `afterBuildItem()` — bridging to build step ordering

When a service depends on state produced by a legacy recorder (e.g.,
synthetic beans must be initialized), declare the dependency via
`afterBuildItem()`:

```java
action
    .forService("io.quarkus.arc.lifecycle")
    .afterBuildItem(SyntheticBeansRuntimeInitBuildItem.class)
    .action(ctx -> ArcRecorder.fireLifecycleEvent(new StartupEvent()));
```

This resolves the producing step's nodes (with passthrough resolution
for steps that have no service graph nodes) and creates ordering edges.

**`afterBuildItem()` is deprecated** — it exists only for recorder
coexistence. Once the producing recorder is converted to a service,
replace with `require()` or `after()`.

#### Cross-phase dependencies

Runtime services can `require()` static-init services. The framework
automatically creates a `CROSS_PHASE_PROXY` node that reads the value
from the `serviceValues` map (populated during static-init and retained
via `retainServiceValues()`).

```java
// static-init service
action.forService(ArcContainer.class)
    .atPhase(Phase.STATIC_INIT)
    .afterBuildItem(ResourcesGeneratedPhaseBuildItem.class)
    .action(ctx -> Arc.initialize());

// runtime service that depends on it
action.forService("io.quarkus.my-ext.setup")
    .require(ArcContainer.class)  // cross-phase: resolved via proxy
    .action((ctx, container) -> { ... });
```

#### Config mappings

- `BUILD_TIME` — extract values into locals before the lambda.
- `BUILD_AND_RUN_TIME_FIXED` — use `.require(ConfigType.class)` (resolved
  directly from SmallRye Config, no graph edge).
- `RUN_TIME` — use `.require(ConfigType.class)` (same mechanism).

### 5. Stop ordering with `before()`

`before(X)` declares "X depends on me" — this service starts before X
and stops after X. Use it for cleanup services that must outlive their
dependents:

```java
action
    .forService("io.quarkus.vertx.netty-thread-local-cleanup")
    .atPhase(Phase.STATIC_INIT)
    .before(IOThreadDetector.class)
    .before(ArcContainer.class)
    .action(ctx -> {
        ctx.onStop(() -> InternalThreadLocalMap.remove());
    });
```

### 6. Bridge to legacy build items

Build items consumed by unconverted extensions need bridge proxies:

| Build Item Stores | Bridge Method |
|-------------------|---------------|
| `RuntimeValue<T>` | `action.staticInitServiceAsRuntimeValue(T.class)` or `action.serviceAsRuntimeValue(T.class)` |
| Bare `T` (interface) | `action.staticInitServiceAsRecorderValue(T.class)` or `action.serviceAsRecorderValue(T.class)` |
| Bare `T` (concrete) | Use `RuntimeValue` variant — concrete classes may not be proxyable |

Recorder proxies resolve via `startupContext.getServiceValue()` (for
`__service$$value()` proxies) or `startupContext.getValue()` (for
standard recorder proxies). The service deploy body stores values in
the `serviceValues` map for this resolution.

### 7. Synthetic beans

**Before:**
```java
Supplier<MyBean> supplier = recorder.createBeanSupplier(args);
SyntheticBeanBuildItem.configure(MyBean.class).supplier(supplier).done();
```

**After:**
```java
action.forService(MyBean.class, "my-bean-name")
    .action(ctx -> new MyBean(args));

SyntheticBeanBuildItem.configure(MyBean.class)
    .serviceValue(MyBean.class, "my-bean-name")
    .done();
```

### 8. Shutdown handlers

Register cleanup via `ctx.onStop(Runnable)` or
`ctx.onStopAsync(Consumer<AsyncStopContext>)`. Stop handlers run in
reverse dependency order.

`addLastShutdownTask()` is **deprecated** — its per-node "last" semantics
don't provide global ordering. Use `before()` to ensure your service
stops after the services it cleans up for.

### 9. Runtime-optional services

A value-producing service action must return a non-`null` value — the
generated code throws `"...returned null; use a void service if no value is
produced"` otherwise. For a service whose *identity* is known at build time
but which may be **absent at runtime** (e.g. a handler gated by
`quarkus.log.file.enabled`), declare `optional()` before `action()`. This
flips the action's return type from `T` to `Optional<T>`; returning
`Optional.empty()` marks the service absent (nothing constructed, no side
effects) instead of failing.

```java
action.forService(Handler.class, "file")
    .atPhase(Phase.LOGGING)
    .require(LogRuntimeConfig.class)
    .optional()
    .action((ctx, config) -> config.file().enabled()
        ? Optional.of(buildFileHandler(config.file()))
        : Optional.empty());
```

Consuming an optional service:
- `consumeAll(T.class)` skips absent producers (the map omits them).
- `request(T.class)` yields `Optional.empty()` when absent.
- `require(T.class)` on an optional service is a **build error** — a
  mandatory dependency cannot depend on a maybe-absent value. Use
  `request()` or `consumeAll()` instead.

Do not return `null` from a normal (non-`optional`) action, and do not use a
sentinel value — `optional()` is the supported mechanism. (The `null`
tolerance in `consumeAll` applies only to aliased recorder values that
resolve to `null`, not to direct service actions.)

### 10. Lambda capture rules

Supported captures:
- Primitives, `String`, `Class`, enum values
- Immutable collections: `List.of()`, `Set.of()`, `Map.of()`,
  `Collections.singletonList/Set/Map()`, `Collections.empty*()`,
  `Collections.unmodifiable*()`, `Set.copyOf()`, `List.copyOf()`,
  `Map.copyOf()`
- `BUILD_AND_RUN_TIME_FIXED` config mappings (via `ConfigCaptureInterceptor`)
- `Constable` values — any type implementing `java.lang.constant.Constable`
  is captured via its `describeConstable()`. To make a custom value type
  capturable, implement `Constable` with a `describeConstable()` that
  reconstructs it (e.g. a `DynamicConstantDesc` invoking a static factory),
  rather than registering a recorder `ObjectSubstitution`. Immutable
  collections of `Constable` values (via `Map.copyOf`/`List.copyOf`) are
  themselves capturable.
- `record` values — captured by reconstructing via the canonical
  constructor, as long as every component is itself capturable (recursively).
  Note this is the **capture** path; it is unrelated to the *proxy* path — a
  `record` still cannot be used as an aliased recorder return type (see
  "Aliased recorder return types must be proxyable" below).

**Not capturable:**
- Mutable collections — use `Set.copyOf()` / `List.copyOf()` first
- `BUILD_TIME` config objects — extract values into locals
- `RUN_TIME` config objects — use `require()` instead
- Arbitrary runtime objects — use `require()` instead

## Common pitfalls

### No implicit ordering from build steps

Services get NO ordering from `@Consume`/`@Produce` annotations or
build item parameters on their build step method. Every ordering
dependency must be declared on the service itself.

If a service needs to run after a legacy recorder step, use
`afterBuildItem()` to depend on a build item that step produces.

### Concrete class proxying

`staticInitServiceAsRecorderValue(ConcreteClass.class)` fails if the
class has a non-trivial constructor. Use
`staticInitServiceAsRuntimeValue()` instead — `RuntimeValue` is always
proxyable.

### Aliased recorder return types must be proxyable

A value published via `aliasRecorderValue(T.class, recorderValue)` — or any
`@Recorder` method whose return value is used as a recorder proxy — must have
a **proxyable** type: an interface or a non-`final` class. A `record` is
`final`, so it fails with *"the return type cannot be proxied; use
RuntimeValue to wrap the return value instead"*. When bridging a bundle of
recorder values into a single service value, make the holder an **interface**
(with a nested `record` impl for the runtime value), or wrap it in
`RuntimeValue`. Abstract JDK types such as `java.util.logging.Formatter` and
`java.util.logging.Handler` are already proxyable.

### Package-private visibility

Lambda bytecode runs at runtime but references runtime classes directly.
Package-private classes become inaccessible from the generated
consolidated class.

Solutions:
1. Add a public static factory method on a public runtime class.
2. Move the class to a `.impl` subpackage and make it public.

### Use `require()` instead of `Arc.container()`

Access CDI beans through `require(BeanContainer.class)` and
`beanInstance()`. Direct `Arc.container()` calls bypass the dependency
graph and can cause ordering failures.

### Mutable collections must be copied

```java
Set<String> captured = Set.copyOf(endpoints);  // before the lambda
```

## Behavioral parity with recorders

Conversion must preserve behavior the recorder model provided *implicitly*.
These are recurring, high-cost regressions — check each when converting a
step that touches classloaders, threads, cleanup, or failure handling.

### TCCL parity (thread context classloader)

Recorder bytecode always ran with the **runtime classloader as the TCCL**
(the generated `doStart` sets it). Any recorder code that captured
`Thread.currentThread().getContextClassLoader()` for later use — request
handlers, thread factories, config/`ServiceLoader` lookups — relied on that.

- After converting, ensure the action still runs with the runtime CL as
  TCCL, and that captured/inherited CLs are the runtime CL (not the system
  `AppClassLoader`). A wrong TCCL surfaces as `ServiceLoader` "not a subtype"
  errors or dev-mode failures, not as an obvious classloader bug.
- **Threads created during startup inherit the creating thread's TCCL.**
  A long-lived or library thread that captures the per-app runtime CL and
  outlives the app **leaks the `QuarkusClassLoader`** (metaspace growth
  across test/dev restarts). Internal/scheduler threads that run no user
  code should not hold an app CL. Clean up threads/`ThreadLocal`s your
  service owns in `onStop`; never rely on a global "reset all TCCLs" sweep.

### Teardown-ordering parity

Start ordering is explicit (`require`/`after`/`before`) — but so is **stop
ordering**, and that is the easy thing to lose. The recorder model gave
cleanup incidental ordering from build-step order; a converted service's
`onStop` runs only in reverse *dependency* order. If a cleanup implicitly
depended on running before/after another (e.g. bean destruction before a
"last" task, or a listener firing before container teardown), declare it
with `before()`/`after()`. Lost teardown ordering surfaces as unrelated
downstream symptoms, so verify shutdown explicitly.

### Failure-path parity

Recorders logged **nothing in-band** on failure — `doStart` caught, wrapped
in `RuntimeException("Failed to start quarkus", cause)`, and rethrew. The
service graph rethrows the first failure (concurrent ones attach as
suppressed), so nothing is lost by staying quiet. **Do not add ERROR/WARN
logging in action bodies for expected failures** — let them propagate. Some
tests assert clean propagation (no WARNING+ log records, no suppressed
exceptions) for a static-init failure, e.g. `StaticInitFailureTest`.

### `consumeAll` uses indexed access

`consumeAll` dependencies expand to multiple consecutive entries in the
ServiceNode's dependency array (one per matching service, sorted by
name). Both `ServiceGraphBuilder` and `LambdaTransliterator` compute
the same match list using the same prefix and sort order, ensuring
index alignment.

## Checklist

- [ ] Identify all `@Record` build steps in the processor
- [ ] For each, determine if all dependencies are available as services
- [ ] Convert recorder methods (inline trivial ones, make complex ones static)
- [ ] Update build steps: remove `@Record`, add `ActionBuilder`, register services
- [ ] Declare ALL dependencies explicitly (`require`, `after`, `before`, `afterBuildItem`)
- [ ] Do NOT rely on build step ordering for service execution order
- [ ] Bridge values to legacy build items where needed
- [ ] Convert synthetic beans from `supplier()`/`runtimeValue()` to `serviceValue()`
- [ ] Check for package-private visibility issues
- [ ] Run `mcp__jetbrains__get_file_problems` on all modified files
- [ ] `./mvnw install` the modified modules before running integration tests
