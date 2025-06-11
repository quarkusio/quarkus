# Maven to Nx Project Configuration Generator

## Overview
Created a Java script using the Maven Model API that can generate Nx Project Configuration JSON from Maven POM files. This bridges Maven projects into Nx workspaces programmatically.

## What the Script Does

### 1. Maven Analysis Mode (default)
```bash
mvn exec:java -Dexec.mainClass="MavenModelReader" -Dexec.args="pom.xml"
```
- Analyzes Maven POM structure
- Shows plugin configurations and executions
- Displays lifecycle phase bindings
- Lists goals bound to each phase

### 2. Nx Configuration Mode (--nx flag)
```bash
mvn exec:java -Dexec.mainClass="MavenModelReader" -Dexec.args="pom.xml --nx"
```
- Generates valid Nx project.json configuration
- Maps Maven lifecycle phases to Nx targets
- Includes plugin-specific targets (Spring Boot, Quarkus, etc.)
- Adds project metadata and tags

## Generated Nx Configuration Features

### Project Metadata
- `name`: From Maven artifactId
- `projectType`: "application" for JAR/WAR, "library" for POM
- `sourceRoot`: Set to "src/main/java"
- `$schema`: Points to Nx project schema

### Targets
Maps Maven commands to Nx targets:
- **Lifecycle phases**: validate, compile, test, package, verify, install, deploy
- **Plugin-specific**: 
  - Spring Boot: `serve` (spring-boot:run)
  - Quarkus: `serve` (quarkus:dev), `build-native`
  - Exec plugin: `exec` (exec:java)
  - Compiler: `compile-main`, `compile-test`

### Tags
Auto-generates tags based on:
- Maven packaging type: `maven:jar`, `maven:pom`
- GroupId segments: `group:com`, `group:example`

### Complete Dependency Analysis (NEW!)
The script now generates all types of Nx dependencies and configurations:

#### 1. `implicitDependencies` - Manual Project Dependencies
- **projects**: Internal project dependencies (same groupId or org prefix)
- **external**: External Maven dependencies with versions and scopes
- **inheritsFrom**: Parent POM relationships

#### 2. `namedInputs` - Cache Input Definitions
- **default**: All project files except build outputs
- **production**: Source files excluding tests
- **test**: Test files and sources
- **withDependencies**: Includes dependency outputs when internal deps exist

#### 3. Target-Level Dependencies
Each target now includes:
- **inputs**: What files/outputs the target depends on for caching
- **dependsOn**: Which other targets must run first
- **outputs**: What files/directories the target produces

#### Smart Internal Detection
Considers dependencies "internal" (project dependencies) if:
1. Same groupId as current project
2. GroupId shares organizational prefix (first 2 parts)
3. Contains common internal patterns (`internal`, `${project.groupId}`)

### Complete Dependency Examples

#### Basic Project (No Internal Dependencies)
```json
{
  "targets": {
    "compile": {
      "inputs": ["production", "^production"],
      "outputs": ["{projectRoot}/target/classes"]
    },
    "test": {
      "inputs": ["test", "^production"],
      "dependsOn": ["compile"],
      "outputs": ["{projectRoot}/target/surefire-reports"]
    }
  },
  "implicitDependencies": {
    "external": ["org.springframework:spring-boot-starter:2.7.0"]
  },
  "namedInputs": {
    "default": ["{projectRoot}/**/*", "!{projectRoot}/target/**/*"],
    "production": ["default", "!{projectRoot}/src/test/**/*"],
    "test": ["default", "{projectRoot}/src/test/**/*"]
  }
}
```

#### Project with Internal Dependencies
```json
{
  "targets": {
    "compile": {
      "inputs": ["production", "^production"],
      "dependsOn": ["^compile"],
      "outputs": ["{projectRoot}/target/classes"]
    },
    "package": {
      "inputs": ["production", "^production"],
      "dependsOn": ["^package", "compile"],
      "outputs": ["{projectRoot}/target/*.jar"]
    }
  },
  "implicitDependencies": {
    "projects": ["core-common", "shared-utils"],
    "external": ["org.junit.jupiter:junit-jupiter:5.8.2 (test)"],
    "inheritsFrom": ["company-parent"]
  },
  "namedInputs": {
    "withDependencies": ["^production"]
  }
}
```

### Nx Dependency Types Explained

#### `inputs` Array
- **`production`**: Use production namedInput
- **`^production`**: Use production outputs from dependencies
- **`{projectRoot}/path/**/*`**: Specific file patterns

#### `dependsOn` Array  
- **`compile`**: Run this project's compile target first
- **`^compile`**: Run all dependency projects' compile targets first
- **`^package`**: Run all dependency projects' package targets first

#### `outputs` Array
- **`{projectRoot}/target/classes`**: Compiled class files
- **`{projectRoot}/target/*.jar`**: Built JAR files
- **`{projectRoot}/target/surefire-reports`**: Test reports

## Technical Implementation

### Maven Model API Usage
- Uses official Apache Maven APIs
- Parses POM.xml programmatically
- Accesses plugin configurations and executions
- No need to execute Maven commands

### JSON Generation
- Custom JSON serializer for clean output
- Maintains proper structure for Nx schema
- Uses LinkedHashMap for consistent ordering

## Benefits

1. **Programmatic**: No shell command parsing needed
2. **Official APIs**: Uses stable Maven Model API
3. **Flexible**: Can be extended for other integrations
4. **Complete**: Captures all Maven plugin information
5. **Bridge to Nx**: Enables Maven projects in Nx workspaces

## Location
Script is in: `/home/jason/projects/triage/java/quarkus/maven-script/`