# Nx TypeScript Type Definitions for Java MavenAnalyzer

## Core Plugin Interface Types

### CreateNodesV2

```typescript
export type CreateNodesV2<T = unknown> = readonly [
    projectFilePattern: string,
    createNodesFunction: CreateNodesFunctionV2<T>
];

export type CreateNodesFunctionV2<T = unknown> = (
    projectConfigurationFiles: readonly string[], 
    options: T | undefined, 
    context: CreateNodesContextV2
) => CreateNodesResultV2 | Promise<CreateNodesResultV2>;

export type CreateNodesResultV2 = Array<readonly [configFileSource: string, result: CreateNodesResult]>;
```

**Key Points:**
- Returns an array of tuples: `[configFile, projectConfig]`
- Each tuple maps a pom.xml file to its project configuration
- Processes multiple files at once (batch processing)

### CreateDependencies

```typescript
export type CreateDependencies<T = unknown> = (
    options: T | undefined, 
    context: CreateDependenciesContext
) => RawProjectGraphDependency[] | Promise<RawProjectGraphDependency[]>;
```

**Key Points:**
- Returns array of dependencies between projects
- Used for cross-project dependency analysis

### Context Types

```typescript
export interface CreateNodesContextV2 {
    readonly nxJsonConfiguration: NxJsonConfiguration;
    readonly workspaceRoot: string;
}

export interface CreateDependenciesContext {
    readonly externalNodes: ProjectGraph['externalNodes'];
    readonly projects: Record<string, ProjectConfiguration>;
    readonly nxJsonConfiguration: NxJsonConfiguration;
    readonly fileMap: FileMap;
    readonly filesToProcess: FileMap;
    readonly workspaceRoot: string;
}
```

## Return Type Structures

### CreateNodesResult

```typescript
export interface CreateNodesResult {
    projects?: Record<string, Optional<ProjectConfiguration, 'root'>>;
    externalNodes?: Record<string, ProjectGraphExternalNode>;
}
```

### ProjectConfiguration

```typescript
export interface ProjectConfiguration {
    name?: string;
    targets?: {
        [targetName: string]: TargetConfiguration;
    };
    root: string;
    sourceRoot?: string;
    projectType?: ProjectType; // 'library' | 'application'
    generators?: {
        [collectionName: string]: {
            [generatorName: string]: any;
        };
    };
    implicitDependencies?: string[];
    namedInputs?: {
        [inputName: string]: (string | InputDefinition)[];
    };
    tags?: string[];
    release?: {
        version?: any; // Complex type omitted for brevity
    };
    metadata?: ProjectMetadata;
}
```

### TargetConfiguration

```typescript
export interface TargetConfiguration<T = any> {
    executor?: string;
    command?: string;
    outputs?: string[];
    dependsOn?: (TargetDependencyConfig | string)[];
    inputs?: (InputDefinition | string)[];
    options?: T;
    configurations?: {
        [config: string]: any;
    };
    defaultConfiguration?: string;
    cache?: boolean;
    metadata?: TargetMetadata;
    parallelism?: boolean;
    continuous?: boolean;
    syncGenerators?: string[];
}
```

### ProjectMetadata

```typescript
export interface ProjectMetadata {
    description?: string;
    technologies?: string[];
    targetGroups?: Record<string, string[]>;
    owners?: {
        [ownerId: string]: {
            ownedFiles: {
                files: ['*'] | string[];
                fromConfig?: {
                    filePath: string;
                    location: {
                        startLine: number;
                        endLine: number;
                    };
                };
            }[];
        };
    };
    js?: {
        packageName: string;
        packageExports?: any;
        packageMain?: string;
        isInPackageManagerWorkspaces?: boolean;
    };
}
```

### RawProjectGraphDependency

