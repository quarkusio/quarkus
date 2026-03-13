# Classloading

Quarkus has a split classloading model. This is one of the most common sources of
mistakes for new contributors and AI agents.

## The Two ClassLoaders

1. **Deployment ClassLoader** - Used at build time. Has access to all dependencies
   including deployment-only libraries. Code here runs during the build (augmentation).

2. **Runtime ClassLoader** - Used at application runtime. Only has access to runtime
   dependencies. This is the classloader the application runs in.

## Key Rules

- **Runtime code MUST NOT reference deployment classes.** The deployment modules are
  not available at runtime. If you import a class from a deployment module in runtime
  code, it will fail at runtime with `ClassNotFoundException`.

- **Deployment code CAN reference runtime classes.** Deployment modules depend on
  their corresponding runtime modules.

- **Recorders bridge the gap.** A `@Recorder` class lives in the runtime module but
  is invoked from deployment build steps. The recorder methods generate bytecode that
  runs at runtime.

- **Runtime config vs build-time config.** `@ConfigMapping` interfaces with `BUILD_TIME`
  phase are evaluated during the build. `RUN_TIME` phase config is available only at
  runtime. Mixing these up causes failures. See [configuration.md](configuration.md)
  for details.

## Runtime-Dev Modules and Conditional Dependencies

Extensions that provide dev-mode-only functionality (e.g., Dev UI pages) use a
`runtime-dev/` module. These classes are **only** loaded in dev mode, not in
production. This requires special wiring:

### 1. Register the runtime-dev module as a conditional dev dependency

In your **runtime** module's `pom.xml`, configure the `quarkus-extension-maven-plugin`
to declare the `runtime-dev` module as a conditional dev dependency:

```xml
<plugin>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-extension-maven-plugin</artifactId>
    <executions>
        <execution>
            <phase>process-resources</phase>
            <goals>
                <goal>extension-descriptor</goal>
            </goals>
            <configuration>
                <conditionalDevDependencies>
                    <artifact>${project.groupId}:${project.artifactId}-dev:${project.version}</artifact>
                </conditionalDevDependencies>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**Without this, the runtime-dev classes will not be loaded in dev mode** and you will
get `ClassNotFoundException` when trying to use them.

### 2. Mark dev-only dependencies as optional in runtime-dev

In your `runtime-dev/pom.xml`, mark dev-only dependencies as
`<optional>true</optional>` to prevent them from leaking into the production
dependency tree.

### 3. Guard build steps with IsLocalDevelopment

Deployment processors that produce Dev UI items must be guarded:

```java
@BuildSteps(onlyIf = IsLocalDevelopment.class)
class MyExtensionDevUIProcessor {
    // ...
}
```

### Why this matters

If you forget the `conditionalDevDependencies` configuration, the runtime-dev module
won't be on the classpath in dev mode. Everything compiles fine, but at runtime in dev
mode, any reference to runtime-dev classes (e.g., from a recorder or build step) will
fail with `ClassNotFoundException`. This is particularly confusing because:
- The code compiles without errors
- It only fails at runtime in dev mode
- The error message doesn't hint at the missing conditional dependency configuration

## Common Mistakes

- Importing a deployment class in runtime code
- Using a build-time-only library in a recorder
- Trying to access runtime config during augmentation (build time)
- Putting user-facing API classes in the deployment module instead of runtime
- **Forgetting `conditionalDevDependencies` when adding a `runtime-dev` module** —
  classes compile but are not loaded at runtime in dev mode
- Forgetting to mark dev-only dependencies as `optional` in runtime-dev modules
