# Target Groups Enhancement: Java-First Approach

## Problem
Target groups for Maven phases weren't showing up because the Java analyzer wasn't providing the necessary `goalsByPhase` data that the TypeScript code expected.

## Solution Architecture

### Java Side (MavenModelReader)
The Java analyzer should provide rich phase-to-goals mapping:

```java
// Should output in maven-results.json:
{
  "projectPath": {
    "goalsByPhase": {
      "clean": ["clean-plugin:clean"],
      "compile": ["compiler:compile", "resources:resources"],
      "test": ["compiler:testCompile", "resources:testResources", "surefire:test"],
      "package": ["jar:jar"],
      "install": ["install:install"],
      "deploy": ["deploy:deploy"]
    },
    "pluginGoals": [
      { "targetName": "clean-plugin:clean", "phase": "clean", ... },
      { "targetName": "compiler:compile", "phase": "compile", ... },
      // etc.
    ]
  }
}
```

### TypeScript Side (maven-plugin2.ts) 
Simplified to directly use Java data:

```typescript
function generateTargetGroups(targets, nxConfig) {
  const targetGroups = {};
  const goalsByPhase = nxConfig.goalsByPhase || {};
  
  // Create target group for each phase with its goals
  for (const [phase, goals] of Object.entries(goalsByPhase)) {
    if (goals.length > 0) {
      const groupName = phase.charAt(0).toUpperCase() + phase.slice(1);
      targetGroups[groupName] = goals.filter(goal => targets[goal]);
    }
  }
  
  return targetGroups;
}
```

## Expected Target Groups Output

For a typical Maven project:
```json
{
  "targetGroups": {
    "Clean": ["clean-plugin:clean"],
    "Compile": ["compiler:compile", "resources:resources"],
    "Test": ["compiler:testCompile", "resources:testResources", "surefire:test"],
    "Package": ["jar:jar"],
    "Install": ["install:install"],
    "Deploy": ["deploy:deploy"]
  }
}
```

## Current Status

✅ **TypeScript Side**: Updated to use `goalsByPhase` data from Java
✅ **Architecture**: Designed to let Java do the heavy lifting
❌ **Java Side**: Needs enhancement to provide `goalsByPhase` data

## Next Steps

1. **Enhance Java MavenModelReader** to analyze plugin executions and their bound phases
2. **Export goalsByPhase mapping** in the JSON output
3. **Test with real Maven projects** to ensure phase grouping works correctly

## Benefits

1. **Accurate Phase Mapping**: Java analyzer has full access to Maven model
2. **Consistent with Maven**: Uses actual plugin execution bindings
3. **Simple TypeScript**: No complex phase detection logic needed
4. **Extensible**: Easy to add more phase-related data from Java side