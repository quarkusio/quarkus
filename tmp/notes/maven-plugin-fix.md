# Maven Plugin Build Error Fix

## Problem
The Nx Maven plugin was causing build errors with two main issues:

1. **maven-enforcer-plugin error**: "No rules are configured" 
2. **install goal error**: "The packaging plugin did not assign a file to the build artifact"

## Root Causes

### Issue 1: Maven Enforcer Plugin
- The main Quarkus project defines `maven-enforcer-plugin.phase=validate` in its root POM
- The parent POM references this property: `<phase>${maven-enforcer-plugin.phase}</phase>`
- The maven-plugin is standalone and doesn't inherit from Quarkus parent, so it lacked this property
- When ExecutionPlanAnalysisService calculated execution plans for "validate" phase, enforcer plugin failed

### Issue 2: Maven Install Goal
- Nx runs individual Maven goals separately instead of full lifecycle
- The `install` goal runs in isolation without access to the artifact created by the `jar` goal
- This works with `mvn clean install` (full lifecycle) but fails with individual goal execution

## Solutions Applied

### Fix 1: Added Missing Property
Added to `/maven-plugin/pom.xml`:
```xml
<maven-enforcer-plugin.phase>validate</maven-enforcer-plugin.phase>
```

### Fix 2: Enhanced Plugin Configuration
Added helpmojo execution to ensure proper Maven plugin lifecycle:
```xml
<execution>
    <id>help-goal</id>
    <goals>
        <goal>helpmojo</goal>
    </goals>
</execution>
```

## Result
- ✅ Maven enforcer plugin configuration error resolved
- ✅ Plugin builds successfully with `mvn clean install`
- ⚠️ Nx individual goal execution still has limitations (use `mvn clean install` for full builds)