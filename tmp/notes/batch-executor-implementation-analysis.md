# Batch Executor Implementation Analysis

## Overview

The Maven plugin uses a sophisticated batch executor architecture that takes Nx targets and converts them to Maven commands. Here's how it works:

## Core Components

### 1. Kotlin Batch Executor (`NxMavenBatchExecutor.kt`)

**Location**: `/Users/jason/projects/triage/java/quarkus3/maven-plugin/src/main/kotlin/NxMavenBatchExecutor.kt`

**Key Methods**:
- `executeBatch()` - Main entry point for batch execution
- `executeMultiProjectGoalsWithInvoker()` - Uses Maven Invoker API to execute goals
- `findMavenExecutable()` - Locates Maven installation

**How it works**:
1. Takes command line arguments: `<goals> <workspaceRoot> <projects> [verbose]`
2. Parses goals (comma-separated list like "maven-jar-plugin:jar,maven-install-plugin:install")
3. Uses Maven Invoker API to execute all goals in a single Maven reactor session
4. Supports multi-project execution using Maven's `-pl` option
5. Returns structured JSON results with per-goal execution details

**Key Features**:
- Single Maven session execution (maintains reactor context)
- Proper Maven executable detection (MAVEN_HOME or PATH)
- Detailed per-goal result tracking
- Multi-project support with proper project selection

### 2. TypeScript Executor (`executor.ts`)

**Location**: `/Users/jason/projects/triage/java/quarkus3/maven-plugin/src/executors/maven-batch/executor.ts`

**Key Functions**:
- `runExecutor()` - Single task execution
- `batchMavenExecutor()` - Batch execution for multiple tasks
- `executeMultiProjectMavenBatch()` - Multi-project batch execution

**How Nx targets are converted**:
1. Nx calls the executor with `MavenBatchExecutorOptions`
2. Options include:
   - `goals`: Array of Maven goals to execute
   - `projectRoot`: Target project directory
   - `verbose`: Debug logging flag
   - `mavenPluginPath`: Path to Maven plugin jar

3. Executor builds Java command:
   ```bash
   java -Dmaven.multiModuleProjectDirectory="${workspaceRoot}" 
        -cp "${classpath}" 
        NxMavenBatchExecutor 
        "${goalsString}" 
        "${workspaceRoot}" 
        "${projectRoot}" 
        ${verboseFlag}
   ```

4. Parses JSON output from Kotlin batch executor

### 3. Target Generation Service (`TargetGenerationService.kt`)

**Location**: `/Users/jason/projects/triage/java/quarkus3/maven-plugin/src/main/kotlin/TargetGenerationService.kt`

**Key Methods**:
- `generateTargets()` - Main target generation entry point
- `generatePluginGoalTargets()` - Creates targets for Maven plugin goals
- `generatePhaseTargets()` - Creates targets for Maven lifecycle phases
- `createGoalTarget()` - Creates individual goal target configurations

**Target Creation Process**:
1. **Plugin Goal Targets**: Each Maven plugin goal becomes an Nx target
   - Target name: `pluginArtifactId:goalName` (e.g., `compiler:compile`)
   - Executor: `@nx-quarkus/maven-plugin:maven-batch`
   - Options include the specific Maven goal to execute

2. **Phase Targets**: Maven lifecycle phases become orchestration targets
   - Target name: Phase name (e.g., `compile`, `test`, `install`)
   - Executor: `nx:noop` (no-op, just dependency orchestration)
   - Dependencies: List of goal targets that complete the phase

**Target Configuration Example**:
```typescript
{
  "executor": "@nx-quarkus/maven-plugin:maven-batch",
  "options": {
    "goals": ["org.apache.maven.plugins:maven-compiler-plugin:compile"],
    "projectRoot": "./",
    "verbose": false,
    "mavenPluginPath": "maven-plugin",
    "failOnError": true
  },
  "dependsOn": ["resources:resources"],
  "inputs": ["{projectRoot}/pom.xml", "{projectRoot}/src/main/java/**/*"],
  "outputs": ["{projectRoot}/target/classes"]
}
```

## Execution Flow

### Single Target Execution
1. Nx calls TypeScript executor with target configuration
2. TypeScript executor extracts Maven goals from options
3. Builds Java command to invoke Kotlin batch executor
4. Kotlin batch executor uses Maven Invoker API
5. Returns structured results back through the chain

### Batch Execution (Multiple Targets)
1. Nx task graph contains multiple Maven targets
2. `batchMavenExecutor()` collects all goals and projects
3. Deduplicates goals and projects
4. Executes ALL goals across ALL projects in single Maven session
5. All tasks get same success/failure result

## Maven Integration Points

### Maven Invoker API Usage
```kotlin
val invoker = DefaultInvoker()
val request = DefaultInvocationRequest().apply {
    pomFile = rootPomFile
    baseDirectory = workspaceDir
    setGoals(goals) // All goals in single session
    projects = projectList // Multi-project selection
}
val result = invoker.execute(request)
```

### Project Selection
- Uses Maven's `-pl` (projects list) option
- Supports both root project (`.`) and child modules
- Maintains proper Maven reactor ordering

### Goal Format
Goals are specified in Maven's standard format:
- `groupId:artifactId:goal` (full format)
- `artifactId:goal` (short format)
- Examples: `compiler:compile`, `org.apache.maven.plugins:maven-surefire-plugin:test`

## Architecture Benefits

1. **Maven Compatibility**: Uses official Maven APIs for exact behavior
2. **Performance**: Single Maven session reduces startup overhead
3. **Reactor Support**: Maintains Maven's multi-module project context
4. **Caching**: Nx can cache individual goal results
5. **Parallelism**: Nx orchestrates parallel execution while Maven handles dependencies

## File Locations Summary

**Core Implementation**:
- `/Users/jason/projects/triage/java/quarkus3/maven-plugin/src/main/kotlin/NxMavenBatchExecutor.kt`
- `/Users/jason/projects/triage/java/quarkus3/maven-plugin/src/executors/maven-batch/executor.ts`
- `/Users/jason/projects/triage/java/quarkus3/maven-plugin/src/executors/maven-batch/impl.ts`

**Target Generation**:
- `/Users/jason/projects/triage/java/quarkus3/maven-plugin/src/main/kotlin/TargetGenerationService.kt`
- `/Users/jason/projects/triage/java/quarkus3/maven-plugin/src/main/kotlin/ExecutionPlanAnalysisService.kt`

**Configuration**:
- `/Users/jason/projects/triage/java/quarkus3/maven-plugin/executors.json`
- `/Users/jason/projects/triage/java/quarkus3/maven-plugin/src/executors/maven-batch/schema.json`

The implementation successfully bridges Nx's task orchestration with Maven's execution model while maintaining compatibility and performance.