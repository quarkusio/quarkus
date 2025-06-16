# Target Dependencies Current Status

## How Target Dependencies Work in the Maven Plugin

Target dependencies in your Maven plugin work through a well-structured service layer that handles three types of dependencies:

### 1. **Service Architecture**

The system uses dedicated services:
- `TargetDependencyService.java` - Calculates all dependency relationships
- `TargetGenerationService.java` - Creates actual target configurations
- `TargetGroupService.java` - Groups related targets together

### 2. **Dependency Types**

#### A. **Phase Dependencies** (Maven Lifecycle)
```java
// From TargetDependencyService.calculatePhaseDependencies()
public List<String> calculatePhaseDependencies(String phase, ...) {
    // Add dependency on preceding phase
    List<String> phaseDependencies = getPhaseDependencies(phase);
    
    // Add dependencies on all goals that belong to this phase
    List<String> goalsForPhase = getGoalsForPhase(phase, allTargets);
    
    // Add cross-module dependencies using Nx ^ syntax
    dependsOn.add("^" + phase);
}
```

**Example**: `test` phase depends on:
- `compile` (preceding phase)
- `maven-compiler:testCompile` (goals in this phase)
- `^test` (same phase in dependent projects)

#### B. **Goal Dependencies** (Plugin-specific)
```java
// From TargetDependencyService.calculateGoalDependencies()
public List<String> calculateGoalDependencies(MavenProject project, String executionPhase, 
                                              String targetName, ...) {
    // Get the phase this goal runs in
    String effectivePhase = executionPhase != null ? executionPhase : inferPhaseFromGoal(goal);
    
    // Add dependency on preceding phase
    String precedingPhase = getPrecedingPhase(effectivePhase);
    if (precedingPhase != null) {
        dependsOn.add(precedingPhase);
    }
    
    // ALWAYS add cross-module dependencies using ^ syntax
    dependsOn.add("^" + effectivePhase);
}
```

**Example**: `quarkus:dev` goal depends on:
- `compile` (preceding phase of its execution phase)
- `^compile` (compile phase in dependent projects)

#### C. **Cross-Project Dependencies** (Multi-module)
Uses Nx's `^` syntax to reference the same target in dependent projects:
- `^compile` = run compile in all projects this project depends on
- `^test` = run tests in all projects this project depends on

### 3. **Smart Phase Inference**

The system automatically determines what phase a goal belongs to:

```java
// Method 1: Check execution configuration
String executionPhase = execution.getPhase();

// Method 2: Analyze Maven execution plan
public String inferPhaseFromGoal(String goal) {
    MavenExecutionPlan executionPlan = getExecutionPlan();
    for (MojoExecution mojoExecution : executionPlan.getMojoExecutions()) {
        if (goal.equals(mojoExecution.getGoal())) {
            return mojoExecution.getLifecyclePhase();
        }
    }
}

// Method 3: Fallback mapping
private String mapGoalToPhase(String goal) {
    switch (goal) {
        case "compile": return "compile";
        case "test": return "test";
        case "jar": return "package";
        // ... etc
    }
}
```

### 4. **Target Generation Process**

1. **Plugin Detection**: Scans `pom.xml` for plugins and their configured executions
2. **Goal Discovery**: Finds all goals from executions + common goals for known plugins
3. **Dependency Calculation**: Uses `TargetDependencyService` to calculate dependencies
4. **Target Creation**: Uses `TargetGenerationService` to create target configurations

### 5. **Real Example Structure**

For a typical Maven project, you get targets like:

```json
{
  "compile": {
    "executor": "nx:noop",
    "dependsOn": ["validate"],
    "metadata": { "type": "phase" }
  },
  "maven-compiler:compile": {
    "executor": "nx:run-commands", 
    "options": { "command": "mvn org.apache.maven.plugins:maven-compiler-plugin:compile" },
    "dependsOn": ["validate", "^compile"],
    "metadata": { "type": "goal", "phase": "compile" }
  },
  "test": {
    "executor": "nx:noop",
    "dependsOn": ["compile", "maven-compiler:testCompile", "maven-surefire:test"],
    "metadata": { "type": "phase" }
  },
  "quarkus:dev": {
    "executor": "nx:run-commands",
    "options": { "command": "mvn io.quarkus:quarkus-maven-plugin:dev" },
    "dependsOn": ["compile", "^compile"], 
    "metadata": { "type": "goal", "phase": "compile" }
  }
}
```

### 6. **Benefits of This Approach**

- **Automatic ordering**: Nx runs tasks in correct dependency order
- **Incremental builds**: Only runs what's needed based on changes
- **Parallel execution**: Independent tasks run simultaneously
- **Cross-project consistency**: `^` syntax ensures dependencies are built first
- **Framework awareness**: Detects Quarkus, Spring Boot, etc. automatically

### 7. **Current Issues**

Based on the error output, the Java Maven analyzer seems to have compilation or execution issues, but the dependency logic itself is well-designed and should work correctly once the analyzer is properly compiled and accessible.