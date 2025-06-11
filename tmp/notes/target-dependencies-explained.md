# Maven Target Dependencies Explained

## What Are Target Dependencies?

Target dependencies tell Nx which tasks need to run before other tasks. Think of it like a recipe - you need to mix ingredients before you can bake, and you need to bake before you can frost.

## Types of Dependencies in Maven Projects

### 1. **Phase Dependencies** (Maven Lifecycle)
These follow Maven's standard build lifecycle:

```
validate → compile → test-compile → test → package → verify → install → deploy
```

**Example:**
- `test` depends on `compile` (can't test uncompiled code)
- `package` depends on `test` (don't package if tests fail)
- `install` depends on `verify` (verify before installing)

### 2. **Plugin Goal Dependencies** (Framework-specific)
These are special tasks that plugins add:

**Quarkus Examples:**
- `quarkus:dev` depends on `compile` (need compiled code for dev mode)
- `quarkus:build` depends on `test` (run tests before building)
- `quarkus:test` depends on `test-compile` (need compiled tests)

**Spring Boot Examples:**
- `spring-boot:run` depends on `compile`
- `spring-boot:build-image` depends on `package`

### 3. **Cross-Project Dependencies**
In multi-module projects, one project's targets depend on another project's targets:

```
core:compile → web:compile (web module needs core compiled first)
core:test → integration:test (integration tests need core tests to pass)
```

## How Nx Uses These Dependencies

### Smart Execution Order
When you run `nx test my-project`, Nx automatically:

1. **Analyzes dependencies**: Looks at what `test` needs
2. **Builds dependency graph**: `validate → compile → test-compile → test`
3. **Runs in order**: Executes each step only if needed
4. **Caches results**: Skips steps that haven't changed

### Example Execution Flow

```bash
$ nx serve my-quarkus-app

# Nx internally runs:
1. validate (check project is valid)
2. compile (compile source code) 
3. quarkus:dev (start development server)
```

### Parallel Execution
When possible, Nx runs independent targets in parallel:

```
┌─ project-a:compile ─┐
│                     ├─ project-a:test
├─ project-b:compile ─┤
│                     ├─ project-b:test  
└─ project-c:compile ─┘
```

## Real-World Benefits

### 1. **Automatic Dependency Resolution**
You don't need to remember to compile before testing - Nx handles it:

```bash
# This automatically runs compile first if needed
nx test my-app
```

### 2. **Incremental Builds**
If source code hasn't changed, Nx skips compilation:

```bash
$ nx test my-app
✓ my-app:compile [existing outputs match the cache, left as is]
✓ my-app:test [2s]
```

### 3. **Affected Project Detection**
Change one file, and Nx only rebuilds what's actually affected:

```bash
$ nx affected:test
# Only tests projects that changed or depend on changed projects
```

### 4. **Framework Integration**
The plugin detects framework-specific commands automatically:

- **Quarkus**: `nx serve` → `mvn quarkus:dev`
- **Spring Boot**: `nx serve` → `mvn spring-boot:run`
- **Standard Maven**: `nx build` → `mvn package`

## Target Configuration Structure

Each target has this structure:

```json
{
  "build": {
    "executor": "@nx/run-commands:run-commands",
    "options": {
      "command": "mvn package",
      "cwd": "{projectRoot}"
    },
    "inputs": [
      "{projectRoot}/src/**/*",
      "{projectRoot}/pom.xml"
    ],
    "outputs": [
      "{projectRoot}/target/*.jar"
    ],
    "dependsOn": ["test"]
  }
}
```

- **`executor`**: How to run the command
- **`command`**: The actual Maven command
- **`inputs`**: Files that affect this target (for caching)
- **`outputs`**: Files this target produces (for caching)
- **`dependsOn`**: Other targets that must run first

## Summary

Target dependencies ensure your build pipeline runs in the correct order, automatically handles prerequisites, and maximizes caching and parallelization. The Maven plugin discovers these dependencies by analyzing your `pom.xml` files and creates the appropriate Nx target configurations.