# What Maven Does When `mvn install` Runs on a Parent POM

## Key Discovery: Parent POMs Have Minimal Lifecycle

When you run `mvn install` on a parent POM with `packaging=pom`, Maven does something very different than regular projects:

### **Parent POM Lifecycle (packaging=pom)**

**"Such projects have the simplest lifecycle that consists of only two steps: install and deploy."**

**Phases that RUN:**
- `validate` ✓
- `install` ✓ (installs POM file to local repository)
- `deploy` ✓ (if called)

**Phases that are SKIPPED:**
- `compile` ❌ (no source code)
- `test-compile` ❌ (no test code)  
- `test` ❌ (no tests to run)
- `package` ❌ (no artifact to create)
- `verify` ❌ (no artifact to verify)

**Only binds goals to the install and deploy phases.**

### **What Actually Happens**

```bash
cd core  # Parent POM directory
mvn install
```

**Maven executes:**
1. **Validate phase**: Checks POM structure
2. **Install phase**: Installs the POM file itself to `~/.m2/repository`
3. **Module building**: If `<modules>` are declared, builds all child modules

**Maven SKIPS:**
- All compilation (no source code)
- All testing (no tests)
- All packaging (no JAR/WAR to create)

## Current Nx Problem

**Current Nx behavior for parent POMs:**
```java
// Generates ALL targets for parent POMs:
targets = {
  "validate": {...},
  "compile": {...},    // ❌ WRONG - Maven skips this
  "test": {...},       // ❌ WRONG - Maven skips this  
  "package": {...},    // ❌ WRONG - Maven skips this
  "verify": {...},     // ❌ WRONG - Maven skips this
  "install": {...}     // ✓ CORRECT
}
```

**What Nx SHOULD generate for parent POMs:**
```java
targets = {
  "validate": {...},   // ✓ CORRECT
  "install": {...},    // ✓ CORRECT
  "deploy": {...}      // ✓ CORRECT
}
// Skip: compile, test, package, verify
```

## The Fix

**Detect parent POMs:**
```java
private static boolean isParentPom(MavenProject project) {
    return "pom".equals(project.getPackaging());
}
```

**Generate appropriate targets:**
```java
if (isParentPom(project)) {
    // Only generate: validate, install, deploy
    String[] parentPhases = {"validate", "install", "deploy"};
} else {
    // Generate full lifecycle for regular projects
    String[] phases = {"clean", "validate", "compile", "test", "package", "verify", "install", "deploy"};
}
```

## Why This Matters

**Current Nx generates targets Maven never executes:**
- `nx compile core` → tries to run compile on parent POM (Maven would skip)
- `nx test core` → tries to run tests on parent POM (Maven would skip)
- `nx package core` → tries to package parent POM (Maven would skip)

**Correct Nx behavior:**
- `nx compile core` → should not exist (or be no-op)
- `nx install core` → should work (installs POM + builds modules)

This explains a key difference between Nx and Maven behavior - Nx is generating and trying to execute lifecycle phases that Maven would automatically skip for parent POMs.