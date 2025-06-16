# How Nx Should Orchestrate Goals Exactly Like Maven

## The Problem: Current Nx Orchestration vs Maven

### Maven's Exact Behavior for `mvn install`

When you run `mvn install` on a single project, Maven:

1. **Only builds the current project** (no cross-project dependencies)
2. **Runs all goals for all phases up to 'install'** in exact order
3. **Each goal depends only on the goals from earlier phases**

**Example Maven execution order for `mvn install`:**
```
validate phase goals → compile phase goals → test phase goals → package phase goals → verify phase goals → install phase goals
```

### Current Nx Problems

#### Problem 1: **Automatic Cross-Project Dependencies**
```java
// Line 65 in TargetDependencyService.calculateGoalDependencies():
dependsOn.add("^" + effectivePhase);

// Line 88 in TargetDependencyService.calculatePhaseDependencies():  
dependsOn.add("^" + phase);
```

**Issue:** Nx automatically adds `^install` dependencies, making `nx install core/runtime` build all dependency projects first. Maven doesn't do this.

#### Problem 2: **Wrong Goal Dependencies**
```java
// Line 57 in TargetDependencyService.calculateGoalDependencies():
dependsOn.add(precedingPhase);
```

**Issue:** Goals depend on entire preceding **phases**, not on specific **goals** from preceding phases.

#### Problem 3: **Phase Dependencies Include Goals**
```java  
// Lines 84-85 in TargetDependencyService.calculatePhaseDependencies():
List<String> goalsForPhase = getGoalsForPhase(phase, allTargets);
dependsOn.addAll(goalsForPhase);
```

**Issue:** Phase targets depend on their own goals, creating circular dependencies.

## How Maven Really Works

### For `mvn install` on quarkus-core:

**Maven runs these goals in order:**
1. `maven-enforcer:enforce` (validate phase)
2. `maven-compiler:compile` (compile phase)  
3. `maven-compiler:testCompile` (test-compile phase)
4. `maven-surefire:test` (test phase)
5. `maven-jar:jar` (package phase)
6. `maven-install:install` (install phase)

**Dependency rules:**
- Each goal only depends on **all goals from earlier phases**
- No cross-project dependencies
- No phase-to-goal dependencies

## Correct Nx Orchestration

### Fix 1: Remove Cross-Project Dependencies

```java
// REMOVE these lines:
// Line 65: dependsOn.add("^" + effectivePhase);
// Line 88: dependsOn.add("^" + phase);

public List<String> calculateGoalDependencies(MavenProject project, String executionPhase, 
                                              String targetName, List<MavenProject> reactorProjects) {
    List<String> dependsOn = new ArrayList<>();
    
    String effectivePhase = // ... phase detection logic ...
    
    if (effectivePhase != null && !effectivePhase.isEmpty()) {
        // Add dependencies on ALL GOALS from all preceding phases
        List<String> precedingGoals = getAllGoalsFromPrecedingPhases(effectivePhase, allTargets);
        dependsOn.addAll(precedingGoals);
        
        // REMOVE: dependsOn.add("^" + effectivePhase);
    }
    
    return dependsOn;
}
```

### Fix 2: Goals Depend on Preceding Goals, Not Phases

```java
public List<String> getAllGoalsFromPrecedingPhases(String currentPhase, Map<String, TargetConfiguration> allTargets) {
    List<String> precedingGoals = new ArrayList<>();
    
    // Get all phases that come before currentPhase
    List<String> precedingPhases = getAllPrecedingPhases(currentPhase);
    
    // Collect all goals from those phases
    for (String phase : precedingPhases) {
        List<String> goalsInPhase = getGoalsForPhase(phase, allTargets);
        precedingGoals.addAll(goalsInPhase);
    }
    
    return precedingGoals;
}
```

### Fix 3: Remove Phase Targets Entirely or Make Them Pure Coordinators

**Option A: Remove Phase Targets**
```java
// Don't create phase targets at all
// Only create goal targets with proper dependencies
```

**Option B: Make Phases Pure Coordinators**
```java
public List<String> calculatePhaseDependencies(String phase, Map<String, TargetConfiguration> allTargets, 
                                               MavenProject project, List<MavenProject> reactorProjects) {
    List<String> dependsOn = new ArrayList<>();
    
    // Phase only depends on goals up to and including this phase
    List<String> allGoalsUpToPhase = getAllGoalsUpToPhase(phase, allTargets);
    dependsOn.addAll(allGoalsUpToPhase);
    
    // REMOVE: dependsOn.add("^" + phase);
    
    return dependsOn;
}
```

## Correct Dependency Structure

### For `nx install core/runtime`:

**maven-install:install** should depend on:
- `maven-enforcer:enforce`
- `maven-compiler:compile` 
- `maven-compiler:testCompile`
- `maven-surefire:test`
- `maven-jar:jar`

**install phase** (if kept) should depend on:
- `maven-install:install` (only)

**No cross-project dependencies at all.**

## Example of Correct Implementation

```java
public class TargetDependencyService {
    
    public List<String> calculateGoalDependencies(MavenProject project, String executionPhase, 
                                                  String targetName, Map<String, TargetConfiguration> allTargets) {
        List<String> dependsOn = new ArrayList<>();
        
        String effectivePhase = determineEffectivePhase(executionPhase, targetName);
        
        if (effectivePhase != null) {
            // Get all goals from all phases that come before this goal's phase
            List<String> precedingPhases = getAllPhasesBefore(effectivePhase);
            
            for (String precedingPhase : precedingPhases) {
                List<String> goalsInPhase = getGoalsForPhase(precedingPhase, allTargets);
                dependsOn.addAll(goalsInPhase);
            }
        }
        
        return dependsOn; // No cross-project dependencies
    }
    
    public List<String> calculatePhaseDependencies(String phase, Map<String, TargetConfiguration> allTargets) {
        List<String> dependsOn = new ArrayList<>();
        
        // Phase depends only on goals in this phase
        List<String> goalsInThisPhase = getGoalsForPhase(phase, allTargets);
        dependsOn.addAll(goalsInThisPhase);
        
        return dependsOn; // No cross-project dependencies
    }
}
```

## Result: Maven-Identical Behavior

With these fixes:

```bash
nx install core/runtime
# Would run exactly the same goals in exactly the same order as:
cd core/runtime && mvn install

# No cross-project building
# No circular dependencies  
# Same goal execution sequence
```

The key insight: **Maven goals depend on preceding goals, not phases. And there are no automatic cross-project dependencies.**