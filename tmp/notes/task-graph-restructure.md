# Task Graph Restructure - Goals as Core Tasks

## Problem Identified
The original task graph structure was inverted:
- **Phases**: Used batch executor to run all goals → bypassed individual goal caching
- **Goals**: Created as individual targets but never used by phases

This prevented granular caching and proper task orchestration.

## Solution Implemented

### New Task Graph Structure
1. **Goals are the core cacheable tasks**
   - Each Maven goal (`maven-compiler:compile`, `maven-jar:jar`, etc.) is an individual Nx target
   - Uses batch executor with single goal for proper Maven session context
   - Can be cached independently by Nx

2. **Phases are entry points that depend on goals**
   - Phases use `nx:noop` executor (just orchestration)
   - Phases have `dependsOn` array pointing to their constituent goal targets
   - Provide convenient entry points (e.g., `nx install` → runs all install-phase goals)

### Code Changes

**TargetGenerationService.java - generatePhaseTargets():**
```java
// OLD: Phase runs goals in batch
target = new TargetConfiguration("@nx-quarkus/maven-plugin:maven-batch");
options.put("goals", goalsToComplete); // Batch execution

// NEW: Phase depends on individual goal targets  
target = new TargetConfiguration("nx:noop");
target.setDependsOn(goalTargetDependencies); // Orchestration
```

**Added getTargetNameFromGoal() method:**
- Converts goal names to target names for dependency resolution
- Ensures phases properly reference existing goal targets

## Results

### Task Execution Verification
```bash
nx install maven-plugin
```

**Output shows correct structure:**
```
> nx run @nx-quarkus/maven-plugin:"maven-compiler:compile"
> nx run @nx-quarkus/maven-plugin:"maven-jar:jar" 
> nx run @nx-quarkus/maven-plugin:"maven-install:install"
> nx run @nx-quarkus/maven-plugin:"maven-surefire:test"
```

### Benefits Achieved

1. **Granular Caching** ✅
   - Each goal can be cached independently
   - Nx can skip already-completed goals
   - Massive performance improvement for incremental builds

2. **Proper Task Dependencies** ✅
   - Task graph reflects actual Maven goal dependencies
   - Nx handles orchestration and parallelization
   - Goals run in correct order automatically

3. **Convenient Entry Points** ✅
   - Phases still work as expected (`nx compile`, `nx install`, etc.)
   - Phases orchestrate multiple goals transparently
   - Users get both granular control and convenience

4. **Maven Session Context** ✅
   - Individual goals still use batch executor (1 goal per batch)
   - Proper Maven setup and context preservation
   - Consistent execution environment

## Task Graph Comparison

### Before (Incorrect)
```
install phase → [batch: compile,jar,install] (no caching)
compile goal → [individual task, unused]
jar goal → [individual task, unused] 
install goal → [individual task, unused]
```

### After (Correct)
```
install phase → dependsOn: [compile goal, jar goal, install goal]
compile goal → [individual cacheable task]
jar goal → [individual cacheable task]
install goal → [individual cacheable task]
```

## Impact
This restructuring transforms the Maven plugin from a monolithic batch system into a proper Nx-integrated build tool with granular caching and optimal task orchestration. Each Maven goal becomes a first-class Nx citizen while maintaining the convenience of Maven phases.