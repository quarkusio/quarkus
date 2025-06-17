# Fixed Target-Dependency Naming Consistency

## Problem Identified

Target names and dependency names used different formats:

- **Target names**: `maven-jar:jar` (normalized, `-plugin` suffix removed)
- **Dependency names**: `maven-jar-plugin:jar` (full artifact ID with `-plugin` suffix)

This caused dependency mismatches where dependencies referenced targets that didn't exist.

## Root Cause

Two different methods for creating goal names:

1. **Target generation** (`TargetGenerationService`): Used `ExecutionPlanAnalysisService.getTargetName()` which normalizes plugin names by removing `-plugin` suffix
2. **Dependency generation** (`TargetDependencyService`): Used raw `plugin.getArtifactId() + ":" + goal` which kept the full plugin name

## Solution Applied

**File**: `TargetDependencyService.java:160`

**Before**:
```java
String pluginGoal = plugin.getArtifactId() + ":" + goal;
```

**After**:
```java
String pluginGoal = ExecutionPlanAnalysisService.getTargetName(plugin.getArtifactId(), goal);
```

## Result

Now both target names and dependencies use the same normalization logic:

- **Target name**: `maven-jar:jar` 
- **Dependency**: `maven-jar:jar` ✅

As verified in test output:
```
[info] Install goal dependencies: [maven-resources:resources, maven-compiler:compile, maven-resources:testResources, maven-compiler:testCompile, maven-surefire:test, maven-jar:jar]
```

## Why This Matters

1. **Dependency Resolution**: Dependencies can now correctly reference existing targets
2. **Nx Task Graph**: Proper dependency chains for incremental builds
3. **Consistency**: Single source of truth for target naming via `ExecutionPlanAnalysisService.getTargetName()`

The fix ensures that when `maven-install:install` depends on `maven-jar:jar`, that target actually exists in the task graph.