# Phase Dependency Issue Analysis

## Problem
Phase targets like `pre-clean` are missing their `dependsOn` configurations. For example:
- ❌ Current: `pre-clean` has empty `dependsOn: []`  
- ✅ Expected: `pre-clean` should depend on `"maven-clean:clean@clean-cache-dirs"`

## Root Cause
Phase dependency resolution is breaking due to the execution ID handling issue:

1. **Phase Dependency Logic**: `pre-clean` phase should depend on goals that execute during `pre-clean`
2. **Goal Collection**: `getGoalsCompletedByPhase()` returns goals like `"org.apache.maven.plugins:maven-clean-plugin:clean"`
3. **Target Name Conversion**: `getTargetNameFromGoal()` should convert to `"maven-clean:clean@clean-cache-dirs"`
4. **Target Lookup**: Checks if target exists in `allTargets` with that exact name
5. **Dependency Addition**: Adds the target name to phase's `dependsOn`

## Failure Points
1. **`extractGoalFromTargetName()`**: Was incorrectly handling execution IDs (fixed but not compiled due to cache)
2. **`getTargetNameFromGoal()`**: May not be finding execution info correctly
3. **Target lookup**: Converted name might not match actual target names

## Expected Flow (Once Cache Fixed)
1. `getGoalsCompletedByPhase("pre-clean")` → `["org.apache.maven.plugins:maven-clean-plugin:clean"]`
2. `getTargetNameFromGoal("org.apache.maven.plugins:maven-clean-plugin:clean")` → `"maven-clean:clean@clean-cache-dirs"`
3. `allTargets.containsKey("maven-clean:clean@clean-cache-dirs")` → `true`
4. `pre-clean.dependsOn` → `["maven-clean:clean@clean-cache-dirs"]`

## Impact
- Phase targets lose their orchestration capability
- Build dependency graph is broken
- Phases run without their prerequisite goals

## Status
Fix implemented but blocked by build cache preventing compilation of corrected `extractGoalFromTargetName()` method.