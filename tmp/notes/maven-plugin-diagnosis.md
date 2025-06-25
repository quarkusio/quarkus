# Maven Plugin Issue Diagnosis

## Problem
`nx install maven-plugin` was failing with 12 Maven tasks failing during batch execution.

## Root Cause Analysis

### 1. Maven Home Detection Issue (FIXED)
- **Issue**: The `NxMavenBatchExecutor` could not find the Maven executable because `MAVEN_HOME` was not set
- **Error**: "Maven executable: 'mvn' not found at project dir: '...' nor maven home: 'null'"
- **Fix**: Added `findMavenExecutable()` function to detect Maven from PATH when MAVEN_HOME is not set
- **Location**: `/maven-plugin/src/main/kotlin/NxMavenBatchExecutor.kt` lines 111-126

### 2. Batch Execution Working
- Individual goals work correctly when tested directly
- The batch executor successfully finds Maven and executes goals
- Full plugin coordinates work (e.g., `org.jetbrains.kotlin:kotlin-maven-plugin:compile`)

### 3. Current Issue - Batch vs Sequential Execution
- **Problem**: Nx is trying to run all 12 Maven goals in parallel/batch mode
- **Goals being executed**: kotlin:compile, maven-resources:resources, maven-compiler:compile, etc.
- **Issue**: These goals have dependency relationships that should be respected
- **Example**: `maven-compiler:compile` depends on `kotlin:compile` and `maven-resources:resources`

## Maven Goals and Dependencies
The install lifecycle includes these goals in order:
1. `kotlin:compile` (process-sources phase)
2. `maven-resources:resources` (process-resources phase) 
3. `maven-compiler:compile` (compile phase)
4. `maven:descriptor` (process-classes phase)
5. `kotlin:test-compile` (process-test-sources phase)
6. `maven-resources:testResources` (process-test-resources phase)
7. `maven-compiler:testCompile` (test-compile phase)
8. `maven-surefire:test` (test phase)
9. `maven:addPluginArtifactMetadata` (package phase)
10. `maven-dependency:copy-dependencies` (package phase)
11. `maven-jar:jar` (package phase)
12. `maven-install:install` (install phase)

## Next Steps
- Need to investigate how Nx is batching these goals
- The batch executor may need to understand Maven lifecycle phases
- Or Nx needs to respect the dependency order in the task graph