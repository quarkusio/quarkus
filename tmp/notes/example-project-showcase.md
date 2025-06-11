# Good Example Project: Quarkus Integration Test Main

## Project Location
`/home/jason/projects/triage/java/quarkus/integration-tests/main`

## Why This Is A Great Example ✅

### 1. **Rich Quarkus Application**
- **Full-stack Quarkus app** with REST, JPA, WebSockets, Health checks, OpenAPI
- **45+ dependencies** including Hibernate ORM, Undertow, Micrometer, Security
- **Demonstrates real-world complexity** of a typical enterprise application

### 2. **Comprehensive Phase Detection**
Our analyzer detects **14 relevant phases**:
```json
"relevantPhases": [
  "clean", "validate", "compile", "test-compile", "test", 
  "package", "verify", "install", "deploy", "process-resources",
  "quarkus:dev", "quarkus:build", "generate-code", "integration-test"
]
```

### 3. **Rich Plugin Goal Detection**
**10 plugin goals** detected across multiple plugins:
- **Quarkus Maven Plugin**: `dev`, `build`, `generate-code`, `test`
- **Maven Surefire Plugin**: `test`

### 4. **Smart Dependency Detection**
Phase dependencies using Maven Model API:
```json
"phaseDependencies": {
  "compile": ["process-resources"],
  "quarkus:dev": ["compile"],
  "quarkus:build": ["test"],
  "generate-code": ["validate"],
  "install": ["verify"],
  "deploy": ["install"]
}
```

Plugin goal dependencies:
```json
{
  "goal": "dev",
  "targetType": "serve", 
  "suggestedDependencies": ["compile"]
},
{
  "goal": "build",
  "targetType": "build",
  "suggestedDependencies": ["test"]
}
```

## Generated NX Targets ✅

This would generate **24 NX targets** (14 phases + 10 goals):

### **Phase Targets** (with Model API dependencies):
- `clean` → no dependencies
- `validate` → no dependencies  
- `compile` → depends on `process-resources`
- `test-compile` → depends on Maven lifecycle
- `test` → depends on Maven lifecycle
- `package` → depends on Maven lifecycle
- `quarkus:dev` → depends on `compile`
- `quarkus:build` → depends on `test`

### **Plugin Goal Targets** (with framework-aware dependencies):
- `serve` (quarkus:dev) → depends on `compile`
- `build` (quarkus:build) → depends on `test`
- `quarkus:generate-code` → depends on `validate`
- `quarkus:test` → depends on `test-compile`
- `maven-surefire:test` → depends on `test-compile`

## Rich Metadata ✅

Each target includes comprehensive metadata:

```json
{
  "executor": "@nx/run-commands:run-commands",
  "options": {
    "command": "mvn quarkus:dev",
    "cwd": "{projectRoot}"
  },
  "metadata": {
    "type": "goal",
    "plugin": "io.quarkus:quarkus-maven-plugin",
    "goal": "dev",
    "targetType": "serve",
    "technologies": ["maven"],
    "description": "Start Quarkus development mode"
  },
  "dependsOn": ["compile"],
  "inputs": ["{projectRoot}/pom.xml", "{projectRoot}/src/**/*"]
}
```

## Developer Experience ✅

Developers get:
1. **Clear distinctions**: "Maven lifecycle phase: test" vs "Start Quarkus development mode"
2. **Proper dependencies**: Can't run `quarkus:dev` without `compile` completing first
3. **Framework awareness**: Quarkus-specific goals with appropriate dependencies
4. **Comprehensive coverage**: All 14 lifecycle phases + 10 plugin goals available

## Alternative Good Examples

### Simple Spring Boot Project
For Spring Boot comparison, you could create a project with:
- `spring-boot-maven-plugin` goals: `run`, `build-image`, `repackage`
- Dependencies: `run`→`compile`, `build-image`→`package`

### Docker/Container Project  
`integration-tests/container-image` would show:
- Container build goals with proper dependencies
- Multi-module structure with parent/child relationships

This Quarkus integration test is the **best example** because it demonstrates the full power of our Maven Model API-based dependency detection across a complex, real-world application.