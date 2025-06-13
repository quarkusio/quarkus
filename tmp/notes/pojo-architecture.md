# POJO-Based Architecture for Maven Nx Integration

## Overview
Replaced generic Maps and Objects with strongly-typed POJO classes for better type safety and maintainability.

## POJO Classes Created

### Model Package (`model/`)

#### Core Maven Models
- **`MavenDependency`**: Represents a Maven dependency with groupId, artifactId, version, scope, type, optional, resolved flags
- **`MavenPlugin`**: Represents a Maven plugin with executions
- **`MavenPluginExecution`**: Represents a plugin execution with id, phase, goals

#### Project Information
- **`ProjectInfo`**: Contains all Maven project information including dependencies, plugins, targets, and metadata
- **`TargetConfiguration`**: Represents an Nx target with executor, options, inputs, outputs, dependsOn, metadata
- **`TargetMetadata`**: Target metadata including type, phase, plugin, goal, technologies, description

#### Nx Integration Models
- **`ProjectConfiguration`**: Nx project configuration with root, sourceRoot, projectType, targets, metadata
- **`ProjectMetadata`**: Project metadata for Nx (groupId, artifactId, version, packaging)
- **`CreateNodesResult`**: Result of CreateNodesV2 function containing projects map
- **`CreateNodesV2Entry`**: Single entry in CreateNodesV2 results array [pomFilePath, CreateNodesResult]
- **`RawProjectGraphDependency`**: Project graph dependency with source, target, type (enum), sourceFile
- **`NxAnalysisResult`**: Top-level result containing both createNodesResults and createDependencies

## Updated Classes

### Generator Classes
- **`CreateNodesResultGenerator`**: Now uses POJOs, returns `List<CreateNodesV2Entry>`
- **`CreateDependenciesGenerator`**: Now uses POJOs, returns `List<RawProjectGraphDependency>`

### Utility Classes  
- **`NxPathUtils`**: Updated to work with `ProjectInfo` objects instead of generic Maps

### Main Class
- **`NxAnalyzerMojo`**: Will be updated to use POJOs throughout the analysis pipeline

## Benefits

1. **Type Safety**: Compile-time checking instead of runtime errors from Map key typos
2. **IDE Support**: Better autocomplete, refactoring, and navigation
3. **Maintainability**: Clear contracts and easier to understand code structure
4. **Validation**: Can add validation logic in POJO setters/constructors
5. **Documentation**: Self-documenting code with clear field types and purposes

## Key Patterns

### Enum for Dependency Types
```java
public enum DependencyType {
    STATIC("static"),
    DYNAMIC("dynamic"), 
    IMPLICIT("implicit");
}
```

### Builder Pattern Support
POJOs have constructors and fluent setters for easy object creation.

### Coordinate Methods
Helper methods like `getCoordinate()` for common operations (groupId:artifactId).

### Path Utilities
Centralized path operations in `NxPathUtils` working with typed objects.

## Next Steps

1. Update `NxAnalyzerMojo` to use POJOs in analysis pipeline
2. Add validation logic to POJO setters where appropriate  
3. Consider adding Builder pattern for complex object construction
4. Add unit tests for POJO classes and generators