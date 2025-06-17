# Scoped Project Dependencies Implementation

## Problem Identified

Previously, cross-module goal dependencies were calculated using **ALL reactor projects** (~932 projects in Quarkus), which created dependencies between unrelated projects.

### The Issue

**Before**: When calculating cross-module dependencies for `maven-install:install` in project A:
- Used ALL 932 reactor projects as potential dependencies
- Created dependencies like: `project-z:maven-jar:jar` even if project A had no Maven dependency on project Z
- Result: Incorrect dependencies between unrelated projects

## Solution Implemented

### 1. Calculate Actual Project Dependencies First

Added `calculateProjectDependencies()` method that:
- Analyzes each project's actual Maven dependencies (`project.getDependencies()`)
- Maps `groupId:artifactId` to `MavenProject` objects  
- Returns only workspace projects that are actual Maven dependencies

```java
private Map<MavenProject, List<MavenProject>> calculateProjectDependencies() {
    // Build artifact mapping for workspace projects
    Map<String, MavenProject> artifactToProject = new HashMap<>();
    for (MavenProject project : reactorProjects) {
        String key = MavenUtils.formatProjectKey(project);
        artifactToProject.put(key, project);
    }
    
    // Calculate dependencies for each project
    for (MavenProject project : reactorProjects) {
        List<MavenProject> dependencies = new ArrayList<>();
        
        for (Dependency dep : project.getDependencies()) {
            String depKey = dep.getGroupId() + ":" + dep.getArtifactId();
            MavenProject targetProject = artifactToProject.get(depKey);
            if (targetProject != null && !targetProject.equals(project)) {
                dependencies.add(targetProject);
            }
        }
        projectDependencies.put(project, dependencies);
    }
}
```

### 2. Updated Goal Dependency Calculation Order

**New Process Flow**:
1. Calculate actual project dependencies for all projects
2. For each project, get its actual dependencies (not all reactor projects)
3. Calculate goal dependencies using only actual dependencies
4. Generate targets with precise dependencies

```java
// OLD: Used all reactor projects
Map<String, List<String>> goalDependencies = calculateGoalDependencies(project, workspaceRoot);

// NEW: Use only actual dependencies  
List<MavenProject> actualDependencies = projectDependencies.getOrDefault(project, new ArrayList<>());
Map<String, List<String>> goalDependencies = calculateGoalDependencies(project, actualDependencies);
```

### 3. Updated Cross-Module Dependency Calculation

The `getCrossModuleGoalsForPhase()` method now receives only actual dependencies instead of all reactor projects:

```java
// In TargetDependencyService.calculateGoalDependencies()
List<String> crossModuleGoals = getCrossModuleGoalsForPhase(project, effectivePhase, actualDependencies);
```

## Results

### More Precise Dependencies
**Before**: Project A's `maven-install:install` depended on goals from ALL 932 projects
**After**: Project A's `maven-install:install` depends only on goals from projects that A actually depends on via Maven

### Example Scenario

**Project**: `io.quarkus:quarkus-core`  
**Actual Maven Dependencies**: `io.quarkus:quarkus-common`, `io.quarkus:quarkus-base`

**Before**: Cross-module dependencies included:
- `io.quarkus:quarkus-hibernate:maven-jar:jar` ❌ (no actual dependency)
- `io.quarkus:quarkus-kubernetes:maven-install:install` ❌ (no actual dependency)
- ... (930+ other unrelated projects)

**After**: Cross-module dependencies include only:
- `io.quarkus:quarkus-common:maven-jar:jar` ✅ (actual dependency)
- `io.quarkus:quarkus-base:maven-install:install` ✅ (actual dependency)

## Performance & Correctness Benefits

1. **Correct Dependencies**: Goals only depend on goals from projects they actually depend on
2. **Faster Analysis**: Fewer cross-module dependencies to calculate and track
3. **Better Nx Caching**: More accurate dependency graph enables better incremental builds
4. **Cleaner Task Graph**: Eliminates spurious dependencies between unrelated projects

## Summary

Cross-module goal dependencies are now scoped to actual Maven project dependencies, eliminating incorrect dependencies between unrelated projects and creating a more accurate and efficient task graph for Nx.