# Phase Dependencies Deep Dive

## What Are Phase Dependencies?

Phase dependencies are like a recipe's cooking steps - each step must complete before the next can begin. In Maven, phases represent stages of the build lifecycle, and each phase depends on all previous phases completing successfully.

## Maven's Standard Lifecycle Phases

The plugin works with these core phases (from `TargetGenerationService.java:73-76`):

```java
String[] phases = {
    "clean", "validate", "compile", "test", "package", 
    "verify", "install", "deploy", "site"
};
```

Think of these like an assembly line:
- **clean**: Clear the workspace
- **validate**: Check project is valid
- **compile**: Turn source code into bytecode
- **test**: Run unit tests on compiled code
- **package**: Bundle code into JAR/WAR
- **verify**: Run integration tests
- **install**: Put artifact in local Maven repository
- **deploy**: Upload to remote repository
- **site**: Generate documentation

## How Phase Dependencies Are Calculated

### 1. **Dynamic Phase Discovery**

The system doesn't use a hardcoded list. Instead, it examines the actual Maven execution plan:

```java
// From TargetDependencyService.getPrecedingPhase()
MavenExecutionPlan executionPlan = getExecutionPlan();
List<String> allPhases = new ArrayList<>();
Set<String> seenPhases = new LinkedHashSet<>();

for (MojoExecution mojoExecution : executionPlan.getMojoExecutions()) {
    String executionPhase = mojoExecution.getLifecyclePhase();
    if (executionPhase != null && !executionPhase.isEmpty()) {
        seenPhases.add(executionPhase);  // Preserves Maven's natural order
    }
}
```

This is smart because:
- Different projects might skip phases (no tests = no test phase)
- Custom lifecycles could have different phases
- Plugin configurations can affect which phases run

### 2. **Linear Dependency Chain**

Each phase depends only on its immediate predecessor:

```java
// From TargetDependencyService.getPrecedingPhase()
int currentPhaseIndex = allPhases.indexOf(phase);
if (currentPhaseIndex > 0) {
    return allPhases.get(currentPhaseIndex - 1);  // Just the previous phase
}
```

**Example Flow:**
```
validate → compile → test → package
    ↑         ↑        ↑        ↑
    |         |        |        |
   none   validate  compile   test
```

### 3. **Three-Layer Dependency Structure**

Each phase target has three types of dependencies:

```java
// From TargetDependencyService.calculatePhaseDependencies()
public List<String> calculatePhaseDependencies(String phase, Map<String, TargetConfiguration> allTargets, 
                                               MavenProject project, List<MavenProject> reactorProjects) {
    List<String> dependsOn = new ArrayList<>();
    
    // 1. PRECEDING PHASE: Previous phase in lifecycle
    List<String> phaseDependencies = getPhaseDependencies(phase);
    dependsOn.addAll(phaseDependencies);
    
    // 2. GOALS IN THIS PHASE: All plugin goals that execute during this phase
    List<String> goalsForPhase = getGoalsForPhase(phase, allTargets);
    dependsOn.addAll(goalsForPhase);
    
    // 3. CROSS-PROJECT: Same phase in dependent projects
    dependsOn.add("^" + phase);
    
    return dependsOn;
}
```

## Real-World Example

For a typical Quarkus project, the `test` phase would depend on:

### 1. Preceding Phase
- `compile` (must compile code before testing)

### 2. Goals in This Phase  
- `maven-compiler:testCompile` (compile test sources)
- `maven-surefire:test` (run unit tests)
- `maven-failsafe:integration-test` (run integration tests, if configured)

### 3. Cross-Project Dependencies
- `^test` (run tests in all projects this one depends on)

**Final dependency list for `test`:**
```json
{
  "test": {
    "dependsOn": [
      "compile",                    // Preceding phase
      "maven-compiler:testCompile", // Goals in test phase  
      "maven-surefire:test",        // Goals in test phase
      "^test"                       // Cross-project
    ]
  }
}
```

## Why This Approach Works

### 1. **Phases Are Orchestrators**
Phase targets don't actually execute Maven commands - they're "nx:noop" targets that just coordinate dependencies:

```java
// From TargetGenerationService.generatePhaseTargets()
TargetConfiguration target = new TargetConfiguration("nx:noop");
target.setOptions(new LinkedHashMap<>());  // No actual command
target.setInputs(new ArrayList<>());       // No inputs
target.setOutputs(new ArrayList<>());      // No outputs
```

Think of phases like a project manager - they don't do the work, they just make sure everything happens in the right order.

### 2. **Goals Do the Real Work**
The actual Maven commands are in goal targets:

```java
// From TargetGenerationService.createGoalTarget()
TargetConfiguration target = new TargetConfiguration("nx:run-commands");
options.put("command", "mvn " + pluginKey + ":" + goal);  // Real Maven command
```

### 3. **Smart Ordering**
When you run `nx test my-project`, Nx automatically runs:

1. **Dependency resolution**: Finds all dependencies of `test` phase
2. **Topological sort**: Orders tasks correctly across all projects
3. **Parallel execution**: Runs independent tasks simultaneously
4. **Caching**: Skips unchanged tasks

## Benefits Over Plain Maven

### Maven Sequential Execution
```bash
mvn test
# Runs: validate → compile → test-compile → test (all in sequence)
```

### Nx Smart Execution  
```bash
nx test my-project
# Runs only: 
# - ^test (tests in dependencies, in parallel if possible)
# - compile (if source changed)
# - maven-compiler:testCompile (if test source changed) 
# - maven-surefire:test (if tests or code changed)
```

This is like having a smart assistant who knows exactly what needs to be done and in what order, rather than following a rigid checklist every time.

## Edge Cases Handled

### 1. Missing Phases
If a project doesn't have tests, the `test` phase still exists but has no goals:
```json
{
  "test": {
    "dependsOn": ["compile", "^test"]  // No test goals added
  }
}
```

### 2. Custom Phases
If plugins add custom phases, they're automatically discovered from the execution plan.

### 3. Phase Inference Failure
If the execution plan can't be determined, goals fall back to hardcoded phase mappings:

```java
// From TargetDependencyService.mapGoalToPhase()
switch (goal) {
    case "compile": return "compile";
    case "test": return "test";
    case "jar": return "package";
    // ... etc
}
```

This multi-layered approach ensures the system works reliably even when Maven configuration is complex or non-standard.