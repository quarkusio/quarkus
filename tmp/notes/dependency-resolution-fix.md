# Dependency Resolution Fix for Execution IDs

## Problem Identified
When target names were changed to include execution IDs (e.g., `maven-enforcer:enforce@enforce`), the dependency resolution system was still looking for old target names without execution IDs, causing `dependsOn` configurations to break.

## Root Cause
1. **Target Creation**: Goals created with execution IDs in `createGoalTarget()`
2. **Analysis Storage**: `ExecutionPlanAnalysisService` was storing target names without execution IDs
3. **Dependency Resolution**: `getTargetNameFromGoal()` couldn't find targets with execution IDs
4. **Result**: Phase targets couldn't depend on goal targets due to name mismatch

## Changes Made

### 1. Updated ExecutionPlanAnalysisService.kt
Fixed analysis to store target names WITH execution IDs:
```kotlin
// Line 509 - now includes execution ID
val targetName = getTargetName(pluginArtifactId, goal, mojoExecution.executionId)
```

### 2. Enhanced getTargetNameFromGoal() in TargetGenerationService.kt
Updated to lookup execution IDs from analysis:
```kotlin
private fun getTargetNameFromGoal(goalName: String, project: MavenProject): String {
    // Extract artifactId and goal from goalName
    // Lookup execution info from analysis
    // Return target name WITH execution ID if found
    // Fallback to target name WITHOUT execution ID
}
```

### 3. Updated Phase Target Generation
Modified to pass project parameter to `getTargetNameFromGoal()` for proper execution ID lookup.

## Expected Result
- Goal targets created with execution IDs: `maven-enforcer:enforce@enforce`
- Phase targets can find and depend on goal targets correctly
- `dependsOn` configurations work with new target naming scheme

This ensures consistency between target creation and dependency resolution when execution IDs are present.