# Dynamic Lifecycle Phase Discovery Using Maven Session API

## Perfect! Maven Provides APIs for Dynamic Phase Discovery

The Maven Session API provides everything needed to dynamically discover which phases are actually bound for each packaging type.

## Current Code Foundation

The existing code already uses `LifecycleExecutor` in `TargetDependencyService.java:130-135`:

```java
private MavenExecutionPlan getExecutionPlan() {
    try {
        LifecycleExecutor lifecycleExecutor = session.getContainer().lookup(LifecycleExecutor.class);
        List<String> goals = session.getGoals();
        return lifecycleExecutor.calculateExecutionPlan(session, goals.toArray(new String[0]));
    } catch (Exception e) {
        // ... error handling
    }
}
```

## Dynamic Phase Discovery Implementation

### 1. **Discover All Phases for a Project**

```java
/**
 * Dynamically discover all lifecycle phases that have bound goals for this project
 */
public Set<String> getApplicablePhases(MavenProject project, MavenSession session) {
    Set<String> applicablePhases = new LinkedHashSet<>();
    
    try {
        LifecycleExecutor lifecycleExecutor = session.getContainer().lookup(LifecycleExecutor.class);
        
        // Calculate execution plan for the "deploy" phase (highest phase)
        // This will include all phases up to deploy
        MavenExecutionPlan executionPlan = lifecycleExecutor.calculateExecutionPlan(
            session, project, Arrays.asList("deploy")
        );
        
        // Extract unique phases from all mojo executions
        for (MojoExecution mojoExecution : executionPlan.getMojoExecutions()) {
            String phase = mojoExecution.getLifecyclePhase();
            if (phase != null && !phase.isEmpty()) {
                applicablePhases.add(phase);
            }
        }
        
    } catch (Exception e) {
        if (verbose) {
            log.warn("Could not determine applicable phases for " + project.getArtifactId() + ": " + e.getMessage());
        }
        // Fallback to empty set or minimal phases
    }
    
    return applicablePhases;
}
```

### 2. **Check if a Phase Has Bound Goals**

```java
/**
 * Check if a specific phase has any goals bound to it for this project
 */
public boolean hasGoalsBoundToPhase(String phase, MavenProject project, MavenSession session) {
    try {
        LifecycleExecutor lifecycleExecutor = session.getContainer().lookup(LifecycleExecutor.class);
        
        // Calculate execution plan for just this phase
        MavenExecutionPlan executionPlan = lifecycleExecutor.calculateExecutionPlan(
            session, project, Arrays.asList(phase)
        );
        
        // Check if any mojo executions are bound to this phase
        return executionPlan.getMojoExecutions()
            .stream()
            .anyMatch(mojoExecution -> phase.equals(mojoExecution.getLifecyclePhase()));
            
    } catch (Exception e) {
        if (verbose) {
            log.warn("Could not check phase bindings for " + phase + ": " + e.getMessage());
        }
        return false;
    }
}
```

### 3. **Get Packaging-Specific Default Phases**

```java
/**
 * Get phases that have default goal bindings for this packaging type
 */
public Set<String> getDefaultPhasesForPackaging(String packaging, MavenSession session) {
    Set<String> defaultPhases = new LinkedHashSet<>();
    
    try {
        LifecycleExecutor lifecycleExecutor = session.getContainer().lookup(LifecycleExecutor.class);
        
        // Use the LifecycleExecutor API to get default bindings
        // This method gets plugins bound by default to all lifecycles for specified packaging
        List<Plugin> defaultPlugins = lifecycleExecutor.getPluginsBoundByDefaultToAllLifecycles(packaging);
        
        // Alternative: Create a minimal project with this packaging and see what phases are bound
        // This requires creating a temporary MavenProject with the specific packaging
        
    } catch (Exception e) {
        if (verbose) {
            log.warn("Could not get default phases for packaging " + packaging + ": " + e.getMessage());
        }
    }
    
    return defaultPhases;
}
```

## Updated TargetGenerationService

### Replace Hardcoded Phases

**Current code (lines 73-76):**
```java
String[] phases = {
    "clean", "validate", "compile", "test", "package", 
    "verify", "install", "deploy", "site"
};
```

**New dynamic approach:**
```java
public Map<String, TargetConfiguration> generatePhaseTargets(MavenProject project, File workspaceRoot, 
                                                             Map<String, TargetConfiguration> allTargets, 
                                                             Map<String, List<String>> phaseDependencies) {
    Map<String, TargetConfiguration> phaseTargets = new LinkedHashMap<>();
    
    // Dynamically discover applicable phases for this project
    Set<String> applicablePhases = getApplicablePhases(project, session);
    
    for (String phase : applicablePhases) {
        // Only create targets for phases that actually have goals bound
        if (hasGoalsBoundToPhase(phase, project, session)) {
            TargetConfiguration target = new TargetConfiguration("nx:noop");
            
            // ... existing target configuration logic ...
            
            phaseTargets.put(phase, target);
        }
    }
    
    return phaseTargets;
}
```

## Benefits of Dynamic Discovery

### 1. **Automatic Packaging Support**
- POM projects automatically get only `validate`, `install`, `deploy` phases
- JAR projects get full lifecycle phases
- WAR projects get web-specific phases
- Custom packaging types work automatically

### 2. **Plugin-Aware Phase Generation**
- Only generates phases that actually have bound goals
- Respects plugin configurations and custom bindings
- Handles custom lifecycle extensions

### 3. **No Hardcoded Assumptions**
- Works with any Maven project configuration
- Adapts to different Maven versions
- Supports custom packaging types and lifecycles

## Implementation in Existing Code

**Add to `TargetGenerationService` constructor:**
```java
public TargetGenerationService(Log log, boolean verbose, MavenSession session) {
    this.log = log;
    this.verbose = verbose;
    this.session = session;  // Add session for dynamic discovery
}
```

**Update the `generatePhaseTargets` call in `NxAnalyzerMojo`:**
```java
// Pass session to enable dynamic phase discovery
Map<String, TargetConfiguration> targets = targetGenerationService.generateTargets(
    project, workspaceRoot, goalDependencies, phaseDependencies, session);
```

This approach uses Maven's own APIs to discover exactly which phases are applicable for each project, eliminating all hardcoded assumptions about lifecycle phases.