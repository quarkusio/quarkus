# Batch Executor Analysis

## Current Implementation Summary

The Maven batch executor currently **executes Maven goals only, not execution IDs**. Here's what I found:

### Key Implementation Details

1. **Goal Execution Only**: The batch executor takes Maven goals as string arguments and passes them directly to Maven's invoker API
2. **No Execution ID Support**: While execution IDs are tracked in metadata for target generation, they are not used in the actual execution
3. **Single Maven Session**: All goals are executed in one Maven reactor session for efficiency

### Code Locations

#### Main Batch Executor (Kotlin)
- File: `/maven-plugin/src/main/kotlin/NxMavenBatchExecutor.kt`
- Line 131: `setGoals(goals)` - passes goals directly to Maven invoker
- Line 24-30: Takes comma-separated goals as first argument
- Line 174: Executes via `invoker.execute(request)`

#### TypeScript Executor Interface
- File: `/maven-plugin/src/executors/maven-batch/executor.ts`
- Line 96: `goals.join(',')` - combines goals into comma-separated string
- Line 101: Passes goals string to Java batch executor
- Line 330-336: Multi-project batch execution also uses goals only

### Execution Parameters

The batch executor receives these parameters:
1. **Goals**: Comma-separated list of Maven goals (e.g., "compile,test")
2. **Workspace Root**: Root directory of the Maven workspace
3. **Projects**: Comma-separated list of project paths to build
4. **Verbose**: Boolean flag for detailed output

### What Gets Executed

- **Input**: Maven goals like "maven-compiler-plugin:compile", "maven-surefire-plugin:test"
- **Maven Command**: Uses Maven Invoker API equivalent to `mvn goal1 goal2 -pl project1,project2`
- **Output**: JSON result with execution status, timing, and output for each goal

### Execution ID Usage

While execution IDs are captured in target metadata (`TargetMetadata.executionId`), they are **not used in execution**:
- Target generation stores execution IDs for documentation purposes
- Batch executor ignores execution IDs completely
- Maven goals are executed without specifying which execution to run

## Implications

This means the batch executor runs the **default execution** of each goal, not specific plugin executions that might be configured with custom execution IDs in the POM.