# Maven API Analysis for Project Inputs and File Dependencies

## Current Implementation Analysis

### Current Hardcoded Patterns in TargetGenerationService.java

**Lines 173-177:**
```java
List<String> inputs = new ArrayList<>();
inputs.add(projectRootToken + "/pom.xml");
if (isSourceProcessingGoal(goal)) {
    inputs.add(projectRootToken + "/src/**/*");
}
```

**Lines 229-234:**
```java
List<String> inputs = new ArrayList<>();
inputs.add(projectRootToken + "/pom.xml");
if (isSourceProcessingGoal(goal)) {
    inputs.add(projectRootToken + "/src/**/*");
}
```

**Problems with current approach:**
1. Uses hardcoded `/src/**/*` pattern regardless of actual Maven configuration
2. Doesn't account for configured source directories that differ from defaults
3. Ignores resource directories entirely 
4. Doesn't leverage Maven's Build API to get actual configured paths

### Available Maven APIs for Source/Resource Discovery

#### From MavenProject API:
```java
// Source directories
project.getCompileSourceRoots()     // List<String> of source roots
project.getTestCompileSourceRoots() // List<String> of test source roots

// Build configuration access
project.getBuild().getOutputDirectory()      // Compiled classes output
project.getBuild().getTestOutputDirectory()  // Test classes output  
project.getBuild().getDirectory()           // Build directory (usually target/)
project.getBuild().getSourceDirectory()     // Main source dir (default: src/main/java)
project.getBuild().getTestSourceDirectory() // Test source dir (default: src/test/java)

// Resource directories
project.getBuild().getResources()           // List<Resource> main resources
project.getBuild().getTestResources()       // List<Resource> test resources
```

#### From Resource API:
```java
Resource resource = ...;
resource.getDirectory()    // String path to resource directory
resource.getTargetPath()   // String target path in output
resource.getIncludes()     // List<String> include patterns
resource.getExcludes()     // List<String> exclude patterns
resource.isFiltering()     // boolean if filtering enabled
```

### Real-World Usage Examples Found

#### Example 1: LocalProject.java (Quarkus Bootstrap)
```java
public PathCollection getResourcesSourcesDirs() {
    final List<Resource> resources = rawModel.getBuild() == null ? List.of()
            : rawModel.getBuild().getResources();
    if (resources.isEmpty()) {
        return PathList.of(resolveRelativeToBaseDir(null, SRC_MAIN_RESOURCES));
    }
    return PathList.from(resources.stream()
            .map(Resource::getDirectory)
            .map(resourcesDir -> resolveRelativeToBaseDir(resourcesDir, SRC_MAIN_RESOURCES))
            .collect(Collectors.toCollection(LinkedHashSet::new)));
}

public PathCollection getTestResourcesSourcesDirs() {
    final List<Resource> resources = rawModel.getBuild() == null ? List.of()
            : rawModel.getBuild().getTestResources();
    if (resources.isEmpty()) {
        return PathList.of(resolveRelativeToBaseDir(null, SRC_TEST_RESOURCES));
    }
    return PathList.from(resources.stream()
            .map(Resource::getDirectory)
            .map(resourcesDir -> resolveRelativeToBaseDir(resourcesDir, SRC_TEST_RESOURCES))
            .collect(Collectors.toCollection(LinkedHashSet::new)));
}
```

#### Example 2: DevMojo.java (Quarkus Maven Plugin)
```java
// Getting resources from project
for (Resource resource : project.getResources()) {
    String dir = resource.getDirectory();
    Path path = Paths.get(dir);
    resourceDirs.add(path);
}

// Getting resources from profiles
build.getResources().stream()
    .map(Resource::getDirectory)
    .map(Path::of)
    .map(Path::toAbsolutePath)
    .collect(Collectors.toList())
```

## Recommendations for Maven Plugin Enhancement

### 1. Replace Hardcoded Patterns with Maven API Calls

**Current problem:**
```java
if (isSourceProcessingGoal(goal)) {
    inputs.add(projectRootToken + "/src/**/*");
}
```

