---
name: working-with-config
description: >
  Quarkus configuration conventions: @ConfigMapping interfaces, config phases,
  and migration from legacy @ConfigRoot classes.
---

# Working with Configuration

## Use @ConfigMapping Interfaces (Not Legacy Classes)

Quarkus configuration **must** use `@ConfigMapping` interfaces. The legacy
`@ConfigRoot`-annotated classes with `@ConfigItem` have been fully removed
(see `adr/0008-phasing-out-config-classes.adoc`).

### Correct (current approach)

```java
@ConfigMapping(prefix = "quarkus.my-extension")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface MyExtensionRuntimeConfig {

    /** Description of the property. */
    @WithDefault("default-value")
    String myProperty();

    /** Optional property (absent means not set by the user). */
    Optional<String> optionalProperty();
}
```

### Do NOT Use Optional to Define Default Values

Never use `Optional` and provide a default via `.orElse(...)` in application
code. The default must be declared in the config mapping itself via
`@WithDefault`:

```java
// BAD — do not do this
String name = config.optName().orElse("theDefaultName");

// GOOD — declare the default in the config mapping
@WithDefault("theDefaultName")
String name();
```

### Non-root @ConfigMapping (without @ConfigRoot)

`@ConfigMapping` can also be used **without** `@ConfigRoot` for non-root
mappings. These are not auto-discovered — you must register them explicitly
(e.g., via `@Inject` in a CDI bean or programmatically with SmallRye Config).
Use this when mapping a sub-tree of configuration that does not represent an
extension's top-level config root.

```java
@ConfigMapping(prefix = "some.custom.prefix")
public interface MyCustomConfig {

    /** A non-root config property. */
    String someValue();
}
```

### Wrong (legacy approach, do not use)

```java
// DO NOT use this pattern
@ConfigRoot(name = "my-extension", phase = ConfigPhase.RUN_TIME)
public class MyExtensionConfig {
    @ConfigItem(defaultValue = "default-value")
    String myProperty;
}
```

## Config Phases

**`@ConfigRoot` defaults to `BUILD_TIME` when no phase is specified.** Always set
the phase explicitly — omitting it for a config injected at runtime will cause
`UnsatisfiedResolutionException` because `BUILD_TIME` configs are not registered
as CDI beans.

- **`BUILD_TIME`** — Baked into the binary at build time, immutable at runtime.
  Use for settings that affect code generation. Only available in `@BuildStep`
  methods — **not injectable via `@Inject` in runtime CDI beans**.
- **`BUILD_AND_RUN_TIME_FIXED`** — Read at build time, also available at
  runtime. Cannot change without rebuilding. Available in both `@BuildStep`
  methods and runtime CDI beans.
- **`RUN_TIME`** — Read at application startup. Can change between runs.
  Available in runtime CDI beans via `@Inject`. **Not** available in
  `@BuildStep` methods.

## Naming

- Config interfaces: `<Feature>Config.java`, or split as
  `<Feature>BuildTimeConfig.java` / `<Feature>RuntimeConfig.java`
- Config prefix: `quarkus.<extension-name>`

## Annotation Processor (`quarkus-extension-processor`)

The `quarkus-extension-processor` annotation processor is **required for all
extension modules**, not just those with `@ConfigRoot`. It generates metadata
files (including `META-INF/quarkus-config-roots.list` for config discovery)
that Quarkus needs to wire extensions correctly. Without it, most extension
functionality will silently fail.

Both `runtime/` and `deployment/` POMs must include:

```xml
<plugin>
    <artifactId>maven-compiler-plugin</artifactId>
    <executions>
        <execution>
            <id>default-compile</id>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>io.quarkus</groupId>
                        <artifactId>quarkus-extension-processor</artifactId>
                        <version>${project.version}</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Most modules already have this. If you create a new module or notice missing
metadata, verify the annotation processor is present in its `pom.xml`.

## Key Rules

- Always specify `phase` explicitly on `@ConfigRoot` — the default is
  `BUILD_TIME`, which is not injectable via CDI at runtime
- Configuration is immutable — never mutate config objects
- Use `RUN_TIME` phase unless the config genuinely affects build-time behavior
- Do NOT access `RUN_TIME` config during build steps — it is not available yet
- `BUILD_TIME` config changes require a rebuild
- When adding new `quarkus.*` properties, always register them in a
  `@ConfigMapping` interface. Reading them programmatically via
  `ConfigProvider.getConfig().getOptionalValue(...)` is fine, but the
  corresponding key must still be defined in a `@ConfigMapping` — otherwise
  SmallRye Config will emit "unknown property" warnings at startup.
