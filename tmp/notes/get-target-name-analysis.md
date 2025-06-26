# GetTargetName Method Analysis

## Summary
Found multiple implementations of `getTargetName` methods in the Kotlin codebase that handle how execution IDs are incorporated into Nx target names.

## Key Findings

### 1. ExecutionPlanAnalysisService.kt (Lines 353-376)
Contains two companion object methods:

#### Method 1: Basic Target Name Generation
```kotlin
fun getTargetName(artifactId: String?, goal: String): String {
    val pluginName = normalizePluginName(artifactId)
    return "$pluginName:$goal"
}
```

#### Method 2: Execution ID Aware Target Name Generation
```kotlin
fun getTargetName(artifactId: String?, goal: String, executionId: String?): String {
    val pluginName = normalizePluginName(artifactId)

    log.debug("Generating target name for plugin: $pluginName, goal: $goal, executionId: $executionId")

    // Use execution ID if available and not a default one
    if (!executionId.isNullOrEmpty() &&
        !executionId.startsWith("default-") &&
        executionId != "default-cli") {
        log.debug("Using execution ID for target name: $executionId")
        return "$pluginName@$executionId"
    }

    // Fall back to goal-based naming
    return "$pluginName:$goal"
}
```

### 2. TargetGenerationService.kt (Lines 190, 284, 337)
Uses the execution ID-aware method:

#### Line 190 (createGoalTarget):
```kotlin
val targetName = ExecutionPlanAnalysisService.getTargetName(plugin.artifactId, goal, execution.id)
```

#### Line 284 (addCommonGoalsForPlugin):
```kotlin
val targetName = ExecutionPlanAnalysisService.getTargetName(artifactId, goal)
```

#### Line 337 (NxAnalyzerMojo):
```kotlin
val targetName = ExecutionPlanAnalysisService.getTargetName(artifactId, goal, execution.id)
```

### 3. TargetDependencyService.kt (Line 157)
Uses the execution ID-aware method:
```kotlin
val pluginGoal = ExecutionPlanAnalysisService.getTargetName(plugin.artifactId, goal, execution.id)
```

## Execution ID Logic

The execution ID incorporation logic:

1. **Uses execution ID format**: `pluginName@executionId` when:
   - Execution ID is not null or empty
   - Execution ID doesn't start with "default-"
   - Execution ID is not "default-cli"

2. **Falls back to goal format**: `pluginName:goal` when:
   - Execution ID is null/empty
   - Execution ID starts with "default-"
   - Execution ID is "default-cli"

## Plugin Name Normalization

The `normalizePluginName` method (line 391-393):
```kotlin
fun normalizePluginName(artifactId: String?): String? {
    return artifactId?.replace("-maven-plugin", "")?.replace("-plugin", "")
}
```

## Issue Analysis

The execution ID logic appears correct, but there might be issues with:

1. **Logging**: The debug logging might not be enabled, making it hard to trace execution ID processing
2. **Execution ID values**: The actual execution IDs might be falling into the "default" categories
3. **Plugin execution configuration**: The executions might not have proper IDs set

## Recommendation

Enable verbose logging to see:
- What execution IDs are being processed
- Which branch of the logic is being taken
- Whether execution IDs are being found in the plugin configurations