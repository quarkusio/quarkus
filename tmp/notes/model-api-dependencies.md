# Maven Model API Dependencies - COMPLETED ✅

## User Request
"Please add dependsOn from phases to goals"

## Implementation Strategy
Instead of hardcoding dependencies in TypeScript, used the Maven Model API to detect actual phase dependencies.

## Java Analyzer Enhancements ✅

### New Method: `detectPhaseDependencies()`
- **Uses Maven's standard lifecycle ordering** from the official Maven documentation
- **Filters dependencies** to only include phases that are relevant to the project
- **Supports all three Maven lifecycles**: default, clean, and site
- **Includes framework-specific phases**: `quarkus:dev`, `spring-boot:run`, etc.

### New Method: `getSuggestedPhaseDependencies()`
- **Framework-aware dependency detection** for plugin goals
- **Quarkus goals**: `dev`→`compile`, `build`→`test`, `generate-code`→`validate`
- **Spring Boot goals**: `run`→`compile`, `build-image`→`package`, `repackage`→`test`
- **Generic fallbacks** based on target type (serve, build, test, deploy, utility)
- **Phase binding override**: Goals explicitly bound to phases use Maven lifecycle dependencies

### Enhanced JSON Output
```json
{
  "relevantPhases": ["clean", "validate", "compile", "test", "package"],
  "pluginGoals": [
    {
      "pluginKey": "io.quarkus:quarkus-maven-plugin",
      "goal": "dev",
      "targetType": "serve",
      "suggestedDependencies": ["compile"]
    }
  ],
  "phaseDependencies": {
    "compile": ["process-resources"],
    "test": ["process-test-classes"],
    "package": ["test"],
    "install": ["verify"],
    "deploy": ["install"]
  }
}
```

## TypeScript Plugin Updates ✅

### Removed Hardcoded Dependencies
- **Eliminated** all hardcoded `dependsOn` arrays in TypeScript
- **Uses Maven Model API data** exclusively for dependency information

### Enhanced `createPhaseTarget()`
- **Accepts `phaseDependencies` parameter** from Java analyzer
- **Applies Maven lifecycle dependencies** automatically
- **Works for both standard and framework-specific phases**

### Enhanced `createPluginGoalTarget()`
- **Uses `suggestedDependencies`** from Java analyzer goal info
- **Framework-aware dependencies** detected in Java, not TypeScript
- **Phase binding support** for goals explicitly bound to Maven phases

## Benefits ✅

1. **Accurate Dependencies**: Uses actual Maven lifecycle ordering instead of guessing
2. **Framework Support**: Proper dependencies for Quarkus, Spring Boot, and other frameworks
3. **Dynamic Detection**: Dependencies adapt to each project's specific phase configuration
4. **Phase Binding**: Goals bound to specific phases get correct Maven lifecycle dependencies
5. **Maintainable**: All dependency logic in one place (Java analyzer) using Maven's own APIs

## Test Results ✅

### Basic Maven Project:
```json
"phaseDependencies": {
  "install": ["verify"], 
  "deploy": ["install"]
}
```

### Quarkus Project:
- **14 detected phases** including `quarkus:dev`, `quarkus:build`, `generate-code`
- **10 plugin goals** with framework-specific dependencies
- **Proper dependency chains** like `quarkus:dev` → `compile` → `process-resources`

The system now uses Maven's own Model API to determine accurate phase and goal dependencies instead of hardcoded assumptions.