**Recommended solution:**
```java
private List<String> getProjectInputs(MavenProject project, String goal, String projectRootToken) {
    List<String> inputs = new ArrayList<>();
    
    // Always include pom.xml
    inputs.add(projectRootToken + "/pom.xml");
    
    if (isSourceProcessingGoal(goal)) {
        // Add actual configured source directories
        addSourceDirectories(project, projectRootToken, inputs);
        addResourceDirectories(project, projectRootToken, inputs);
    }
    
    return inputs;
}

private void addSourceDirectories(MavenProject project, String projectRootToken, List<String> inputs) {
    // Main source directories
    for (String sourceRoot : project.getCompileSourceRoots()) {
        String relativePath = getRelativePathFromProject(project, sourceRoot);
        inputs.add(projectRootToken + "/" + relativePath + "/**/*");
    }
    
    // Test source directories 
    for (String testSourceRoot : project.getTestCompileSourceRoots()) {
        String relativePath = getRelativePathFromProject(project, testSourceRoot);
        inputs.add(projectRootToken + "/" + relativePath + "/**/*");
    }
}

private void addResourceDirectories(MavenProject project, String projectRootToken, List<String> inputs) {
    // Main resources
    if (project.getBuild() != null && project.getBuild().getResources() != null) {
        for (Resource resource : project.getBuild().getResources()) {
            String resourceDir = resource.getDirectory();
            if (resourceDir != null) {
                String relativePath = getRelativePathFromProject(project, resourceDir);
                inputs.add(projectRootToken + "/" + relativePath + "/**/*");
            }
        }
    }
    
    // Test resources
    if (project.getBuild() != null && project.getBuild().getTestResources() != null) {
        for (Resource resource : project.getBuild().getTestResources()) {
            String resourceDir = resource.getDirectory();
            if (resourceDir != null) {
                String relativePath = getRelativePathFromProject(project, resourceDir);
                inputs.add(projectRootToken + "/" + relativePath + "/**/*");
            }
        }
    }
}
```

### 2. Enhanced Output Detection

**Current limitation:** ExecutionPlanAnalysisService.getGoalOutputs() returns empty list

**Recommended enhancement:**
```java
public List<String> getGoalOutputs(String goal, String projectRootToken, MavenProject project) {
    List<String> outputs = new ArrayList<>();
    
    if (project.getBuild() != null) {
        // Add output directories based on goal type
        if (isCompileGoal(goal)) {
            String outputDir = project.getBuild().getOutputDirectory();
            String relativePath = getRelativePathFromProject(project, outputDir);
            outputs.add(projectRootToken + "/" + relativePath);
        }
        
        if (isTestGoal(goal)) {
            String testOutputDir = project.getBuild().getTestOutputDirectory();
            String relativePath = getRelativePathFromProject(project, testOutputDir);
            outputs.add(projectRootToken + "/" + relativePath);
        }
        
        // Add other goal-specific outputs based on plugin configuration
        addPluginSpecificOutputs(goal, project, projectRootToken, outputs);
    }
    
    return outputs;
}
```

### 3. Plugin-Specific Input Detection

**Enhanced goal detection based on Maven plugin configuration:**
```java
private boolean isSourceProcessingGoal(String goal, MavenProject project) {
    // Check if goal actually processes sources by looking at plugin configuration
    // This replaces the current hardcoded string matching approach
    
    // Standard Maven lifecycle goals that process sources
    if (Arrays.asList("compile", "testCompile", "test", "integration-test").contains(goal)) {
        return true;
    }
    
    // Check plugin-specific configuration
    return isPluginConfiguredForSources(goal, project);
}
```

## Benefits of Using Maven APIs

1. **Accuracy**: Reflects actual Maven project configuration instead of assumptions
2. **Flexibility**: Handles non-standard source/resource directory layouts
3. **Completeness**: Includes all configured source roots and resource directories
4. **Future-proof**: Adapts to Maven configuration changes automatically
5. **Plugin compatibility**: Works with projects using custom source/resource configurations

## Implementation Priority

1. **High Priority**: Replace hardcoded `/src/**/*` with actual source root detection
2. **High Priority**: Add resource directory detection for proper input tracking  
3. **Medium Priority**: Enhance output directory detection beyond hardcoded patterns
4. **Medium Priority**: Add plugin-specific input/output detection based on actual configuration
5. **Low Priority**: Support for filtered resources and complex resource configurations

## File Locations to Modify

- `/home/jason/projects/triage/java/quarkus2/maven-plugin/src/main/java/TargetGenerationService.java` (lines 173-177, 229-234)
- `/home/jason/projects/triage/java/quarkus2/maven-plugin/src/main/java/ExecutionPlanAnalysisService.java` (lines 356-359)