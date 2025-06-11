# New Maven Task Graph Structure Implementation

## What We've Implemented

### 1. **Moved Logic to Java Analyzer**
- Goal organization by phase is now handled in `MavenModelReader.java`
- Goal-to-goal dependency calculation is in Java (where Maven model knowledge exists)
- New JSON output includes `goalsByPhase` and `goalDependencies`

### 2. **New Data Structure** 
The Java analyzer now outputs:
```json
{
  "project-path": {
    "relevantPhases": ["clean", "validate", "compile", "test", "package"],
    "pluginGoals": [...],
    "phaseDependencies": {"test": ["compile"], "package": ["test"]},
    "goalsByPhase": {
      "compile": ["maven-compiler:compile"],  
      "test": ["surefire:test", "quarkus:test"]
    },
    "goalDependencies": {
      "surefire:test": ["maven-compiler:compile"],
      "quarkus:test": ["maven-compiler:compile"]
    }
  }
}
```

### 3. **Task Graph Structure**
The new approach creates a better dependency graph:

**Phase Targets** (Aggregators):
- `compile` depends on `["maven-compiler:compile"]`  
- `test` depends on `["surefire:test", "quarkus:test"]`
- `package` depends on `["maven-jar:jar"]`

**Goal Targets** (Actual Work):
- `maven-compiler:compile` has no dependencies (first in chain)
- `surefire:test` depends on `["maven-compiler:compile"]`
- `quarkus:test` depends on `["maven-compiler:compile"]`
- `maven-jar:jar` depends on `["surefire:test", "quarkus:test"]`

## Benefits of New Structure

### 1. **More Granular Control**
- You can run specific goals: `nx maven-compiler:compile`
- Goals can run in parallel if they don't depend on each other
- Better caching at the goal level

### 2. **Cleaner Dependencies**
- Goals depend on goals (not phases)
- Phases act as convenient aggregators
- Follows Maven's actual execution model

### 3. **Better Parallelization**
```
maven-compiler:compile
         ↓
    ┌─ surefire:test ──┐
    │                 ├─ maven-jar:jar
    └─ quarkus:test ──┘
```
Both test goals can run in parallel after compilation.

## Current Issues to Fix

### 1. **Goal Assignment Logic**
Some goals are being assigned to wrong phases:
- `maven-compiler:testCompile` should go to `test-compile` phase
- Current logic has priority issues in phase assignment

### 2. **Goal Dependencies Not Generated**
The `goalDependencies` field is empty because:
- Need to ensure prerequisite phases have goals to depend on
- Need better fallback logic when phases don't exist

### 3. **TypeScript Plugin Updates Needed**
The TypeScript plugin needs updates to:
- Use `goalsByPhase` instead of calculating organization
- Use `goalDependencies` for goal targets
- Create phase targets that depend on their goals

## Next Steps

1. **Fix Java goal assignment logic** - ensure goals go to correct phases
2. **Implement goal dependency generation** - ensure goals depend on prerequisite goals
3. **Update TypeScript plugin** - use new data structure from Java
4. **Test complete pipeline** - verify targets are created correctly

## Expected Final Result

When complete, running `nx graph` should show:
- Phase targets that aggregate their goals
- Goal targets with proper dependencies
- Clean separation between compilation, testing, and packaging
- Proper parallelization opportunities