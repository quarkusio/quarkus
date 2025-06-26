# Missing Dependencies Analysis

## Current State (Confirmed by Analysis Output)

### ✅ Working Correctly
1. **Target Names**: Include execution IDs (`maven-enforcer:enforce@enforce`)
2. **Goal Strings**: Include execution IDs (`"org.apache.maven.plugins:maven-enforcer-plugin:enforce@enforce"`)

### ❌ Broken
1. **Dependencies**: All targets with execution IDs have empty `dependsOn: []`

## Root Cause
The dependency calculation happens before target creation, but the key used to store/lookup dependencies doesn't match the final target name.

## Evidence from Analysis Output
```json
"maven-enforcer:enforce@enforce": {
  "options": {
    "goals": ["org.apache.maven.plugins:maven-enforcer-plugin:enforce@enforce"]
  },
  "dependsOn": [],  // ❌ Should not be empty
  "metadata": {
    "executionId": "enforce"
  }
}
```

## Fix Status
Changes implemented but not taking effect due to build cache:

1. **ExecutionPlanAnalysisService.kt**: Updated to store target names with execution IDs  
2. **TargetGenerationService.kt**: Updated `getTargetNameFromGoal()` to lookup execution IDs
3. **Dependency Resolution**: Updated to use consistent naming

## Next Steps
1. Find way to bypass build cache completely
2. Verify dependency resolution works with execution ID consistency
3. Test that `nx validate build-parent` works with correct dependencies