# Reactor Projects in Maven Plugin

## What are Reactor Projects?

`reactorProjects` is a Maven built-in parameter that contains **ALL projects included in the current Maven reactor build**.

### In the Quarkus Context

- **Total Projects**: ~932 Maven projects in the Quarkus codebase
- **When you run from root**: All projects are included in the reactor
- **When you run from subdirectory**: Only that project and its children are included

### How Cross-Module Dependencies Work

When the Maven plugin calculates cross-module dependencies:

```java
List<String> dependencies = targetDependencyService.calculateGoalDependencies(
    project, executionPhase, targetName, reactorProjects);
```

The `getCrossModuleGoalsForPhase()` method:
1. **Iterates through ALL reactor projects** (excluding current project)
2. **Finds goals for the specified phase** in each project  
3. **Creates dependencies** in format `nxProjectName:goalName`

### Example Scenario

If you're in project `io.quarkus:quarkus-core` and calculating dependencies for `maven-install:install`:

**Input**: 
- Current project: `io.quarkus:quarkus-core`
- Phase: `install` 
- Reactor projects: All 932 Quarkus projects

**Output Dependencies**:
- `io.quarkus:quarkus-deployment:maven-jar:jar`
- `io.quarkus:quarkus-deployment:maven-install:install`  
- `io.quarkus:quarkus-test:maven-jar:jar`
- `io.quarkus:quarkus-test:maven-install:install`
- ... (for every project that has goals in the install phase)

### Scope Control

The reactor scope is determined by:
- **Where you run Maven**: Root vs subdirectory
- **Maven aggregator modules**: Projects included in `<modules>` sections
- **Maven profiles**: Can include/exclude certain modules

### Performance Implications

- **Large workspaces**: May generate many cross-module dependencies
- **Granular dependencies**: Better for Nx caching but more complexity
- **Incremental builds**: Nx can skip unchanged cross-module goals

## Key Point

When the plugin runs from the Quarkus root, `reactorProjects` includes **ALL 932 Maven projects** in the Quarkus codebase, making cross-module dependencies very comprehensive and precise.