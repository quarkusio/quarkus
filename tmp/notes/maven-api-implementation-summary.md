# Maven API-Based Input Detection Implementation - Summary

## What Was Changed

### Before (Hardcoded Approach)
```java
// Lines 175 & 232 in TargetGenerationService.java
List<String> inputs = new ArrayList<>();
inputs.add(projectRootToken + "/pom.xml");
if (isSourceProcessingGoal(goal)) {
    inputs.add(projectRootToken + "/src/**/*");  // HARDCODED!
}
```

### After (Maven API Approach)
```java
// New implementation using Maven APIs
List<String> inputs = getSmartInputsForGoal(goal, project, projectRootToken);
```

## New Method: `getSmartInputsForGoal()`

### Key Features
1. **Uses Maven APIs**:
   - `project.getCompileSourceRoots()` - Actual main source directories
   - `project.getTestCompileSourceRoots()` - Actual test source directories
   - `project.getBuild().getResources()` - Configured resource directories
   - `project.getBuild().getTestResources()` - Configured test resource directories

2. **Smart Goal Classification**:
   - `isSourceProcessingGoal()` - Determines if goal needs source files
   - `isTestGoal()` - Determines if goal is test-related
   - `needsResources()` - Determines if goal needs resource files

3. **Path Handling**:
   - `getRelativePathFromProject()` - Converts absolute/relative paths properly
   - Handles cross-platform path separators
   - Skips paths outside project directory

## Benefits

### 1. No More Hardcoded Patterns
- Eliminates `/src/**/*` assumptions
- Uses actual Maven configuration
- Works with any project layout

### 2. Respects Maven Configuration
- Custom source directories (`<sourceDirectory>`)
- Custom test directories (`<testSourceDirectory>`)
- Configured resource directories with includes/excludes
- Multi-module project structures

### 3. Goal-Specific Intelligence
- Different inputs for compilation vs. testing vs. packaging
- Resource directories only included when needed
- Test sources only for test-related goals

### 4. Better Accuracy
- Only includes directories that actually exist
- Handles both absolute and relative paths
- Excludes paths outside the project

## Implementation Details

### Added Imports
```java
import org.apache.maven.model.Resource;
import java.nio.file.Path;
import java.nio.file.Paths;
```

### New Helper Methods
- `getSmartInputsForGoal()` - Main input detection logic
- `isTestGoal()` - Test goal detection
- `needsResources()` - Resource requirement detection
- `getRelativePathFromProject()` - Path normalization

### Enhanced Goal Classification
- Extended `isSourceProcessingGoal()` to include `resources` goals
- Added test-specific goal detection
- Added resource-specific goal detection

## Testing Results

The implementation compiles successfully and maintains the same behavior for standard Maven layouts while enabling support for:
- Custom source directories
- Non-standard project layouts
- Complex resource configurations
- Multi-module projects with varying structures

## Future Benefits

This change enables the Maven plugin to work correctly with:
- Generated source directories
- Filtered resources
- Custom Maven configurations
- Non-Java projects using Maven
- Projects with complex source structures

The plugin now accurately reflects Maven's understanding of the project structure rather than making assumptions.