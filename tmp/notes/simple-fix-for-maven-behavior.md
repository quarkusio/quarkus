# Simple Fix: Make Nx Orchestrate Exactly Like Maven

You're absolutely right! Phases should be in the pipeline. The current structure is actually mostly correct:

## Current Structure (Good)

**Phase Pipeline:**
```
validate → compile → test → package → verify → install
```

**Phase Dependencies:**
- `install` depends on `verify` + `maven-install:install` 
- `verify` depends on `package` + `maven-failsafe:verify`
- `package` depends on `test` + `maven-jar:jar`
- etc.

**Goal Dependencies:**
- `maven-install:install` depends on `verify` (preceding phase)
- `maven-jar:jar` depends on `test` (preceding phase)
- etc.

This correctly mimics Maven's behavior where phases run in sequence and coordinate their goals.

## The ONLY Issue: Cross-Project Dependencies

**Problem Lines:**

```java
// Line 88 in calculatePhaseDependencies():
dependsOn.add("^" + phase);

// Line 65 in calculateGoalDependencies(): 
dependsOn.add("^" + effectivePhase);
```

**Issue:** Maven NEVER automatically builds dependency projects. `mvn install` only builds the current project.

## Simple Fix

Remove just those two lines:

```java
public List<String> calculatePhaseDependencies(String phase, Map<String, TargetConfiguration> allTargets, 
                                               MavenProject project, List<MavenProject> reactorProjects) {
    List<String> dependsOn = new ArrayList<>();
    
    // Add dependency on preceding phase ✓
    List<String> phaseDependencies = getPhaseDependencies(phase);
    dependsOn.addAll(phaseDependencies);
    
    // Add dependencies on all goals that belong to this phase ✓
    List<String> goalsForPhase = getGoalsForPhase(phase, allTargets);
    dependsOn.addAll(goalsForPhase);
    
    // REMOVE THIS LINE:
    // dependsOn.add("^" + phase);
    
    return dependsOn;
}

public List<String> calculateGoalDependencies(MavenProject project, String executionPhase, 
                                              String targetName, List<MavenProject> reactorProjects) {
    List<String> dependsOn = new ArrayList<>();
    
    String effectivePhase = // ... phase detection logic ...
    
    if (effectivePhase != null && !effectivePhase.isEmpty()) {
        // Add dependency on preceding phase ✓
        String precedingPhase = getPrecedingPhase(effectivePhase);
        if (precedingPhase != null && !precedingPhase.isEmpty()) {
            dependsOn.add(precedingPhase);
        }
        
        // REMOVE THIS LINE:
        // dependsOn.add("^" + effectivePhase);
    }
    
    return dependsOn;
}
```

## Result: Perfect Maven Behavior

With this fix:

```bash
nx install core/runtime
# Runs exactly like: cd core/runtime && mvn install
# Only builds core/runtime (no dependency projects)
# Same phase and goal execution order
```

The phase pipeline structure is perfect - the only issue was the automatic cross-project building that Maven doesn't do.