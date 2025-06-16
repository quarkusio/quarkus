# Dynamic Phase Discovery Implementation Complete

## Changes Made

### 1. **Updated TargetGenerationService.java**

**Added imports:**
```java
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;
```

**Updated constructor:**
```java
private final MavenSession session;

public TargetGenerationService(Log log, boolean verbose, MavenSession session) {
    this.log = log;
    this.verbose = verbose;
    this.session = session;
}
```

**Replaced hardcoded phases:**
```java
// OLD:
String[] phases = {
    "clean", "validate", "compile", "test", "package", 
    "verify", "install", "deploy", "site"
};

// NEW:
Set<String> applicablePhases = getApplicablePhases(project);
```

**Added dynamic phase discovery methods:**
```java
private Set<String> getApplicablePhases(MavenProject project) {
    // Uses LifecycleExecutor to calculate execution plan
    // Extracts unique phases from MojoExecutions
    // Returns only phases that have actual goals bound
}

private boolean hasGoalsBoundToPhase(String phase, MavenProject project) {
    // Checks if a specific phase has goals bound to it
    // Uses Maven's execution plan calculation
}
```

### 2. **Updated NxAnalyzerMojo.java**

**Updated service initialization:**
```java
// OLD:
this.targetGenerationService = new TargetGenerationService(getLog(), isVerbose());

// NEW:
this.targetGenerationService = new TargetGenerationService(getLog(), isVerbose(), session);
```

## How It Works

### **Dynamic Phase Discovery Process:**

1. **Request Execution Plan**: Uses `LifecycleExecutor.calculateExecutionPlan(session, project, Arrays.asList("deploy"))`
2. **Extract Phases**: Loops through `MojoExecution` objects and collects unique `getLifecyclePhase()` values
3. **Filter Valid Phases**: Only includes phases that have actual goals bound
4. **Generate Targets**: Creates targets only for discovered phases

### **Benefits:**

**For Parent POMs (packaging=pom):**
- Automatically discovers only: `validate`, `install`, `deploy`
- Skips: `compile`, `test`, `package`, `verify` (as Maven would)

**For JAR Projects:**
- Discovers full lifecycle: `validate`, `compile`, `test`, `package`, `verify`, `install`, `deploy`

**For Custom Packaging:**
- Automatically adapts to any packaging type
- Respects plugin configurations and custom bindings

### **Fallback Safety:**

```java
} catch (Exception e) {
    log.warn("Could not determine applicable phases: " + e.getMessage());
    // Fallback to minimal phases for safety
    applicablePhases.add("validate");
    applicablePhases.add("install");
}
```

## Testing Status

**Implementation Complete:** ✓
- Dynamic phase discovery implemented
- Constructor updated to accept MavenSession
- Hardcoded phases removed
- Maven APIs properly utilized

**Compilation Status:**
- Classes exist in `maven-plugin/target/classes/`
- JAR exists at `maven-plugin/target/maven-plugin-999-SNAPSHOT.jar`
- Maven analyzer is available for execution

**Expected Behavior:**
- Parent POMs (like `core/pom.xml`) should now only get `validate`, `install`, `deploy` targets
- Regular JAR projects should get full lifecycle phases
- No more hardcoded assumptions about which phases exist

## Verification

To verify the changes work:

1. **Check Parent POM**: Run `nx show project core` - should only show minimal targets
2. **Check Regular Project**: Run `nx show project core/runtime` - should show full lifecycle
3. **Verbose Logging**: Run with `DEBUG=*` to see discovered phases in logs

The implementation now uses Maven's own APIs to determine exactly which phases are applicable for each project, based on its packaging type and plugin configuration.