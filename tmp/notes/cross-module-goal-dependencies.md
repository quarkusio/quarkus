# Cross-Module Goal Dependencies Implementation

## What Was Done

### Replaced Phase-Based Cross-Module Dependencies with Goal-Based Dependencies

**Old Behavior**: 
```java
dependsOn.add("^" + effectivePhase);  // e.g., "^install"
```

**New Behavior**: 
```java
List<String> crossModuleGoals = getCrossModuleGoalsForPhase(project, effectivePhase, reactorProjects);
dependsOn.addAll(crossModuleGoals);  // e.g., ["io.quarkus:core:maven-jar:jar", "io.quarkus:deployment:maven-install:install"]
```

### Key Changes Made

1. **Updated `calculateGoalDependencies()` method**: Now calls `getCrossModuleGoalsForPhase()` instead of adding phase dependency

2. **Implemented `getCrossModuleGoalsForPhase()` method**:
   - Iterates through all reactor projects (excluding current project)
   - Gets all goals for the specified phase in each project
   - Returns goals with Nx project name prefix using `MavenUtils.formatProjectKey()`
   - Example: For "install" phase, returns `["io.quarkus:core:maven-jar:jar", "io.quarkus:deployment:maven-install:install"]`

3. **Updated unit tests**: Modified tests to expect goal-based cross-module dependencies instead of phase-based ones

### Behavior Examples

**Before**: 
- `maven-install:install` depended on `^install` (phase across all modules)

**After**:
- `maven-install:install` depends on actual goals from install phase across modules:
  - `io.quarkus:core:maven-jar:jar` (from core module)  
  - `io.quarkus:deployment:maven-install:install` (from deployment module)

### Technical Implementation

```java
public List<String> getCrossModuleGoalsForPhase(MavenProject currentProject, String phase, List<MavenProject> reactorProjects) {
    List<String> crossModuleGoals = new ArrayList<>();
    
    // For each project in reactor (excluding current)
    for (MavenProject otherProject : reactorProjects) {
        if (otherProject != null && !otherProject.equals(currentProject)) {
            // Get all goals for this phase in the other project
            List<String> phaseGoals = executionPlanAnalysisService.getGoalsForPhase(otherProject, phase);
            
            for (String goal : phaseGoals) {
                if (goal.contains(":")) {
                    // Add with Nx project name to create project:goal dependency
                    String nxProjectName = MavenUtils.formatProjectKey(otherProject);
                    crossModuleGoals.add(nxProjectName + ":" + goal);
                }
            }
        }
    }
    
    return crossModuleGoals;
}
```

## Test Results
✅ All unit tests passing
✅ Goals now depend on specific goals from other modules, not phases
✅ Cross-module dependencies are more granular and precise

## Impact
- **More Precise Dependencies**: Goals depend on actual goals from other modules, not entire phases
- **Better Nx Caching**: More granular dependencies enable better incremental builds
- **Consistent Architecture**: Both intra-project and cross-module dependencies are now goal-based

## Summary
Cross-module dependencies now use actual Maven goals with Nx project names instead of phases, completing the goal-to-goal dependency architecture as requested. Dependencies like `^install` are now replaced with specific goal dependencies like `io.quarkus:core:maven-jar:jar`, `io.quarkus:deployment:maven-install:install`.

### Final Result
Goals now depend on specific `nxProjectName:goalName` across modules, where `nxProjectName` is the Nx project identifier in "groupId:artifactId" format. This provides precise, granular dependencies that Nx can cache and execute efficiently.