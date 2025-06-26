# Nx Maven Plugin

A sophisticated Nx plugin that integrates Maven projects into the Nx ecosystem, providing enhanced caching, dependency analysis, and parallel execution capabilities while maintaining Maven's exact execution behavior.

## Overview

This plugin bridges the gap between Maven and Nx by:
- Analyzing Maven projects using official Maven APIs
- Generating Nx project graphs from Maven dependencies
- Providing batch executors that preserve Maven session context
- Enabling Nx's caching and parallelization for Maven builds

## Architecture

### Core Components

#### 1. TypeScript Graph Plugin (`maven-plugin.ts`)
- **Purpose**: Main entry point for Nx integration
- **Responsibilities**:
  - Implements Nx's `createNodesV2` and `createDependencies` interfaces
  - Manages caching of Maven analysis results
  - Coordinates with Java analyzer for project discovery

#### 2. Java/Kotlin Analyzer (`src/main/kotlin/`)
- **Purpose**: Maven project analysis using official Maven APIs
- **Key Components**:
  - `NxAnalyzerMojo.kt`: Maven plugin that orchestrates analysis
  - `TargetGenerationService.kt`: Generates Nx targets from Maven goals
  - `TargetDependencyService.kt`: Analyzes dependencies between targets
  - `MavenUtils.kt`: Utility functions for Maven project introspection

#### 3. Batch Maven Executor (`src/executors/maven-batch/`)
- **Purpose**: Executes Maven goals with Nx's batch execution capabilities
- **Features**:
  - Preserves Maven session context across multiple goals
  - Supports parallel execution of independent goals
  - Provides detailed execution reporting

### Data Flow

```
Nx Discovery → TypeScript Plugin → Java Analyzer → Maven APIs
     ↓              ↓                    ↓             ↓
Project Graph ← Cached Results ← JSON Output ← Project Analysis
```

## Installation & Setup

### Prerequisites

- Nx CLI (v21.0.0+)
- Maven (3.8.8+)
- Java Development Kit (21+)
- Compiled plugin: `maven-plugin/target/classes/`

### Initial Setup

1. **Compile the Java components**:
   ```bash
   cd maven-plugin
   mvn clean compile
   ```

2. **Copy dependencies** (required for batch executor):
   ```bash
   mvn dependency:copy-dependencies
   ```

3. **Reset Nx state** to pick up the plugin:
   ```bash
   nx reset
   ```

### Configuration

Add the plugin to your `nx.json`:

```json
{
  "plugins": [
    {
      "plugin": "./maven-plugin",
      "options": {
        "mavenExecutable": "mvn",
        "verbose": false
      }
    }
  ]
}
```

#### Plugin Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `mavenExecutable` | string | `"mvn"` | Path to Maven executable |
| `verbose` | boolean | `false` | Enable verbose logging |

## Usage

### Project Discovery

The plugin automatically discovers Maven projects by scanning for `pom.xml` files:

```bash
# View discovered projects
nx show projects

# Generate project graph
nx graph --file graph.json

# View detailed project information  
nx show projects --verbose
```

### Target Execution

The plugin generates Nx targets for Maven goals:

```bash
# Run Maven compile goal
nx run my-project:compile

# Run Maven test goal with caching
nx run my-project:test

# Run goals in parallel across projects
nx run-many --target=compile --all
```

### Batch Execution

Use the maven-batch executor for complex workflows:

```json
{
  "targets": {
    "build": {
      "executor": "@nx-quarkus/maven-plugin:maven-batch",
      "options": {
        "goals": ["clean", "compile", "test"]
      }
    }
  }
}
```

#### Batch Executor Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `goals` | string[] | required | Maven goals to execute |
| `projectRoot` | string | `"."` | Project root directory |
| `verbose` | boolean | `false` | Enable verbose output |
| `timeout` | number | `300000` | Timeout in milliseconds |
| `failOnError` | boolean | `true` | Fail on Maven errors |

## Advanced Features

### Session Context Preservation

Traditional Maven goal execution in separate processes loses session context:
```bash
# ❌ This fails because install can't find the JAR
mvn jar:jar    # Creates JAR, sets project.artifact.file
mvn install:install  # New session, JAR context lost
```

Our batch executor preserves session context:
```bash
# ✅ This works because both goals share the same session
java -cp ... NxMavenBatchExecutor "jar:jar,install:install" "." false
```

### Generated Targets

The plugin automatically generates:

- **Individual Goal Targets**: `maven-compiler:compile`, `maven-surefire:test`, etc.
- **Phase Targets**: `compile`, `test`, `package`, `install`, etc.
- **Custom Batch Commands**: Use the TypeScript executor for combinations

### Complex Workflows

```json
{
  "targets": {
    "build-and-analyze": {
      "executor": "@nx-quarkus/maven-plugin:maven-batch",
      "options": {
        "goals": [
          "clean:clean",
          "compile:compile", 
          "org.sonarsource.scanner.maven:sonar-maven-plugin:sonar"
        ],
        "timeout": 600000,
        "verbose": true
      }
    },
    "integration-test": {
      "executor": "@nx-quarkus/maven-plugin:maven-batch",
      "options": {
        "goals": ["failsafe:integration-test", "failsafe:verify"]
      },
      "dependsOn": ["package"]
    }
  }
}
```

