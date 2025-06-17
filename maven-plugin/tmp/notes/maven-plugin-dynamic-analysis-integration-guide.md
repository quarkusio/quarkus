# Maven Plugin Dynamic Analysis Integration Guide

## Overview

This guide shows how to replace hardcoded plugin-specific logic with dynamic Maven API introspection for determining plugin and goal behavior.

## Key Maven APIs for Plugin Introspection

### 1. MojoExecution API (Already Available)
```java
// Access through ExecutionPlanAnalysisService
MavenExecutionPlan executionPlan = lifecycleExecutor.calculateExecutionPlan(session, phase);
for (MojoExecution mojoExecution : executionPlan.getMojoExecutions()) {
    String goal = mojoExecution.getGoal();
    String phase = mojoExecution.getLifecyclePhase();
    Plugin plugin = mojoExecution.getPlugin();
    MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();
    Xpp3Dom configuration = mojoExecution.getConfiguration();
}
```

### 2. MojoDescriptor API (Available via MojoExecution)
```java
MojoDescriptor descriptor = mojoExecution.getMojoDescriptor();
if (descriptor != null) {
    String description = descriptor.getDescription();
    boolean requiresProject = descriptor.isProjectRequired();
    String dependencyResolution = descriptor.getDependencyResolutionRequired();
    List<Parameter> parameters = descriptor.getParameters();
}
```

### 3. Parameter Analysis
```java
for (Parameter parameter : descriptor.getParameters()) {
    String name = parameter.getName();
    String type = parameter.getType(); // e.g., "java.io.File"
    String description = parameter.getDescription();
    
    // Check if parameter represents a file/directory
    if ("java.io.File".equals(type) || name.contains("Dir") || name.contains("File")) {
        // This parameter deals with files/directories
    }
}
```

### 4. Configuration Analysis
```java
Xpp3Dom configuration = mojoExecution.getConfiguration();
if (configuration != null) {
    // Recursively analyze configuration elements
    analyzeConfigurationElement(configuration);
}

private void analyzeConfigurationElement(Xpp3Dom element) {
    String name = element.getName();
    String value = element.getValue();
    
    // Look for file/directory configurations
    if (name.toLowerCase().contains("dir") || name.toLowerCase().contains("path")) {
        // This configuration element deals with paths
    }
}
```

## Implementation Example

### Replace DynamicGoalAnalysisService.analyzeMojoExecution()

**Before (empty implementation):**
```java
private GoalBehavior analyzeMojoExecution(String goal, MavenProject project) {
    GoalBehavior behavior = new GoalBehavior();
    // Empty implementation
    return behavior;
}
```

**After (dynamic introspection):**
```java
private GoalBehavior analyzeMojoExecution(String goal, MavenProject project) {
    GoalBehavior behavior = new GoalBehavior();
    
    try {
        // Find MojoExecution for this goal
        MojoExecution mojoExecution = findMojoExecutionForGoal(goal, project);
        if (mojoExecution != null) {
            // Analyze MojoDescriptor
            MojoDescriptor descriptor = mojoExecution.getMojoDescriptor();
            if (descriptor != null) {
                analyzeParameters(descriptor.getParameters(), behavior);
                
                // Check dependency resolution requirements
                if ("compile".equals(descriptor.getDependencyResolutionRequired())) {
                    behavior.setProcessesSources(true);
                }
            }
            
            // Analyze configuration
            Xpp3Dom config = mojoExecution.getConfiguration();
            if (config != null) {
                analyzeConfiguration(config, behavior);
            }
        }
    } catch (Exception e) {
        // Fall back to existing logic
    }
    
    return behavior;
}
```

## Benefits of Dynamic Introspection

### 1. Automatic Plugin Support
- Works with any Maven plugin without hardcoding
- Handles custom enterprise plugins automatically
- Adapts to plugin configuration changes

### 2. More Accurate Analysis
- Uses actual plugin metadata instead of guessing
- Understands parameter types and requirements
- Analyzes actual configuration values

### 3. Detailed Information
- File/directory parameters and their types
- Input vs output parameters
- Plugin descriptions and requirements
- Configuration analysis

## Integration Steps

### Step 1: Add MavenPluginIntrospectionService
```java
// In NxAnalyzerMojo or service initialization
MavenPluginIntrospectionService introspectionService = 
    new MavenPluginIntrospectionService(session, lifecycleExecutor, getLog(), isVerbose());
```

### Step 2: Replace DynamicGoalAnalysisService
```java
// Replace with EnhancedDynamicGoalAnalysisService
EnhancedDynamicGoalAnalysisService enhancedService = new EnhancedDynamicGoalAnalysisService(
    session, executionPlanAnalysisService, lifecycleExecutor, getLog(), isVerbose());
```

### Step 3: Update Service Usage
```java
// Instead of:
GoalBehavior behavior = dynamicGoalAnalysisService.analyzeGoal(goal, project);

// Use:
GoalBehavior behavior = enhancedService.analyzeGoal(goal, project);

// For detailed analysis:
MavenPluginIntrospectionService.GoalIntrospectionResult detailedResult = 
    enhancedService.getIntrospectionResult(goal, project);
```

## Example Results

### Compiler Plugin Analysis
```
Goal: compile
Plugin: org.apache.maven.plugins:maven-compiler-plugin
Phase: compile
Description: Compiles application sources
Parameters:
  - compileSourceRoots (java.util.List)
  - outputDirectory (java.io.File)
  - sourceDirectory (java.io.File)
  - testSourceDirectory (java.io.File)
Requires project: true
Dependency resolution: compile
```

### Quarkus Plugin Analysis
```
Goal: dev
Plugin: io.quarkus:quarkus-maven-plugin
Phase: null (unbound goal)
Description: Quarkus development mode
Parameters:
  - sourceDir (java.io.File)
  - workingDir (java.io.File)
  - debug (boolean)
  - debugHost (java.lang.String)
Configuration:
  - source = src/main/java
  - workingDir = ${project.build.directory}
```

## Testing the Implementation

Run the test to see dynamic introspection in action:
```bash
mvn test -Dtest=MavenPluginIntrospectionServiceTest
```

This will demonstrate how the introspection service provides much more detailed information than hardcoded approaches.

## Migration Strategy

1. **Phase 1**: Add introspection service alongside existing hardcoded logic
2. **Phase 2**: Gradually replace hardcoded patterns with dynamic analysis  
3. **Phase 3**: Remove hardcoded logic and rely on introspection
4. **Phase 4**: Add additional Maven dependencies if needed for fuller PluginDescriptor access

The implementation provides a clear path forward from hardcoded plugin analysis to dynamic Maven API-based introspection.