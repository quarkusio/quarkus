# Target Name Conversion Logic

## Overview
Found the key methods that handle conversion between Maven plugin goal names and Nx target names.

## Key Methods and Locations

### 1. Primary Conversion Method: `getTargetName()`
**Location:** `/ExecutionPlanAnalysisService.java` lines 268-271

```java
public static String getTargetName(String artifactId, String goal) {
    String pluginName = normalizePluginName(artifactId);
    return pluginName + ":" + goal;
}
```

**Example conversions:**
- `maven-jar-plugin` + `jar` → `maven-jar:jar`
- `maven-compiler-plugin` + `compile` → `maven-compiler:compile`
- `maven-surefire-plugin` + `test` → `maven-surefire:test`

### 2. Plugin Name Normalization: `normalizePluginName()`
**Location:** `/ExecutionPlanAnalysisService.java` lines 286-291

```java
public static String normalizePluginName(String artifactId) {
    if (artifactId == null) {
        return null;
    }
    return artifactId.replace("-maven-plugin", "").replace("-plugin", "");
}
```

**Example transformations:**
- `maven-jar-plugin` → `maven-jar`
- `maven-compiler-plugin` → `maven-compiler`
- `quarkus-maven-plugin` → `quarkus`
- `spring-boot-maven-plugin` → `spring-boot`

### 3. Reverse Conversion: `extractGoalFromTargetName()`
**Location:** `/ExecutionPlanAnalysisService.java` lines 276-281

```java
public static String extractGoalFromTargetName(String targetName) {
    if (targetName == null || !targetName.contains(":")) {
        return targetName;
    }
    return targetName.substring(targetName.lastIndexOf(":") + 1);
}
```

**Example extractions:**
- `maven-compiler:compile` → `compile`
- `maven-jar:jar` → `jar`
- `maven-surefire:test` → `test`

### 4. Goal to Target Name Conversion in Phase Generation
**Location:** `/TargetGenerationService.java` lines 139-149

```java
private String getTargetNameFromGoal(String goalName) {
    // goalName is already in format "artifactId:goal" or "groupId:artifactId:goal"
    // Extract just the artifactId:goal part for target name
    String[] parts = goalName.split(":");
    if (parts.length >= 2) {
        String artifactId = parts[parts.length - 2]; // Second to last part is artifactId
        String goal = parts[parts.length - 1];       // Last part is goal
        return ExecutionPlanAnalysisService.getTargetName(artifactId, goal);
    }
    return goalName; // Fallback to original name
}
```

## Usage Patterns

### In Target Generation
1. **Plugin Goal Targets:** Line 179 in `TargetGenerationService.java`
   ```java
   String targetName = ExecutionPlanAnalysisService.getTargetName(plugin.getArtifactId(), goal);
   ```

2. **Phase Target Dependencies:** Line 98 in `TargetGenerationService.java`
   ```java
   String targetName = getTargetNameFromGoal(goalName);
   ```

### In Analysis Service
1. **Project Analysis:** Line 348 in `ExecutionPlanAnalysisService.java`
   ```java
   String targetName = getTargetName(pluginArtifactId, goal);
   ```

## Key Insight
The conversion follows this pattern:
- **Full Maven Goal:** `org.apache.maven.plugins:maven-jar-plugin:jar`
- **Normalized Target:** `maven-jar:jar`

The system removes:
- Group ID (`org.apache.maven.plugins`)
- `-maven-plugin` and `-plugin` suffixes from artifact ID
- Keeps artifact ID + goal in `plugin:goal` format