```typescript
export type RawProjectGraphDependency = ImplicitDependency | StaticDependency | DynamicDependency;

export type StaticDependency = {
    source: string;
    target: string;
    sourceFile?: string;
    type: typeof DependencyType.static;
};

export type DynamicDependency = {
    source: string;
    target: string;
    sourceFile: string;
    type: typeof DependencyType.dynamic;
};

export type ImplicitDependency = {
    source: string;  
    target: string;
    type: typeof DependencyType.implicit;
};

export enum DependencyType {
    static = "static",
    dynamic = "dynamic", 
    implicit = "implicit"
}
```

## Java Data Structure Mapping

### For CreateNodesV2 Return

```java
// Return type: List<Tuple<String, ProjectResult>>
public class ProjectResult {
    public Map<String, ProjectConfig> projects;
    public Map<String, ExternalNode> externalNodes; // optional
}

public class ProjectConfig {
    public String name;                              // optional
    public Map<String, TargetConfig> targets;       // optional
    public String root;                              // required
    public String sourceRoot;                        // optional  
    public String projectType;                       // "library" | "application"
    public List<String> implicitDependencies;       // optional
    public List<String> tags;                        // optional
    public ProjectMetadata metadata;                 // optional
}

public class TargetConfig {
    public String executor;                          // e.g. "@nx/run-commands:run-commands"
    public String command;                           // optional shorthand
    public List<String> outputs;                     // e.g. ["{projectRoot}/target/**/*"]
    public List<String> dependsOn;                   // other targets this depends on
    public List<String> inputs;                      // e.g. ["{projectRoot}/pom.xml", "{projectRoot}/src/**/*"]
    public Map<String, Object> options;             // executor options
    public Map<String, Object> configurations;      // optional
    public TargetMetadata metadata;                  // optional
}

public class TargetMetadata {
    public String description;
    public List<String> technologies;               // e.g. ["maven", "java"]
    public String type;                             // e.g. "goal", "phase"
    public String plugin;                           // e.g. "org.apache.maven.plugins:maven-compiler-plugin"
    public String goal;                             // e.g. "compile"
}
```

### For CreateDependencies Return

```java
// Return type: List<ProjectDependency>
public class ProjectDependency {
    public String source;        // project name that depends on target
    public String target;        // project name being depended on  
    public String type;          // "static", "dynamic", or "implicit"
    public String sourceFile;    // optional: file where dependency is declared
}
```

## Example Implementation Structure

```java
public class MavenAnalyzer {
    // Main entry for CreateNodesV2
    public List<Tuple<String, ProjectResult>> analyzeProjects(List<String> pomFiles) {
        List<Tuple<String, ProjectResult>> results = new ArrayList<>();
        
        for (String pomFile : pomFiles) {
            ProjectResult result = new ProjectResult();
            result.projects = new HashMap<>();
            
            // Analyze pom.xml and create ProjectConfig
            ProjectConfig config = analyzePom(pomFile);
            String projectRoot = getProjectRoot(pomFile);
            result.projects.put(projectRoot, config);
            
            results.add(new Tuple<>(pomFile, result));
        }
        
        return results;
    }
    
    // Main entry for CreateDependencies  
    public List<ProjectDependency> analyzeDependencies(Map<String, ProjectConfig> projects) {
        List<ProjectDependency> dependencies = new ArrayList<>();
        
        // Analyze Maven dependencies and create cross-project dependencies
        for (Map.Entry<String, ProjectConfig> entry : projects.entrySet()) {
            dependencies.addAll(extractDependencies(entry.getKey(), entry.getValue(), projects));
        }
        
        return dependencies;
    }
}
```

## Key Requirements for Java Implementation

1. **Batch Processing**: CreateNodesV2 processes multiple pom.xml files at once
2. **Tuple Return Format**: Must return `[(configFile, result), ...]` pairs
3. **Target Structure**: Each target needs executor, options, inputs, outputs, metadata
4. **Dependency Analysis**: CreateDependencies analyzes inter-project dependencies
5. **Path Handling**: All paths should be relative to workspace root
6. **Maven Integration**: Must extract plugin goals, phases, and dependencies from pom.xml