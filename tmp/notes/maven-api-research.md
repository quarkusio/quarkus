# Maven API Research for Dynamic Goal Analysis

## Current Hardcoded Logic to Replace

The current code in `TargetGenerationService.java` uses these hardcoded methods:
- `isSourceProcessingGoal()` - checks if goal contains "compile", "test", "build", "dev", "run", "resources"
- `isTestGoal()` - checks if goal contains "test" or specific test-related goals
- `needsResources()` - checks if goal needs resource directories

## Maven APIs Available for Dynamic Analysis

### 1. MojoExecution API
Located in `ExecutionPlanAnalysisService.java`, lines 273-304:
```java
for (MojoExecution mojoExecution : executionPlan.getMojoExecutions()) {
    String goal = mojoExecution.getGoal();
    String phase = mojoExecution.getLifecyclePhase();
    String pluginArtifactId = mojoExecution.getPlugin().getArtifactId();
    // ... access to execution configuration
}
```

**Available MojoExecution methods:**
- `getGoal()` - goal name
- `getLifecyclePhase()` - which phase this goal binds to
- `getPlugin()` - access to plugin information
- `getExecutionId()` - execution ID
- `getConfiguration()` - XML configuration for this execution

### 2. PluginDescriptor and MojoDescriptor APIs
Found in `QuarkusMavenPluginDocsGenerator.java`:
```java
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;

// Usage:
for (MojoDescriptor mojo : pluginDescriptor.getMojos()) {
    mojo.getFullGoalName();
    mojo.getDescription();
    mojo.getParameters(); // List<Parameter>
}

for (Parameter parameter : mojo.getParameters()) {
    parameter.getName();
    parameter.getType();
    parameter.getExpression();
    parameter.getDefaultValue();
    parameter.isRequired();
}
```

### 3. Lifecycle Phase Information
Already being used in `ExecutionPlanAnalysisService.java`:
```java
// Get lifecycle for specific phase
org.apache.maven.lifecycle.Lifecycle getLifecycleForPhase(String phase)

// Get all phases from lifecycles
Set<String> getAllLifecyclePhases()
```

## Potential Dynamic Analysis Approaches

### Approach 1: Use MojoExecution.getConfiguration()
Extract configuration to determine:
- Source directories from compiler plugins
- Test patterns from surefire/failsafe plugins  
- Resource processing from resources plugin

### Approach 2: Load PluginDescriptor at Runtime
Get plugin descriptors to analyze:
- Parameter types and defaults
- Goal requirements and capabilities
- Plugin metadata

### Approach 3: Lifecycle Phase Analysis
Use phase binding to infer goal behavior:
- Goals bound to compile phases likely process sources
- Goals bound to test phases likely run tests
- Goals bound to process-resources likely need resources

### Approach 4: Plugin Artifact Analysis
Analyze plugin artifact ID and group ID:
- org.apache.maven.plugins:maven-compiler-plugin -> source processing
- org.apache.maven.plugins:maven-surefire-plugin -> test execution
- org.apache.maven.plugins:maven-resources-plugin -> resource processing

## Recommended Implementation Strategy

1. **Extract MojoExecution configuration** to get actual directories used
2. **Use lifecycle phase binding** to categorize goal types
3. **Fallback to plugin artifact analysis** for unknown plugins
4. **Cache plugin descriptors** for performance

This would replace hardcoded patterns with actual Maven project analysis.