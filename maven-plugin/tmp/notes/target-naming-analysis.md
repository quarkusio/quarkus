# Target Naming Analysis - Execution IDs vs Goals

## Investigation Summary

I analyzed the Maven plugin codebase to determine if any Nx targets are currently defined using execution IDs rather than goals as their names.

## Key Findings

### 1. Target Naming Strategy
**Current approach**: All targets use **goal-based naming**, not execution ID-based naming.

The target naming pattern is: `{pluginArtifactId}:{goalName}`

Examples from the core project:
- `maven-compiler:compile`
- `maven-surefire:test`
- `maven-jar:jar`
- `quarkus-extension:extension-descriptor`

### 2. Code Analysis

**Target Generation Logic** (`TargetGenerationService.kt`):
```kotlin
fun getTargetName(artifactId: String?, goal: String): String {
    val pluginName = normalizePluginName(artifactId)
    return "$pluginName:$goal"
}
```

**Execution Processing** (`createGoalTarget` method):
- Processes `PluginExecution` objects from Maven
- Iterates through each `execution.goals` 
- Creates targets named by goal, not execution ID
- Execution ID is stored in metadata but NOT used for target naming

### 3. Execution ID Usage

Execution IDs are **captured in metadata only**:
```kotlin
val metadata = TargetMetadata("goal", generateGoalDescription(plugin.artifactId, goal)).apply {
    this.plugin = pluginKey
    this.goal = goal
    executionId = execution.id  // Stored as metadata, not used for naming
    // ...
}
```

### 4. Examples from Live Targets

From the `io.quarkus:quarkus-core` project:

**Goal-based target names:**
- `maven-enforcer:enforce` (executionId: "enforce")
- `buildnumber:create` (executionId: "get-scm-revision") 
- `maven-compiler:compile` (executionId: "default-compile")
- `maven-source:jar-no-fork` (executionId: "attach-sources")

**Pattern observed**: Target names follow `plugin:goal` format, while execution IDs are stored separately in metadata.

## Conclusion

**Answer**: No targets are currently defined using execution IDs as names.

- All targets use goal-based naming: `{artifactId}:{goal}`
- Execution IDs are preserved in target metadata for reference
- This design allows multiple executions of the same goal to share a single target
- The approach follows Maven's semantic model where goals are the atomic units of work

## Code Locations

- Target generation: `/maven-plugin/src/main/kotlin/TargetGenerationService.kt`
- Target naming: `ExecutionPlanAnalysisService.getTargetName()`
- Metadata model: `/maven-plugin/src/main/kotlin/model/TargetMetadata.kt`