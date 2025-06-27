# Maven Executor Analysis

## Overview
Located the Maven executor implementation in the Nx plugin for Maven integration. The executor handles both single task and batch execution of Maven goals.

## Key Files Found

### Main Executor Implementation
- `/home/jason/projects/triage/java/quarkus/maven-plugin/src/executors/maven-batch/executor.ts`
  - Main TypeScript executor that wraps Java-based batch execution
  - Contains both single executor and batch executor functions
  - Uses Java subprocess execution via `execSync`

### Schema Definition
- `/home/jason/projects/triage/java/quarkus/maven-plugin/src/executors/maven-batch/schema.json`
  - Defines executor configuration options and validation
  - Includes timeout configuration with default of 300000ms (5 minutes)

### Re-export Module
- `/home/jason/projects/triage/java/quarkus/maven-plugin/src/executors/maven-batch/impl.ts`
  - Simple re-export module for executor functions

## Timeout Configuration Details

### Interface Definition (Lines 6-14)
```typescript
export interface MavenBatchExecutorOptions {
  goals: string[];
  projectRoot?: string;
  verbose?: boolean;
  mavenPluginPath?: string;
  timeout?: number;           // <- Timeout option here
  outputFile?: string;
  failOnError?: boolean;
}
```

### Default Timeout Values
- **Schema default**: 300000ms (5 minutes) - defined in schema.json line 34
- **Code default**: 300000ms (5 minutes) - used in executor.ts line 49 and 307
- Applied to both single executor and batch executor functions

### Usage in Code
1. **Single Executor** (line 49): `timeout = 300000`
2. **Batch Executor** (line 307): `timeout = 300000` 
3. **execSync calls** (lines 116, 343): `timeout: timeout` parameter passed to child process

## Executor Architecture

### Single Task Executor (`runExecutor`)
- Executes Maven goals for a single project
- Validates plugin compilation and dependencies
- Uses Java subprocess with `NxMavenBatchExecutor` class
- Parses JSON output from Java executor

### Batch Executor (`batchMavenExecutor`)
- Handles multiple tasks from Nx task graph
- Collects all goals and projects from task graph
- Executes everything in single batch for efficiency
- All tasks share same success/failure result

### Java Integration
- Calls compiled Java class `NxMavenBatchExecutor`
- Requires Maven plugin to be compiled (`target/classes`)
- Uses Maven APIs through Java subprocess execution
- Command pattern: `java -cp "classpath" NxMavenBatchExecutor "goals" "workspace" "projects" verbose`

## Error Handling
- Validates plugin directory existence
- Checks for compiled classes and dependencies
- Parses JSON output with error recovery
- Timeout handling via execSync timeout parameter
- Comprehensive error reporting with context