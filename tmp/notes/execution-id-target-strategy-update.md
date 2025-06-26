# Execution ID Target Strategy Update

## Summary

Updated the Maven plugin to generate Nx targets using execution IDs when available, while maintaining compatibility with the batch executor.

## Changes Made

### 1. Enhanced Target Naming Strategy

**File**: `ExecutionPlanAnalysisService.kt:353-373`

Added new overloaded `getTargetName()` method that prefers execution IDs:

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

**Strategy**:
- Use execution ID for target naming when it's meaningful (not auto-generated defaults)
- Fall back to goal-based naming for default executions
- Preserve existing behavior for common goals

### 2. Updated Target Generation Points

**Files Updated**:
- `ExecutionPlanAnalysisService.kt:514` - Updated execution plan analysis
- `TargetGenerationService.kt:177` - Updated target generation from executions  
- `NxAnalyzerMojo.kt:337` - Updated analyzer mojo target collection
- `TargetDependencyService.kt:157` - Updated dependency calculation

**Changes**:
- All calls to `getTargetName(artifactId, goal)` updated to `getTargetName(artifactId, goal, executionId)`
- Maintains execution context throughout the target generation pipeline

### 3. Batch Executor Compatibility

**Analysis**: The batch executor already handles execution ID-based targets correctly:

- Target options store actual Maven goals: `"goals" to listOf("$pluginKey:$goal")`
- Execution ID is stored in metadata but doesn't affect goal execution
- Maven Invoker API receives correct goals regardless of target naming

**No changes needed** - existing implementation is compatible.

## Target Name Examples

### Before (Goal-based only):
- `compiler:compile`
- `surefire:test` 
- `jar:jar`

### After (Execution ID preferred):
- `compiler:my-compile-step` (custom execution ID)
- `surefire:integration-tests` (custom execution ID)
- `compiler:compile` (falls back to goal for default executions)

## Benefits

1. **Better Target Identification**: Targets with custom execution IDs are now distinguishable
2. **Maven Compatibility**: Maintains exact Maven execution behavior
3. **Backward Compatibility**: Default executions still use goal-based names
4. **Execution Context Preserved**: Metadata retains both goal and execution ID information

## Edge Cases Handled

1. **Null Execution IDs**: Falls back to goal-based naming
2. **Default Execution IDs**: Uses goal naming for "default-*" and "default-cli"
3. **Common Goals**: Generic plugin goals without execution context use goal naming
4. **Batch Execution**: Goals extracted from options, not target names

## Testing Verification

To verify the changes work correctly:

```bash
# 1. Recompile the Java components
npm run compile-java

# 2. Reset Nx state
nx reset  

# 3. Test target generation
nx show projects --verbose

# 4. Test goal execution  
nx graph --file graph.json
```

The updated implementation provides more meaningful target names while maintaining full compatibility with Maven's execution model.