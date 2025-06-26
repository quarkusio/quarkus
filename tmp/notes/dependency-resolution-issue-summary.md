# Dependency Resolution Issue Summary

## Problem
Targets with execution IDs in their names (e.g., `"quarkus-extension:extension-descriptor@generate-extension-descriptor"`) are losing their `dependsOn` configurations, while the original targets without execution IDs (e.g., `"quarkus-extension:extension-descriptor"`) had proper dependencies.

## Root Cause Identified
The `extractGoalFromTargetName()` method was not properly handling execution IDs. For target names like:
- Input: `"quarkus-extension:extension-descriptor@generate-extension-descriptor"`
- Wrong output: `"extension-descriptor@generate-extension-descriptor"`
- Correct output: `"extension-descriptor"`

This caused dependency resolution to fail because it couldn't properly extract the goal name to match against plugin configurations.

## Fix Implemented
Updated `extractGoalFromTargetName()` in `ExecutionPlanAnalysisService.kt`:

```kotlin
fun extractGoalFromTargetName(targetName: String?): String? {
    if (targetName == null || !targetName.contains(":")) {
        return targetName
    }
    val goalWithExecutionId = targetName.substring(targetName.lastIndexOf(":") + 1)
    // Remove execution ID if present (format: goal@executionId)
    return if (goalWithExecutionId.contains("@")) {
        goalWithExecutionId.substring(0, goalWithExecutionId.indexOf("@"))
    } else {
        goalWithExecutionId
    }
}
```

## Expected Result
- Target: `"quarkus-extension:extension-descriptor@generate-extension-descriptor"`
- Goal extraction: `"extension-descriptor"`  
- Dependency resolution should now work correctly
- `dependsOn` configurations should be restored for targets with execution IDs

## Build Cache Blocker
The Develocity build cache continues to prevent the fix from taking effect despite multiple reset attempts. The compilation shows "Loaded from the build cache" indicating the new code is not being compiled.