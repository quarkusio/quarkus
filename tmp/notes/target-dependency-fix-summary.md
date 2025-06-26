# Target Dependency Fix Summary

## Issue Fixed
The pre-clean target was missing the dependency on `maven-clean:clean@clean-cache-dirs`.

## Root Cause
Target names with execution IDs were being incorrectly processed in the dependency resolution system:

1. **Target Creation**: Targets were correctly created with execution IDs (e.g., `maven-clean:clean@clean-cache-dirs`)
2. **Phase Analysis**: Execution plans correctly stored target names with execution IDs
3. **Dependency Resolution**: `getGoalsCompletedByPhase()` was incorrectly converting target names back to groupId:artifactId:goal format, losing execution IDs
4. **Target Matching**: Phase targets couldn't find goal targets because the names didn't match

## Solution
Fixed the dependency resolution process to preserve execution IDs:

### 1. Fixed `getGoalsCompletedByPhase()` in ExecutionPlanAnalysisService.kt
**Before**: Complex conversion logic that stripped execution IDs
```kotlin
// Convert to proper plugin:goal format
for (goal in phaseGoals) {
    if (goal.contains(":")) {
        val parts = goal.split(":")
        when {
            parts.size >= 3 -> {
                val groupId = parts[0]
                val artifactId = parts[1]
                val goalName = parts[2]
                uniqueGoals.add("$groupId:$artifactId:$goalName")
            }
            // ... more conversion logic
        }
    }
}
```

**After**: Simple preservation of target names
```kotlin
// Use the goal names as-is since they're already in proper target format with execution IDs
uniqueGoals.addAll(phaseGoals)
```

### 2. Simplified `getTargetNameFromGoal()` in TargetGenerationService.kt
**Before**: Complex lookup and conversion logic
**After**: Simple pass-through since goal names are already properly formatted target names
```kotlin
private fun getTargetNameFromGoal(goalName: String, project: MavenProject): String {
    // goalName is now already a properly formatted target name (e.g., "maven-clean:clean@clean-cache-dirs")
    return goalName
}
```

## Testing
Added smoke test to verify the fix:
- `nx validate quarkus-core` should now succeed
- Test will catch regressions in target dependency resolution

## Files Modified
1. `maven-plugin/src/main/kotlin/ExecutionPlanAnalysisService.kt` - Fixed goal name processing
2. `maven-plugin/src/main/kotlin/TargetGenerationService.kt` - Simplified target name conversion
3. `e2e-smoke.test.ts` - Added validation smoke test

## Impact
This fix ensures that phase targets can correctly depend on goal targets that have execution IDs, maintaining proper Maven dependency chains in Nx.