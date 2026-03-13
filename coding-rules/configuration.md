# Configuration

## Use @ConfigMapping Interfaces (Not Legacy Classes)

Quarkus configuration **must** use `@ConfigMapping` interfaces, not the legacy
`@ConfigRoot`-annotated classes. The legacy approach is being phased out
(see `adr/0008-phasing-out-config-classes.adoc`).

### Correct (current approach)

```java
@ConfigMapping(prefix = "quarkus.my-extension")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface MyExtensionRuntimeConfig {

    /** Description of the property. */
    @WithDefault("default-value")
    String myProperty();

    /** Optional property. */
    Optional<String> optionalProperty();
}
```

### Wrong (legacy approach, do not use)

The `@ConfigItem` annotation has been fully removed from the codebase. Any code using
it must be migrated to `@ConfigMapping` interfaces with `@WithDefault`.

```java
// DO NOT use this pattern for new code
@ConfigRoot(name = "my-extension", phase = ConfigPhase.RUN_TIME)
public class MyExtensionConfig {
    @ConfigItem(defaultValue = "default-value")
    String myProperty;
}
```

## Config Phases

- `BUILD_TIME` - Baked into the binary at build time, immutable at runtime.
  Use for settings that affect code generation.
- `BUILD_AND_RUN_TIME_FIXED` - Read at build time but also available at runtime.
  Cannot be changed at runtime without rebuilding.
- `RUN_TIME` - Read at application startup. Can be changed between runs.

## Key Rules

- Configuration is immutable — never mutate config objects
- Use `RUN_TIME` phase unless the config genuinely affects build-time behavior
- Do not access `RUN_TIME` config during build steps (augmentation) — it's not available yet
- `BUILD_TIME` config changes require a rebuild
