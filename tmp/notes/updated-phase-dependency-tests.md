# Updated Phase Dependency Tests

## What Changed in Phase Dependencies

You simplified the `calculatePhaseDependencies()` method in `TargetDependencyService.java` to only include goals that belong to the phase:

### Before
```java
public List<String> calculatePhaseDependencies(...) {
    List<String> dependsOn = new ArrayList<>();
    
    // Add dependency on preceding phase
    List<String> phaseDependencies = getPhaseDependencies(phase, project);
    dependsOn.addAll(phaseDependencies);
    
    // Add dependencies on all goals that belong to this phase
    List<String> goalsForPhase = getGoalsForPhase(phase, allTargets);
    dependsOn.addAll(goalsForPhase);
    
    // Add cross-module dependencies using Nx ^ syntax
    dependsOn.add("^" + phase);
    
    return dependsOn;
}
```

### After
```java
public List<String> calculatePhaseDependencies(...) {
    List<String> dependsOn = new ArrayList<>();
    
    // Add dependencies on all goals that belong to this phase
    List<String> goalsForPhase = getGoalsForPhase(phase, allTargets);
    dependsOn.addAll(goalsForPhase);
    
    return dependsOn;
}
```

## Test Updates Made

### 1. Updated `testCalculatePhaseDependencies()`

**Before**: Expected cross-module dependency `^test`
```java
assertTrue("Should contain cross-module test dependency", 
    dependencies.contains("^test"));
```

**After**: Expects only goals for the phase, no cross-module or preceding phase dependencies
```java
// Phase dependencies now only contain goals that belong to the phase
// Cross-module dependencies are handled at the goal level, not phase level
// In test environment with no actual goals, this may be empty
assertTrue("Phase dependencies should only contain goals for the phase", 
    dependencies.isEmpty() || dependencies.stream().allMatch(dep -> !dep.startsWith("^")));
```

### 2. Updated `testCalculatePhaseDependencies_PostIntegrationTest()`

**Before**: Expected preceding phase `integration-test` and cross-module dependency `^post-integration-test`
```java
// Should contain the preceding phase (integration-test)
boolean containsIntegrationTest = dependencies.contains("integration-test");
// Should also contain cross-module dependency
assertTrue("Should contain cross-module dependency", dependencies.contains("^post-integration-test"));
```

**After**: Expects only goals for the phase
```java
// Phase dependencies now only contain goals that belong to the phase
// No longer contain preceding phases or cross-module dependencies
// In test environment with no actual goals, this may be empty
assertTrue("Phase dependencies should only contain goals for the phase", 
    dependencies.isEmpty() || dependencies.stream().allMatch(dep -> !dep.startsWith("^") && !dep.equals("integration-test")));
```

## Why This Change Makes Sense

### Cleaner Separation of Concerns
- **Phase targets**: Simple orchestration that depends on goals in that phase
- **Goal targets**: Handle all the complex dependencies (goal-to-goal, cross-module)

### More Precise Dependencies
- Phase dependencies are now purely about aggregating goals within the phase
- All complex dependency logic (lifecycle ordering, cross-module) is handled at the goal level

### Better Nx Performance
- Simpler phase targets with fewer dependencies
- More granular caching at the goal level where it matters most

## Test Results
✅ All tests now pass
✅ Tests correctly reflect the simplified phase dependency behavior
✅ Goal-level dependencies still handle all the complex dependency scenarios

## Summary
The tests have been updated to match your simplified phase dependency implementation, which removes cross-module and preceding phase dependencies from phase targets and keeps phases as simple orchestration entry points.