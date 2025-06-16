# Implementing Packaging-Aware Target Generation Using Maven Session API

## Available Maven APIs

The `MavenProject` object in the Maven Session API provides all the information needed:

```java
// From Maven Session API:
MavenProject project = ...;
String packaging = project.getPackaging();  // "pom", "jar", "war", etc.
List<String> modules = project.getModules(); // Child modules (if any)
```

## Required Changes to TargetGenerationService

### 1. **Modify `generatePhaseTargets()` Method**

**Current code (TargetGenerationService.java:73-76):**
```java
String[] phases = {
    "clean", "validate", "compile", "test", "package", 
    "verify", "install", "deploy", "site"
};
```

**Updated code:**
```java
public Map<String, TargetConfiguration> generatePhaseTargets(MavenProject project, File workspaceRoot, 
                                                             Map<String, TargetConfiguration> allTargets, 
                                                             Map<String, List<String>> phaseDependencies) {
    Map<String, TargetConfiguration> phaseTargets = new LinkedHashMap<>();
    
    // Get packaging-appropriate phases
    String[] phases = getPhasesForPackaging(project.getPackaging());
    
    for (String phase : phases) {
        // ... existing target creation logic ...
    }
    
    return phaseTargets;
}

/**
 * Get appropriate lifecycle phases based on Maven packaging type
 */
private String[] getPhasesForPackaging(String packaging) {
    if ("pom".equals(packaging)) {
        // Parent POMs only have minimal lifecycle: validate, install, deploy
        return new String[]{"validate", "install", "deploy"};
    } else {
        // Regular projects have full lifecycle
        return new String[]{
            "clean", "validate", "compile", "test", "package", 
            "verify", "install", "deploy", "site"
        };
    }
}
```

### 2. **Modify Plugin Goal Generation for Parent POMs**

**Update `generatePluginGoalTargets()` method:**
```java
public Map<String, TargetConfiguration> generatePluginGoalTargets(MavenProject project, File workspaceRoot, 
                                                                  Map<String, List<String>> goalDependencies) {
    Map<String, TargetConfiguration> goalTargets = new LinkedHashMap<>();
    
    // Skip plugin goal generation for parent POMs
    if ("pom".equals(project.getPackaging())) {
        if (verbose) {
            log.debug("Skipping plugin goal targets for POM project: " + project.getArtifactId());
        }
        return goalTargets; // Return empty map
    }
    
    // ... existing plugin goal generation logic for non-POM projects ...
}
```

### 3. **Alternative: Conditional Target Generation**

**If you want to keep some goals for parent POMs:**
```java
private String[] getPhasesForPackaging(String packaging) {
    if ("pom".equals(packaging)) {
        return new String[]{"validate", "install", "deploy"};
    } else if ("war".equals(packaging)) {
        return new String[]{
            "clean", "validate", "compile", "test", "package", 
            "verify", "install", "deploy", "site"
        };
    } else if ("ear".equals(packaging)) {
        return new String[]{
            "clean", "validate", "compile", "test", "package", 
            "verify", "install", "deploy"
        };
    } else {
        // Default for JAR and other packaging types
        return new String[]{
            "clean", "validate", "compile", "test", "package", 
            "verify", "install", "deploy", "site"
        };
    }
}
```

## Implementation Steps

### Step 1: Add Helper Method

Add this method to `TargetGenerationService.java`:

```java
/**
 * Check if project is a parent POM (aggregator project)
 */
private boolean isParentPom(MavenProject project) {
    return "pom".equals(project.getPackaging());
}

/**
 * Check if project is an aggregator (has modules)
 */
private boolean isAggregator(MavenProject project) {
    return project.getModules() != null && !project.getModules().isEmpty();
}
```

### Step 2: Modify Phase Generation

**Replace lines 73-76 in `generatePhaseTargets()`:**
```java
// OLD:
String[] phases = {
    "clean", "validate", "compile", "test", "package", 
    "verify", "install", "deploy", "site"
};

// NEW:
String[] phases = getPhasesForPackaging(project.getPackaging());
```

### Step 3: Update Plugin Goal Generation

**Add packaging check at start of `generatePluginGoalTargets()`:**
```java
// Skip plugin goals for parent POMs - they don't compile/test/package
if (isParentPom(project)) {
    if (verbose) {
        log.debug("Skipping plugin goals for parent POM: " + project.getArtifactId());
    }
    return new LinkedHashMap<>();
}
```

## Result

**Before (incorrect):**
```json
{
  "core": {
    "targets": {
      "validate": {...},
      "compile": {...},    // ❌ Wrong for parent POM
      "test": {...},       // ❌ Wrong for parent POM  
      "package": {...},    // ❌ Wrong for parent POM
      "verify": {...},     // ❌ Wrong for parent POM
      "install": {...}
    }
  }
}
```

**After (correct):**
```json
{
  "core": {
    "targets": {
      "validate": {...},   // ✓ Correct
      "install": {...},    // ✓ Correct
      "deploy": {...}      // ✓ Correct
    }
  }
}
```

## Additional Considerations

### 1. **Module Building for Aggregators**

For parent POMs with modules, you might want to add special aggregator targets:

```java
if (isAggregator(project)) {
    // Add special targets that build all modules
    TargetConfiguration buildAllModules = createModuleBuildTarget(project);
    phaseTargets.put("build-modules", buildAllModules);
}
```

### 2. **Dependency Calculation Updates**

**Update `TargetDependencyService` to handle parent POMs:**
```java
public List<String> calculatePhaseDependencies(String phase, Map<String, TargetConfiguration> allTargets, 
                                               MavenProject project, List<MavenProject> reactorProjects) {
    // For parent POMs, dependencies are simpler
    if ("pom".equals(project.getPackaging())) {
        return calculateParentPomDependencies(phase, allTargets);
    } else {
        return calculateRegularProjectDependencies(phase, allTargets, project, reactorProjects);
    }
}
```

This approach uses the Maven Session API's `project.getPackaging()` method to detect parent POMs and generate appropriate targets that match Maven's actual behavior.