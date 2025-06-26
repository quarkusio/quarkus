# Execution ID Analysis Results

## Overview
This document contains a comprehensive analysis of execution IDs and targets with execution IDs in the Maven plugin codebase. Execution IDs are Maven's way to uniquely identify plugin executions within a project, allowing the same plugin goal to be executed multiple times with different configurations.

## Key Findings

### 1. TargetMetadata Model (Line 10)
**File**: `/Users/jason/projects/triage/java/quarkus3/maven-plugin/src/main/kotlin/model/TargetMetadata.kt`
- Contains `executionId: String?` property that stores the execution ID for each target
- This is the core data structure that holds execution ID information

### 2. Target Name Generation Strategy (Lines 361-373)
**File**: `/Users/jason/projects/triage/java/quarkus3/maven-plugin/src/main/kotlin/ExecutionPlanAnalysisService.kt`
```kotlin
fun getTargetName(artifactId: String?, goal: String, executionId: String?): String {
    val pluginName = normalizePluginName(artifactId)
    
    // Use execution ID if available and not a default one
    if (!executionId.isNullOrEmpty() && 
        !executionId.startsWith("default-") && 
        executionId != "default-cli") {
        return "$pluginName:$executionId"
    }
    
    // Fall back to goal-based naming
    return "$pluginName:$goal"
}
```

### 3. Target Generation with Execution ID (Lines 184-194, 255)
**File**: `/Users/jason/projects/triage/java/quarkus3/maven-plugin/src/main/kotlin/TargetGenerationService.kt`
- Line 185: `val targetName = ExecutionPlanAnalysisService.getTargetName(plugin.artifactId, goal, execution.id)`
- Line 255: `executionId = execution.id` - Sets the execution ID in target metadata

### 4. Target Dependency Calculations (Lines 157, 514-516)
**File**: Multiple files show execution ID is used in dependency calculations
- Uses execution ID preference when creating target names for dependencies
- Line 157 in `TargetDependencyService.kt`: Uses execution ID in target name generation for dependencies

### 5. Execution Plan Analysis (Lines 507-511, 514-516)
**File**: `/Users/jason/projects/triage/java/quarkus3/maven-plugin/src/main/kotlin/ExecutionPlanAnalysisService.kt`
```kotlin
val execInfo = ExecutionInfo(
    goal, phase, pluginArtifactId,
    mojoExecution.executionId,  // Line 508 - captures execution ID
    "${mojoExecution.plugin.groupId}:$pluginArtifactId"
)
// Line 514 - Uses execution ID in target name generation
val targetName = getTargetName(pluginArtifactId, goal, mojoExecution.executionId)
```

## Specific Examples of Targets with Execution IDs

### From Generated Project Graph (`graph.json`):

1. **Properties Plugin (Line 146)**:
   - Target: `properties:set-system-properties` 
   - ExecutionId: `"default"`
   - Plugin: `org.codehaus.mojo:properties-maven-plugin`

2. **Maven Enforcer Plugin (Line 193)**:
   - Target: `enforcer:enforce`
   - ExecutionId: `"enforce"`
   - Plugin: `org.apache.maven.plugins:maven-enforcer-plugin`

3. **Buildnumber Plugin (Line 241)**:
   - Target: `buildnumber:create`
   - ExecutionId: `"get-scm-revision"`
   - Plugin: `org.codehaus.mojo:buildnumber-maven-plugin`

### From Test POM Files:

1. **Complex Test Project** (`complex-test/pom.xml`):
   - `maven-compiler-plugin` with executions:
     - `id="default-compile"` → Target would be `compiler:default-compile` (but falls back to `compiler:compile` due to default- prefix filtering)
     - `id="default-testCompile"` → Similar fallback behavior

2. **Quarkus-like Test Project** (`quarkus-like-test/pom.xml`):
   - `maven-compiler-plugin`:
     - `id="default-compile"` (Line 37)
     - `id="test-compile"` (Line 49) → Would create target `compiler:test-compile`
   - `maven-antrun-plugin`:
     - `id="process-classes-custom"` (Line 88) → Creates target `antrun:process-classes-custom`

## Execution ID Filtering Logic

The system implements smart filtering for execution IDs:
- **Skips** `default-*` prefixed execution IDs (like `default-compile`, `default-testCompile`)
- **Skips** `default-cli` execution ID
- **Uses** meaningful execution IDs like `get-scm-revision`, `enforce`, `test-compile`

This prevents generic default execution IDs from cluttering target names while preserving meaningful custom execution IDs.

## Technical Implementation

### Target Name Priority:
1. **Preferred**: `pluginName:executionId` (when execution ID is meaningful)
2. **Fallback**: `pluginName:goal` (when execution ID is default/empty)

### Data Flow:
1. Maven execution plans are analyzed to extract `MojoExecution` objects
2. Each execution contains `executionId`, `goal`, `phase`, and `plugin` information
3. Target names are generated using the preference logic
4. Execution IDs are stored in `TargetMetadata` for reference
5. Dependencies between targets use the same naming strategy for consistency

## Files Containing Execution ID References

1. **Core Model**: `model/TargetMetadata.kt`
2. **Target Generation**: `TargetGenerationService.kt`
3. **Execution Analysis**: `ExecutionPlanAnalysisService.kt`
4. **Dependency Calculation**: `TargetDependencyService.kt`
5. **Batch Execution**: `NxMavenBatchExecutor.kt`
6. **Test Files**: Various test POM files in `src/test/resources/unit/`

## Summary

The Maven plugin extensively uses execution IDs to create precise, meaningful target names that reflect Maven's actual execution model. This allows Nx to maintain Maven's exact execution semantics while providing enhanced caching and parallelization capabilities.