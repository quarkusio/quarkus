# Target Naming Analysis for maven-clean:clean@clean-cache-dirs

## Issue
The pre-clean target is missing the dependency on `maven-clean:clean@clean-cache-dirs`.

## Root Cause Analysis

The issue is with how target names with execution IDs are handled in the dependency resolution process.

### Key Findings

1. **Target Creation**: In `generatePluginGoalTargets`, targets are created with execution IDs:
   - Target name: `maven-clean:clean@clean-cache-dirs`
   - Created via: `ExecutionPlanAnalysisService.getTargetName(plugin.artifactId, goal, execution.id)`

2. **Phase Analysis Storage**: In `ProjectExecutionAnalysis.addExecutionPlan()`:
   - Line 525: `phaseToGoalsMap.computeIfAbsent(phase) { mutableListOf() }.add(targetName)`
   - Stores the full target name with execution ID

3. **Phase Dependency Resolution**: In `getGoalsCompletedByPhase()`:
   - Calls `getGoalsForPhase(project, phase)` which returns target names with execution IDs
   - Previously was converting these back to groupId:artifactId:goal format, losing execution IDs
   - Now fixed to use target names as-is

4. **Target Name Mapping**: In `getTargetNameFromGoal()`:
   - Now simplified to return the target name as-is since it's already properly formatted

## Profile Issue
The `clean-cache-dirs` execution is defined in a profile that may not be active during analysis:
- Profile ID: `clean-cache`
- Activation: Property `clean-cache` != "false"

## Changes Made

1. **Fixed `getGoalsCompletedByPhase()`**: 
   - Removed conversion logic that was stripping execution IDs
   - Now preserves target names with execution IDs

2. **Simplified `getTargetNameFromGoal()`**:
   - Now returns goal names as-is since they're already properly formatted target names

## Next Steps
- Verify that the profile activation is working correctly
- Test if the maven-clean:clean@clean-cache-dirs target is actually being detected and created