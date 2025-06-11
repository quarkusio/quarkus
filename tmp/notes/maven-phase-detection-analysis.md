# Maven Phase Detection Analysis

## Maven Model API Capabilities

The Maven Model API provides several ways to detect relevant phases:

### 1. Plugin Executions
- `model.getBuild().getPlugins()` - Get all plugins
- `plugin.getExecutions()` - Get executions for each plugin  
- `execution.getPhase()` - Get the phase each execution binds to
- `execution.getGoals()` - Get the goals executed in that phase

### 2. Packaging Type
- `model.getPackaging()` - Returns "jar", "war", "pom", "maven-plugin", etc.
- Different packaging types have different default phase bindings

### 3. Plugin Management
- `model.getBuild().getPluginManagement()` - Inherited plugin configs
- Can affect which phases are relevant

## Phase Detection Strategy

### Standard Phases by Packaging
- **jar**: validate, compile, test, package, verify, install, deploy
- **war**: + process-resources, process-classes (web-specific)
- **pom**: validate, install, deploy (minimal for parent POMs)
- **maven-plugin**: + plugin-specific phases

### Custom Phase Detection
1. Scan all plugin executions to find bound phases
2. Include phases that have explicit plugin bindings
3. Detect framework-specific phases (Spring Boot, Quarkus)
4. Filter out phases that don't make sense for the project type

### Framework-Specific Phases
- **Spring Boot**: spring-boot:run, spring-boot:build-image
- **Quarkus**: quarkus:dev, quarkus:build, quarkus:generate-code  
- **Surefire/Failsafe**: test, integration-test phases

## Implementation Plan

Add phase detection methods to `MavenModelReader.java`:
- `detectRelevantPhases(Model model)`
- `getPluginBoundPhases(Model model)` 
- `getPackagingSpecificPhases(String packaging)`
- `detectFrameworkPhases(Model model)`