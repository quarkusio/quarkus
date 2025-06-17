# Maven API-Based Input Detection Implementation

## Current Problem
The current implementation in `TargetGenerationService.java` uses hardcoded patterns:
```java
inputs.add(projectRootToken + "/src/**/*"); // Lines 175, 232
```

This ignores Maven's actual configuration and misses important directories.

## Available Maven APIs

### From MavenProject
```java
// Source directories
project.getCompileSourceRoots()     // List<String> of main source dirs
project.getTestCompileSourceRoots() // List<String> of test source dirs

// Resource directories  
project.getBuild().getResources()     // List<Resource> main resources
project.getBuild().getTestResources() // List<Resource> test resources

// Output directories
project.getBuild().getOutputDirectory()     // Compiled classes output
project.getBuild().getTestOutputDirectory() // Test classes output
```

### Resource Object Methods
```java
Resource resource = ...;
resource.getDirectory()  // String path to resource directory
resource.getIncludes()   // List<String> include patterns
resource.getExcludes()   // List<String> exclude patterns
```

## Recommended Implementation

### New Method: `getSmartInputsForGoal()`
```java
private List<String> getSmartInputsForGoal(String goal, MavenProject project, String projectRootToken) {
    List<String> inputs = new ArrayList<>();
    
    // Always include POM
    inputs.add(projectRootToken + "/pom.xml");
    
    if (isSourceProcessingGoal(goal)) {
        // Add actual source directories from Maven configuration
        for (String sourceRoot : project.getCompileSourceRoots()) {
            String relativePath = getRelativePathFromProject(sourceRoot, project);
            inputs.add(projectRootToken + "/" + relativePath + "/**/*");
        }
        
        if (isTestGoal(goal)) {
            for (String testSourceRoot : project.getTestCompileSourceRoots()) {
                String relativePath = getRelativePathFromProject(testSourceRoot, project);
                inputs.add(projectRootToken + "/" + relativePath + "/**/*");
            }
        }
        
        // Add resource directories
        if (needsResources(goal)) {
            for (Resource resource : project.getBuild().getResources()) {
                String relativePath = getRelativePathFromProject(resource.getDirectory(), project);
                inputs.add(projectRootToken + "/" + relativePath + "/**/*");
            }
            
            if (isTestGoal(goal)) {
                for (Resource resource : project.getBuild().getTestResources()) {
                    String relativePath = getRelativePathFromProject(resource.getDirectory(), project);
                    inputs.add(projectRootToken + "/" + relativePath + "/**/*");
                }
            }
        }
    }
    
    return inputs;
}
```

### Helper Methods Needed
```java
private boolean isTestGoal(String goal) {
    return goal.contains("test") || goal.equals("testCompile") || goal.equals("testResources");
}

private boolean needsResources(String goal) {
    return goal.contains("compile") || goal.contains("resources") || 
           goal.equals("dev") || goal.equals("build");
}

private String getRelativePathFromProject(String absolutePath, MavenProject project) {
    Path projectBase = project.getBasedir().toPath();
    Path targetPath = Paths.get(absolutePath);
    
    if (targetPath.isAbsolute()) {
        return projectBase.relativize(targetPath).toString();
    }
    return absolutePath; // Already relative
}
```

## Key Benefits

1. **No Hardcoded Paths**: Uses actual Maven configuration
2. **Respects Custom Directories**: Works with non-standard layouts
3. **Accurate Resource Detection**: Includes configured resource directories
4. **Test vs Main Separation**: Different inputs for different goal types
5. **Plugin Configuration Aware**: Considers actual Maven setup

## Migration Plan

1. Add new `getSmartInputsForGoal()` method
2. Replace hardcoded `/src/**/*` calls with API-based detection
3. Add tests to verify correct input detection for various project layouts
4. Remove `isSourceProcessingGoal()` hardcoded list in favor of goal analysis

This ensures the Nx plugin accurately reflects Maven's understanding of the project structure.