## Caching Strategy

The plugin implements multi-level caching:

1. **Global In-Memory Cache**: Fastest access for repeated operations
2. **File-based Cache**: Persistent cache between Nx runs  
3. **Hash-based Invalidation**: Automatic cache invalidation on changes

Cache is stored in: `${workspaceDataDirectory}/maven-analysis-cache.json`

## Development

### Build Cache Considerations

This repository uses **Develocity (Gradle Enterprise) build caching** which can prevent source changes from being compiled. If changes aren't reflected:

```bash
# Disable build cache for compilation
GRADLE_ENTERPRISE_BUILD_CACHE_ENABLED=false mvn clean compile

# Or force fresh compilation
find src -name "*.kt" -exec touch {} \;
mvn clean compile
```

### Development Workflow

1. **Make changes** to Java/Kotlin source files
2. **Recompile**:
   ```bash
   npm run compile-java
   # or manually: mvn clean compile
   ```
3. **Reset Nx state**:
   ```bash
   nx reset
   ```
4. **Test changes**:
   ```bash
   nx show projects
   ```
5. **Run end-to-end tests** (MANDATORY before committing):
   ```bash
   npm run test:e2e
   ```

### Testing

The plugin includes comprehensive test suites:

- **Unit Tests**: Test individual components in isolation
- **Integration Tests**: Test Maven API interactions
- **End-to-End Tests**: Test complete Nx integration

```bash
# Run unit tests
mvn test

# Run E2E tests (required before commits)
npm run test:e2e
```

## API Reference

### Maven Analysis Output

The Java analyzer produces structured JSON output with the following schema:

```typescript
interface CreateNodesResult {
  createNodesResults: CreateNodesV2Entry[];
  createDependencies: RawProjectGraphDependency[];
}

interface CreateNodesV2Entry {
  file: string;
  nodes: Record<string, ProjectConfiguration>;
}

interface ProjectConfiguration {
  name: string;
  root: string;
  targets: Record<string, TargetConfiguration>;
  metadata: ProjectMetadata;
}
```

### Target Configuration

Maven goals are mapped to Nx targets with rich metadata:

```typescript
interface TargetConfiguration {
  executor: string;
  options: Record<string, any>;
  metadata: TargetMetadata;
  dependsOn: TargetDependency[];
  cache: boolean;
  inputs: string[];
  outputs: string[];
}
```

### Batch Executor Result

```typescript
interface MavenBatchResult {
  overallSuccess: boolean;
  totalDurationMs: number;
  errorMessage?: string;
  goalResults: MavenGoalResult[];
}

interface MavenGoalResult {
  goal: string;
  success: boolean;
  durationMs: number;
  exitCode: number;
  output: string[];
  errors: string[];
}
```

## Troubleshooting

### Common Issues

#### Plugin Not Found
```bash
# Ensure plugin is compiled
mvn clean compile
# Reset Nx state
nx reset
```

#### Cache Issues
```bash
# Clear Maven analysis cache
rm -rf .nx/cache/maven-analysis-cache.json
# Reset Nx completely
nx reset
```

#### Build Cache Problems
```bash
# Disable build cache temporarily
GRADLE_ENTERPRISE_BUILD_CACHE_ENABLED=false mvn clean compile
```

#### Dependencies Missing
```bash
# Copy Maven dependencies for batch executor
mvn dependency:copy-dependencies
```

#### Session Context Issues
If individual goals fail due to missing artifacts, use the batch executor:
```json
{
  "options": {
    "goals": ["jar:jar", "install:install"]
  }
}
```

### Verbose Logging

Enable detailed logging for debugging:

```bash
# Environment variable
export NX_VERBOSE_LOGGING=true

# Command line flag
nx show projects --verbose

# Plugin configuration
{
  "plugin": "./maven-plugin",
  "options": {
    "verbose": true
  }
}
```

## Contributing

### Commit Guidelines

1. **Always run end-to-end tests** before committing:
   ```bash
   npm run test:e2e
   ```

2. **Follow conventional commit format**:
   ```
   feat: add support for Maven profiles
   fix: resolve dependency resolution issues
   docs: update API documentation
   ```

3. **Test with real Maven projects** to ensure compatibility

### Architecture Principles

- **API-First**: Always use official Maven APIs for Maven interactions
- **Maven Compatibility**: Execute goals exactly as Maven would
- **Nx Enhancement**: Add caching and parallelism without changing behavior
- **Never Hardcode**: Use Maven APIs to retrieve all project information

## License

MIT

## Related Documentation

- [Nx Plugin Development](https://nx.dev/extending-nx/intro/getting-started)
- [Maven Plugin API](https://maven.apache.org/plugin-developers/index.html)
- [Project Configuration](https://nx.dev/reference/project-configuration)