---
name: classloading-and-runtime-dev
description: >
  Quarkus split classloading model, runtime-dev module wiring, conditional
  dependencies, and common classloading mistakes.
---

# Classloading and Runtime-Dev Modules

## The Two ClassLoaders

1. **Deployment ClassLoader** — Used at build time. Has access to all
   dependencies including deployment-only libraries.
2. **Runtime ClassLoader** — Used at application runtime. Only has access to
   runtime dependencies.

## Core Rules

- **Runtime code MUST NOT reference deployment classes.** Violations cause
  `ClassNotFoundException` at runtime.
- **Deployment code CAN reference runtime classes.**
- **Recorders bridge the gap.** A `@Recorder` lives in the runtime module but
  is invoked from deployment build steps.

## Runtime-Dev Modules

Extensions providing dev-mode-only functionality (e.g., Dev UI pages) use a
`runtime-dev/` module. These classes are **only** loaded in dev mode, not in
production. This requires two steps:

### 1. Register as conditional dev dependency

In the **runtime** module's `pom.xml`, configure `quarkus-extension-maven-plugin`:

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

**Without this, runtime-dev classes will NOT be loaded in dev mode** — you get
`ClassNotFoundException`.

### 2. Guard build steps with IsLocalDevelopment

```java
@BuildSteps(onlyIf = IsLocalDevelopment.class)
class MyExtensionDevUIProcessor {
    // Dev UI build steps here
}
```

## Why This Matters

Forgetting `conditionalDevDependencies` is particularly confusing because:
- The code compiles without errors
- It only fails at runtime in dev mode
- The error message doesn't hint at the missing configuration

## Common Mistakes

- Importing a deployment class in runtime code
- Using a build-time-only library in a recorder
- Accessing runtime config during augmentation (build time)
- Putting user-facing API classes in deployment instead of runtime
- Forgetting `conditionalDevDependencies` for `runtime-dev` modules
