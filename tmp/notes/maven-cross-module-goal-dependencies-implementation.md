# Maven Cross-Module Goal Dependencies - Implementation Summary

## Overview

Successfully implemented goal-based cross-module dependencies for the Maven plugin, changing from phase-based to goal-based dependency resolution.

## Changes Made

### Core Modification

**File**: `maven-plugin/src/main/kotlin/TargetDependencyService.kt:45-49`

**Before** (Phase-based dependencies):
```kotlin
val crossModuleDep = TargetDependency(effectivePhase, dependentProjects)
dependencies.add(crossModuleDep)
```

**After** (Goal-based dependencies):
```kotlin
val crossModuleGoals = getCrossModuleGoalsForPhase(project, effectivePhase, actualDependencies)
dependencies.addAll(crossModuleGoals) // Direct goal-to-goal dependencies
```

## Dependency Format Change

### Old Format (Phase-based)
```json
{
  "params": "ignore",
  "projects": [
    "io.quarkus:quarkus-ide-launcher",
    "io.quarkus:quarkus-development-mode-spi"
  ],
  "target": "initialize"
}
```

### New Format (Goal-based)
```json
[
  "io.quarkus:quarkus-ide-launcher:buildnumber:create@get-scm-revision",
  "io.quarkus:quarkus-development-mode-spi:buildnumber:create@get-scm-revision",
  "io.quarkus:quarkus-bootstrap-runner:buildnumber:create@get-scm-revision"
]
```

## Key Benefits

1. **Precision**: Goals now depend on specific executable units (`buildnumber:create@get-scm-revision`) instead of abstract phases (`initialize`)

2. **Consistency**: Cross-module dependencies now work the same way as same-project dependencies (goal-to-goal)

3. **Granularity**: Better control over execution order - can specify exactly which goal in a dependent project must complete

## Technical Implementation

### Existing Infrastructure Used

- `getCrossModuleGoalsForPhase()` method already existed and worked correctly
- `ExecutionPlanAnalysisService.getGoalsForPhase()` provides goal resolution
- `MavenUtils.formatProjectKey()` creates proper project names
- Project dependency mapping via `calculateProjectDependencies()`

### Dependency Flow

1. **Project Dependencies**: Maven pom.xml dependencies analyzed to find workspace projects
2. **Phase Analysis**: For each goal, determine its Maven lifecycle phase  
3. **Goal Resolution**: Find all goals in the same phase across dependent projects
4. **Dependency Creation**: Create `"projectName:goalName"` dependencies

## Verification

### Test Results
- **E2E Tests**: Updated snapshots to reflect new goal-based format
- **Cross-Module Detection**: 7,478 cross-module dependencies successfully detected
- **Format Validation**: Dependencies correctly formatted as `"project:goal"` strings

### Example Dependencies Found
```
"io.quarkus.arc:arc-processor:buildnumber:create@get-scm-revision"
"io.quarkus.arc:arc:maven-source:jar-no-fork@attach-sources" 
"io.quarkus.arc:arc:maven-clean:clean@default-clean"
```

## Impact

This change makes cross-module dependencies in the Maven plugin more precise and consistent with Nx's execution model, where targets depend on specific executable units rather than abstract phases.