# Maven Analyzer Structure Analysis

## Current Java MavenAnalyzer Structure

### Java Classes
- **Location**: `/maven-plugin-v2/src/main/java/MavenAnalyzer.java`
- **Main Class**: `MavenAnalyzer`
- **Dependencies**: 
  - Apache Maven Model (`org.apache.maven.model`)
  - Google Gson for JSON serialization
  - Java File I/O utilities

### Current Output Format
The Java analyzer produces a JSON structure with this format:

```json
{
  "rootProject": "/absolute/path/to/workspace",
  "projects": [
    {
      "id": "groupId:artifactId",
      "artifactId": "project-name",
      "groupId": "org.example", 
      "version": "1.0.0",
      "packaging": "jar|pom|war",
      "basedir": "/absolute/path/to/project",
      "dependencies": [
        {
          "groupId": "...",
          "artifactId": "...",
          "version": "...",
          "scope": "compile|test|provided|runtime",
          "type": "jar|pom"
        }
      ],
      "plugins": [
        {
          "groupId": "...",
          "artifactId": "...",
          "version": "...",
          "executions": [
            {
              "id": "execution-id",
              "phase": "compile|test|package",
              "goals": ["goal1", "goal2"]
            }
          ]
        }
      ],
      "targets": {
        "clean": {
          "executor": "@nx/run-commands:run-commands",
          "options": {
            "command": "mvn clean",
            "cwd": "{projectRoot}"
          },
          "inputs": ["{projectRoot}/pom.xml"],
          "outputs": [],
          "metadata": {
            "type": "phase|goal",
            "phase": "clean",
            "technologies": ["maven"],
            "description": "Maven lifecycle phase: clean"
          }
        }
      }
    }
  ]
}
```

### Current TypeScript Integration (maven-plugin2.ts)

The TypeScript plugin processes the Java analyzer output:

1. **CreateNodesV2 Implementation**:
   - Filters pom.xml files (excludes target/, node_modules/, maven-script/)
   - Calls Java analyzer via Maven exec:java
   - Converts Java output to Nx project configurations
   - Maps Maven projects to Nx format

2. **Key Data Transformations**:
   - `project.basedir` → Nx project root
   - `project.artifactId` → Nx project name
   - `project.targets` → Nx target configurations
   - `project.dependencies` → Internal workspace dependencies

3. **Target Generation**:
   - Maven lifecycle phases (clean, compile, test, package, etc.)
   - Plugin-specific goals (quarkus:dev, spring-boot:run, etc.)
   - Pre-calculated dependencies between targets

4. **CreateDependencies Implementation**:
   - Currently returns empty array
   - Dependencies handled via target `dependsOn` configuration

### Current Target Structure

Each generated target has:
- `executor`: Always "@nx/run-commands:run-commands"
- `options`: Command and working directory
- `inputs`: File patterns that affect the target
- `outputs`: Generated file patterns
- `dependsOn`: Other targets this depends on
- `metadata`: Maven-specific information

### Plugin Recognition
The Java analyzer recognizes these plugin types:
- Quarkus plugins (`io.quarkus`)
- Spring Boot plugins (`org.springframework.boot`)
- Maven core plugins (compiler, surefire, failsafe, etc.)
- Packaging plugins (jar, war)
- Utility plugins (clean, install, deploy, site)

### Current Data Flow
1. TypeScript → Java via Maven exec
2. Java reads pom.xml files recursively
3. Java generates comprehensive target configurations
4. Java outputs JSON to file
5. TypeScript reads JSON file
6. TypeScript converts to CreateNodesV2 format
7. Nx uses the configurations

## Areas for CreateNodesV2/CreateDependencies Integration

### For CreateNodesV2
- Output format is mostly compatible
- Need to ensure proper tuple format: `[configFile, result]`
- Need to handle project filtering and batching

### For CreateDependencies  
- Currently unused - dependencies via `dependsOn`
- Could extract cross-project dependencies from Maven analysis
- Could create explicit dependency graph relationships