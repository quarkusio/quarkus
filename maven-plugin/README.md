# Nx Maven Plugin with Batch Executor

A comprehensive Maven integration for Nx that provides granular caching while preserving Maven session context through a batch executor.

## Features

- **Session Context Preservation**: Multiple Maven goals share the same session, allowing artifacts from one goal to be accessible by subsequent goals
- **Granular Caching**: Each Maven goal is a separate Nx target for optimal caching
- **Batch Execution**: Execute multiple goals together when needed to maintain Maven compatibility
- **TypeScript Executor**: Native Nx executor for seamless integration
- **True Nx Batch Support**: Implements proper Nx batch executor interface for parallel task processing

## Installation

1. Ensure the Maven plugin is compiled:
   ```bash
   cd maven-plugin
   mvn compile dependency:copy-dependencies
   ```

2. The plugin automatically generates Nx targets for all Maven goals and phases.

## Usage

### Using the TypeScript Executor

Add to your `project.json`:

```json
{
  "targets": {
    "custom-build": {
      "executor": "@nx-quarkus/maven-plugin:maven-batch",
      "options": {
        "goals": ["compile", "test"],
        "verbose": true
      }
    },
    "install-with-jar": {
      "executor": "@nx-quarkus/maven-plugin:maven-batch",
      "options": {
        "goals": [
          "org.apache.maven.plugins:maven-jar-plugin:jar",
          "org.apache.maven.plugins:maven-install-plugin:install"
        ],
        "failOnError": true,
        "outputFile": "install-results.json"
      }
    }
  }
}
```

### Executor Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `goals` | `string[]` | **required** | Maven goals to execute in batch |
| `projectRoot` | `string` | `"."` | Project root relative to workspace |
| `verbose` | `boolean` | `false` | Enable verbose output |
| `mavenPluginPath` | `string` | `"maven-plugin"` | Path to Maven plugin directory |
| `timeout` | `number` | `300000` | Timeout in milliseconds |
| `outputFile` | `string` | `undefined` | Optional file to write results |
| `failOnError` | `boolean` | `true` | Fail executor if any goal fails |

### Running Executors

```bash
# Execute custom build target
nx custom-build my-project

# Execute install with JAR creation
nx install-with-jar my-project

# Generated individual goal targets (auto-created)
nx maven-compiler:compile my-project
nx maven-surefire:test my-project

# Generated phase targets (auto-created)
nx compile my-project  # Runs all goals up to compile phase
nx install my-project  # Runs all goals up to install phase

# Run with batch execution (experimental)
nx run-many --target=custom-build --projects=project1,project2 --batch
```

## How It Works

### Architecture

1. **Maven Analysis**: The Java plugin analyzes your Maven configuration and generates Nx targets
2. **Batch Executor**: A Java application using Maven Invoker API that preserves session context
3. **TypeScript Executor**: An Nx executor that wraps the batch executor with proper error handling

### Session Context Benefits

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
- **Batch Commands**: Use the TypeScript executor for custom combinations

## Examples

### Basic Usage

```bash
# Auto-generated targets work out of the box
nx compile my-java-project
nx test my-java-project  
nx package my-java-project
```

### Custom Batch Execution

```typescript
// In project.json
{
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
  }
}
```

### Handling Dependencies

The executor automatically handles goal ordering within a batch, but you can still use Nx dependencies:

```json
{
  "integration-test": {
    "executor": "@nx-quarkus/maven-plugin:maven-batch",
    "options": {
      "goals": ["failsafe:integration-test", "failsafe:verify"]
    },
    "dependsOn": ["package"]
  }
}
```

## Troubleshooting

### Plugin Not Found
```
Error: Maven plugin directory not found
```
**Solution**: Ensure the maven-plugin is compiled: `mvn compile`

### Dependencies Missing
```
Error: Maven dependencies not copied
```
**Solution**: Copy dependencies: `mvn dependency:copy-dependencies`

### Session Context Issues
If individual goals fail due to missing artifacts, use the batch executor:
```json
{
  "options": {
    "goals": ["jar:jar", "install:install"]
  }
}
```

## Development

To build the TypeScript executor:
```bash
cd maven-plugin
npm install
npm run build
```