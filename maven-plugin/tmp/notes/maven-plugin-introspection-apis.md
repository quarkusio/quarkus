# Maven Plugin Introspection APIs

## Research Summary

Based on analysis of the current codebase and Maven's API capabilities, here are the findings for dynamically determining plugin and goal behavior instead of hardcoded checks.

## Current State

The `DynamicGoalAnalysisService` currently uses hardcoded plugin-specific logic (lines 108-153) to determine goal behavior. This approach has several limitations:
- Must be updated for every new plugin
- Cannot handle custom plugins or unknown plugins well
- Doesn't leverage Maven's built-in metadata

## Available Maven APIs

### 1. MojoExecution API (Currently Used)
The codebase already accesses `MojoExecution` objects through `ExecutionPlanAnalysisService`:
- `mojoExecution.getGoal()` - Get goal name
- `mojoExecution.getLifecyclePhase()` - Get lifecycle phase
- `mojoExecution.getPlugin()` - Get plugin information
- `mojoExecution.getExecutionId()` - Get execution ID

### 2. Plugin and Mojo Descriptors (Not Currently Used)
Maven provides `PluginDescriptor` and `MojoDescriptor` APIs for introspection, but these require additional dependencies and plugin manager access.

### 3. Parameter Analysis via MojoExecution
The `MojoExecution` object contains configuration that can be analyzed for file/directory requirements.

## Recommended Enhancements

### Phase 1: Enhance MojoExecution Analysis
Leverage existing `MojoExecution` data more effectively without adding new dependencies.

### Phase 2: Add Plugin Manager Integration
Add `PluginManager` component to access `PluginDescriptor` and `MojoDescriptor`.

### Phase 3: Parameter Introspection
Use `@Parameter` annotations and configuration to understand file/directory requirements.

## Implementation Strategy

Focus on enhancing the existing `MojoExecution` analysis in `DynamicGoalAnalysisService.analyzeMojoExecution()` method which currently returns empty behavior.

## Key Maven APIs Discovered

### 1. MojoExecution (Already Available)
- `mojoExecution.getGoal()` - Goal name
- `mojoExecution.getLifecyclePhase()` - Lifecycle phase
- `mojoExecution.getPlugin()` - Plugin information
- `mojoExecution.getMojoDescriptor()` - Mojo metadata
- `mojoExecution.getConfiguration()` - Plugin configuration XML

### 2. MojoDescriptor (Available via MojoExecution)
- `descriptor.getDescription()` - Mojo description
- `descriptor.isProjectRequired()` - Whether mojo requires project
- `descriptor.getDependencyResolutionRequired()` - Dependency resolution scope
- `descriptor.getParameters()` - List of mojo parameters

### 3. Parameter (Available via MojoDescriptor)
- `parameter.getName()` - Parameter name
- `parameter.getType()` - Parameter type (e.g., "java.io.File")
- `parameter.getDescription()` - Parameter description
- Used to identify file/directory parameters dynamically

### 4. Xpp3Dom Configuration (Available via MojoExecution)
- `configuration.getName()` - Configuration element name
- `configuration.getValue()` - Configuration value
- `configuration.getChildren()` - Child configuration elements
- Used to analyze actual plugin configuration

## Files Created

1. **MavenPluginIntrospectionService.java** - Core introspection service using Maven APIs
2. **EnhancedDynamicGoalAnalysisService.java** - Enhanced analysis service integrating introspection
3. **MavenPluginIntrospectionServiceTest.java** - Test demonstrating dynamic plugin analysis
4. **Integration guide** - Comprehensive guide for replacing hardcoded logic

## Key Benefits

- **Automatic Plugin Support**: Works with any Maven plugin without hardcoding
- **Parameter Analysis**: Understands file/directory parameters through type inspection
- **Configuration Analysis**: Analyzes actual plugin configuration values
- **Rich Metadata**: Provides plugin descriptions, requirements, and detailed parameter info
- **Framework Detection**: Enhanced heuristics for Quarkus, Spring Boot, etc.

## Next Steps

1. Integrate `MavenPluginIntrospectionService` into existing `DynamicGoalAnalysisService`
2. Replace hardcoded plugin checks with dynamic parameter analysis
3. Test with various Maven plugins to validate behavior detection
4. Consider adding additional Maven dependencies for even richer PluginDescriptor access