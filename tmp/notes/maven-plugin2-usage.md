# Maven Plugin 2 - Nx Plugin for Maven Integration

## Overview

`maven-plugin2.ts` is an advanced Nx plugin that integrates Maven projects into Nx workspaces using a Java-based analyzer. It provides comprehensive dependency analysis, proper caching, and task orchestration.

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Nx Plugin     │───▶│  Java Analyzer  │───▶│ Maven Model API │
│ (TypeScript)    │    │ (MavenModelReader)   │   (Official)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                        │                        │
         ▼                        ▼                        ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ Nx project.json │    │    JSON Output  │    │    pom.xml     │
│  Configuration  │    │ (Nx Compatible) │    │   Analysis      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Key Features

### 1. **Automatic Project Discovery (V2 API)**
- Scans workspace for `pom.xml` files in parallel
- Creates Nx projects for each Maven module concurrently
- Uses `CreateNodesV2` API for maximum performance

### 2. **Comprehensive Dependency Analysis**
- **Project Dependencies**: Internal Maven dependencies → Nx project graph
- **Parent Relationships**: Maven parent POMs → Nx implicit dependencies
- **External Dependencies**: Maven artifacts tracked as metadata

### 3. **Advanced Caching & Task Orchestration**
- **namedInputs**: Smart cache invalidation based on file changes
- **Target Dependencies**: Proper build order with `dependsOn`
- **Outputs**: Tracks build artifacts for caching

### 4. **Async Java Program Integration**
- Executes compiled `MavenModelReader` Java program asynchronously
- Parallel execution for multiple projects
- Enhanced error handling with larger output buffers
- Framework detection (Spring Boot, Quarkus, Micronaut)

## Installation & Setup

### 1. Compile the Java Analyzer
```bash
cd maven-script
mvn compile
```

### 2. Register the Plugin in nx.json
```json
{
  "plugins": [
    {
      "plugin": "./maven-plugin2.ts",
      "options": {
        "buildTargetName": "build",
        "testTargetName": "test",
        "serveTargetName": "serve"
      }
    }
  ]
}
```

### 3. Verify Setup
```bash
nx show projects
nx graph
```

## Plugin Options

```typescript
interface MavenPluginOptions {
  buildTargetName?: string;      // Default: "build"
  testTargetName?: string;       // Default: "test" 
  serveTargetName?: string;      // Default: "serve"
  javaExecutable?: string;       // Default: "java"
  mavenExecutable?: string;      // Default: "mvn"
  compilerArgs?: string[];       // Default: []
}
```

## Generated Project Structure

### Basic Maven Project
```json
{
  "name": "my-service",
  "root": "services/my-service",
  "sourceRoot": "services/my-service/src/main/java",
  "projectType": "application",
  "targets": {
    "compile": {
      "executor": "nx:run-commands",
      "options": {
        "command": "mvn compile"
      },
      "inputs": ["production", "^production"],
      "outputs": ["{projectRoot}/target/classes"]
    },
    "test": {
      "executor": "nx:run-commands", 
      "options": {
        "command": "mvn test"
      },
      "inputs": ["test", "^production"],
      "dependsOn": ["compile"],
      "outputs": ["{projectRoot}/target/surefire-reports"]
    }
  },
  "namedInputs": {
    "default": ["{projectRoot}/**/*", "!{projectRoot}/target/**/*"],
    "production": ["default", "!{projectRoot}/src/test/**/*"],
    "test": ["default", "{projectRoot}/src/test/**/*"]
  },
  "implicitDependencies": {
    "external": ["org.springframework:spring-boot-starter:2.7.0"],
    "inheritsFrom": ["company-parent"]
  }
}
```

### Project with Internal Dependencies
```json
{
  "implicitDependencies": {
    "projects": ["shared-utils", "common-lib"],
    "external": ["org.junit.jupiter:junit-jupiter (test)"],
    "inheritsFrom": ["service-parent"]
  },
  "targets": {
    "compile": {
      "dependsOn": ["^compile"],
      "inputs": ["production", "^production"]
    },
    "package": {
      "dependsOn": ["^package", "compile"],
      "outputs": ["{projectRoot}/target/*.jar"]
    }
  }
}
```

## Dependency Types Generated

### 1. Static Dependencies (`CreateDependencies`)
```typescript
{
  source: "my-service",
  target: "shared-utils", 
  type: "static"
}
```

### 2. Implicit Dependencies
```typescript
{
  source: "my-service",
  target: "company-parent",
  type: "implicit" 
}
```

## Java Analyzer Integration

### Execution Flow
1. **Discovery**: Plugin finds `pom.xml` files
2. **Analysis**: Calls Java program: `java -cp "target/classes" MavenModelReader "pom.xml" --nx`
3. **Parsing**: Extracts JSON from program output
4. **Mapping**: Converts to Nx project configuration

### Error Handling
- **Timeout**: 30-second limit for Java execution
- **Parsing**: Robust JSON extraction from output
- **Fallbacks**: Graceful degradation when analysis fails
- **Logging**: Warnings for debugging issues

### Performance Optimizations
- **Caching**: Nx caches project configurations
- **Parallel**: Multiple projects analyzed concurrently
- **Incremental**: Only re-analyzes changed POMs

## Target Mapping

| Maven Phase | Nx Target | Dependencies | Outputs |
|-------------|-----------|--------------|---------|
| `compile` | `compile` | `^compile` | `target/classes` |
| `test` | `test` | `compile, ^compile` | `target/surefire-reports` |
| `package` | `build` | `compile, ^package` | `target/*.jar` |
| `install` | `install` | `package, ^install` | Local repo |
| `clean` | `clean` | None | Removes `target/` |

## Advanced Features

### 1. Multi-Module Support
- Handles Maven parent-child relationships
- Maps module dependencies to Nx project graph
- Supports nested module structures

### 2. Spring Boot Integration
```json
{
  "targets": {
    "serve": {
      "executor": "nx:run-commands",
      "options": {
        "command": "mvn spring-boot:run"
      }
    }
  }
}
```

### 3. Quarkus Integration
```json
{
  "targets": {
    "serve": {
      "executor": "nx:run-commands", 
      "options": {
        "command": "mvn quarkus:dev"
      }
    },
    "build-native": {
      "executor": "nx:run-commands",
      "options": {
        "command": "mvn clean package -Pnative"
      }
    }
  }
}
```

## Usage Examples

### Running Targets
```bash
# Build a specific project
nx build my-service

# Test with dependencies
nx test my-service

# Build all affected projects
nx affected:build

# Serve in dev mode
nx serve my-service
```

### Dependency Visualization
```bash
# Show project graph
nx graph

# Show dependencies for specific project
nx show project my-service

# List affected projects
nx print-affected
```

## Troubleshooting

### Java Program Not Found
```
Error: Maven analyzer Java program not found
```
**Solution**: Ensure `maven-script` is compiled:
```bash
cd maven-script && mvn compile
```

### JSON Parsing Errors
```
Error: No valid JSON output from Maven analyzer
```
**Solution**: Check Java program execution manually:
```bash
java -cp "maven-script/target/classes" MavenModelReader "pom.xml" --nx
```

### Missing Dependencies
```
Warning: Failed to analyze dependencies for project
```
**Solution**: Verify pom.xml is valid and accessible:
```bash
mvn validate -f path/to/pom.xml
```

## Benefits

1. **Unified Build System**: Single `nx` command for all projects
2. **Smart Caching**: Only rebuilds what changed
3. **Dependency Tracking**: Visual project graph
4. **Parallel Execution**: Builds independent projects concurrently
5. **Developer Experience**: IDE integration and tooling