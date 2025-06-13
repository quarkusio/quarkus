# Maven Analyzer vs Maven Analyzer Mojo: Component Analysis

## Overview
This codebase contains two distinct but related Maven analysis components designed for Nx workspace integration. Both serve the same purpose but use different architectural approaches.

## Component 1: Maven Analyzer (Standalone Java Application)

### Location & Structure
- **File**: `/maven-plugin-v2/src/main/java/MavenAnalyzer.java`
- **Type**: Standalone Java application with main() method
- **Dependencies**: Apache Maven Model, Google Gson
- **Execution**: Direct Java execution or via Maven exec plugin

### Key Characteristics
- **Standalone Application**: Can be run independently using `java -jar` or `mvn exec:java`
- **Batch Processing**: Designed to analyze multiple pom.xml files in one execution
- **Command Line Interface**: Takes pom file paths as command line arguments
- **Direct POM Parsing**: Uses MavenXpp3Reader to read pom.xml files directly
- **Nx Integration Focus**: Outputs CreateNodesV2 and CreateDependencies compatible JSON

### Usage Pattern
```bash
java -cp <classpath> MavenAnalyzer /path/to/pom1.xml /path/to/pom2.xml
# or
mvn exec:java -Dexec.mainClass="MavenAnalyzer" -Dexec.args="pom.xml"
```

### Output Format
Generates JSON compatible with Nx TypeScript interfaces:
```json
{
  "createNodesResults": [
    ["pom.xml", { "projects": { "project-name": {...} } }]
  ],
  "createDependencies": [...]
}
```

## Component 2: Maven Analyzer Mojo (Maven Plugin)

### Location & Structure
- **File**: `/maven-plugin-v2/src/main/java/NxAnalyzerMojo.java`
- **Type**: Maven plugin (mojo = Maven plain Old Java Object)
- **Parent**: Extends AbstractMojo
- **Dependencies**: Maven Plugin API, Maven Core, Maven Session
- **Execution**: Invoked as Maven plugin goal

### Key Characteristics
- **Maven Plugin Architecture**: Integrates directly into Maven's execution lifecycle
- **Session Integration**: Has access to MavenSession and ProjectBuilder APIs
- **Workspace Discovery**: Automatically discovers all pom.xml files in workspace
- **Effective POM Analysis**: Uses Maven's resolved/effective POM data
- **Plugin Annotation**: Uses @Mojo annotation for Maven integration

### Usage Pattern
```bash
mvn io.quarkus:maven-plugin-v2:analyze
# or with configuration
mvn nx:analyze -Dnx.outputFile=/custom/path.json
```

### Output Format
Same JSON structure as standalone analyzer but with enhanced data from Maven's effective POM resolution.

## Key Differences

### 1. **Execution Model**
- **MavenAnalyzer**: Standalone application, external to Maven
- **NxAnalyzerMojo**: Maven plugin, runs within Maven's execution context

### 2. **Data Access**
- **MavenAnalyzer**: Limited to raw pom.xml parsing
- **NxAnalyzerMojo**: Access to resolved dependencies, effective POM, Maven session

### 3. **Integration Approach**
- **MavenAnalyzer**: Called from TypeScript via external process execution
- **NxAnalyzerMojo**: Native Maven plugin that can be invoked directly

### 4. **Configuration**
- **MavenAnalyzer**: Command line arguments and system properties
- **NxAnalyzerMojo**: Maven plugin parameters and session properties

### 5. **Project Discovery**
- **MavenAnalyzer**: Requires explicit pom.xml file paths as input
- **NxAnalyzerMojo**: Automatically discovers all projects in workspace

## What is a "Mojo"?

A **Mojo** (Maven plain Old Java Object) is the core component of a Maven plugin. Think of it like this:

- **Maven Plugin** = Collection of mojos (goals)
- **Mojo** = Individual executable goal within a plugin
- **Goal** = Specific task the mojo performs

### Mojo Characteristics
1. **Extends AbstractMojo**: Inherits Maven plugin functionality
2. **@Mojo annotation**: Defines goal name and execution requirements
3. **@Parameter annotations**: Define configurable plugin parameters
4. **execute() method**: Contains the actual logic to perform

### Example Mojo Declaration
```java
@Mojo(name = "analyze", requiresProject = false)
public class NxAnalyzerMojo extends AbstractMojo {
    @Parameter(property = "nx.outputFile")
    private String outputFile;
    
    public void execute() throws MojoExecutionException {
        // Plugin logic here
    }
}
```

## Current Integration Strategy

The TypeScript plugin (`maven-plugin2.ts`) currently uses the **standalone MavenAnalyzer** approach:

1. Discovers pom.xml files
2. Spawns Maven process with exec plugin
3. MavenAnalyzer runs within that process
4. Results written to JSON file
5. TypeScript reads and processes the JSON

## Benefits of Each Approach

### Standalone MavenAnalyzer Benefits
- **Simplicity**: Direct execution, no plugin registration needed
- **Flexibility**: Can be called from any context
- **Portability**: Works without Maven plugin infrastructure
- **Debugging**: Easier to test and debug standalone

### Maven Mojo Benefits
- **Native Integration**: True Maven plugin with proper lifecycle integration
- **Rich Data Access**: Full Maven session and effective POM data
- **Professional Approach**: Standard Maven ecosystem pattern
- **Enhanced Analysis**: Access to resolved dependencies and plugin configurations

## Relationship to Each Other

These components are **complementary implementations** of the same functionality:
- Both analyze Maven projects for Nx integration
- Both generate compatible JSON output
- Both target the same use case but with different technical approaches
- The mojo version represents a more sophisticated, Maven-native implementation

The current codebase maintains both approaches, with the TypeScript integration primarily using the standalone analyzer while the mojo provides an alternative execution path with enhanced capabilities.