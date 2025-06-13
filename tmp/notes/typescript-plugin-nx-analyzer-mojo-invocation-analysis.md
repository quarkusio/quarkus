# TypeScript Plugin to NxAnalyzerMojo Invocation Analysis

## Summary
The TypeScript plugin (maven-plugin2.ts) invokes the NxAnalyzerMojo Java class through Maven plugin execution, not direct Java calls.

## Key Invocation Flow

### 1. Main Invocation Point
**File:** `/home/jason/projects/triage/java/quarkus/maven-plugin2.ts`
**Lines:** 272-276

```typescript
const child = spawn(options.mavenExecutable, [
  'io.quarkus:maven-plugin-v2:analyze',
  `-Dnx.outputFile=${outputFile}`,
  '-q'
], {
```

The TypeScript plugin spawns a Maven process that executes the Maven plugin goal `io.quarkus:maven-plugin-v2:analyze`.

### 2. Maven Plugin Configuration
**File:** `/home/jason/projects/triage/java/quarkus/maven-plugin-v2/pom.xml`
**Lines:** 8-11, 71-72

```xml
<groupId>io.quarkus</groupId>
<artifactId>maven-plugin-v2</artifactId>
<version>999-SNAPSHOT</version>
<packaging>maven-plugin</packaging>

<configuration>
    <goalPrefix>nx</goalPrefix>
</configuration>
```

The Maven plugin is configured with groupId `io.quarkus`, artifactId `maven-plugin-v2`, and goal prefix `nx`. However, the TypeScript code invokes the `analyze` goal.

### 3. NxAnalyzerMojo Class
**File:** `/home/jason/projects/triage/java/quarkus/maven-plugin-v2/src/main/java/NxAnalyzerMojo.java`
**Lines:** 22-23

```java
@Mojo(name = "analyze", aggregator = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class NxAnalyzerMojo extends AbstractMojo {
```

The Java class is annotated as a Maven Mojo with name "analyze", making it executable as `io.quarkus:maven-plugin-v2:analyze`.

## Communication Flow

1. **TypeScript initiates:** `generateBatchNxConfigFromMavenAsync()` function (line 246)
2. **Maven spawn:** Uses Node.js `spawn()` to execute Maven command (line 272)
3. **Maven plugin execution:** Maven loads and executes NxAnalyzerMojo.analyze goal
4. **Java analysis:** NxAnalyzerMojo processes all Maven projects and generates JSON output
5. **File-based communication:** Java writes results to `maven-analysis.json` (line 263)
6. **TypeScript reads:** Reads JSON file and parses results (line 331)

## Key Parameters

- **Output file:** Controlled via `-Dnx.outputFile=${outputFile}` system property
- **Default output:** `maven-plugin-v2/maven-analysis.json` in workspace root
- **Input:** All `pom.xml` files found by TypeScript plugin

## Helper Classes
The NxAnalyzerMojo uses two generator classes:
- `CreateNodesResultGenerator.java` - Generates Nx-compatible project configurations
- `CreateDependenciesGenerator.java` - Generates inter-project dependencies

## Process Management
The TypeScript plugin tracks active Maven processes for cleanup using:
- `activeMavenProcesses` Set to track spawned processes
- Cleanup handlers for SIGINT, SIGTERM, and process exit events