# CreateDependencies Java Type Mapping

## TypeScript Interface
```typescript
type CreateDependenciesFunction<T = any> = (
  options: T | undefined,
  context: CreateDependenciesContext
) => RawProjectGraphDependency[];

interface RawProjectGraphDependency {
  source: string;
  target: string; 
  type: 'static' | 'dynamic' | 'implicit';
  sourceFile?: string;
}
```

## Java Implementation Strategy

### Dependency Types in Maven Context

1. **Static Dependencies**: Direct Maven dependencies between projects in workspace
   - Parent-child module relationships
   - Explicit dependencies in pom.xml between workspace projects

2. **Implicit Dependencies**: Framework-inferred relationships
   - Common parent pom inheritance
   - Shared plugin configurations

3. **Dynamic Dependencies**: Runtime-discovered dependencies
   - Less common in Maven, but could include test dependencies

### Java Output Format

```java
public List<Map<String, Object>> generateCreateDependencies(Map<String, Object> analysis) {
    List<Map<String, Object>> dependencies = new ArrayList<>();
    
    // Extract workspace projects
    List<Map<String, Object>> projects = (List<Map<String, Object>>) analysis.get("projects");
    Map<String, String> artifactToProject = buildArtifactMapping(projects);
    
    for (Map<String, Object> project : projects) {
        String source = getProjectName(project);
        String sourceFile = getRelativePomPath(project);
        
        // 1. Static dependencies - direct Maven dependencies
        addStaticDependencies(dependencies, project, artifactToProject, sourceFile);
        
        // 2. Implicit dependencies - parent/module relationships  
        addImplicitDependencies(dependencies, project, projects, source);
    }
    
    return dependencies;
}
```

### Static Dependencies (Maven Dependencies)
```java
private void addStaticDependencies(List<Map<String, Object>> dependencies, 
                                   Map<String, Object> project,
                                   Map<String, String> artifactToProject,
                                   String sourceFile) {
    List<Map<String, Object>> deps = (List<Map<String, Object>>) project.get("dependencies");
    String source = getProjectName(project);
    
    for (Map<String, Object> dep : deps) {
        String groupId = (String) dep.get("groupId");
        String artifactId = (String) dep.get("artifactId");
        String depKey = groupId + ":" + artifactId;
        
        // Check if this dependency refers to another project in workspace
        String target = artifactToProject.get(depKey);
        if (target != null && !target.equals(source)) {
            Map<String, Object> dependency = new LinkedHashMap<>();
            dependency.put("source", source);
            dependency.put("target", target);
            dependency.put("type", "static");
            dependency.put("sourceFile", sourceFile);
            dependencies.add(dependency);
        }
    }
}
```

### Implicit Dependencies (Module Structure)
```java
private void addImplicitDependencies(List<Map<String, Object>> dependencies,
                                     Map<String, Object> project, 
                                     List<Map<String, Object>> allProjects,
                                     String source) {
    // Find parent-child relationships
    File projectDir = new File((String) project.get("basedir"));
    
    for (Map<String, Object> otherProject : allProjects) {
        File otherDir = new File((String) otherProject.get("basedir"));
        String target = getProjectName(otherProject);
        
        if (!source.equals(target)) {
            // Check if one is parent of the other
            if (isParentChildRelation(projectDir, otherDir)) {
                Map<String, Object> dependency = new LinkedHashMap<>();
                dependency.put("source", source);
                dependency.put("target", target);
                dependency.put("type", "implicit");
                dependencies.add(dependency);
            }
        }
    }
}
```

### Helper Methods
```java
private Map<String, String> buildArtifactMapping(List<Map<String, Object>> projects) {
    Map<String, String> mapping = new HashMap<>();
    for (Map<String, Object> project : projects) {
        String groupId = (String) project.get("groupId");
        String artifactId = (String) project.get("artifactId");
        String projectName = getProjectName(project);
        String key = groupId + ":" + artifactId;
        mapping.put(key, projectName);
    }
    return mapping;
}

private String getProjectName(Map<String, Object> project) {
    // Use directory name as project name for Nx
    String basedir = (String) project.get("basedir");
    return new File(basedir).getName();
}

private String getRelativePomPath(Map<String, Object> project) {
    String basedir = (String) project.get("basedir");
    return new File(basedir).getName() + "/pom.xml";
}

private boolean isParentChildRelation(File dir1, File dir2) {
    try {
        String path1 = dir1.getCanonicalPath();
        String path2 = dir2.getCanonicalPath();
        return path1.startsWith(path2) || path2.startsWith(path1);
    } catch (IOException e) {
        return false;
    }
}
```

## Expected Output Example

```json
[
  {
    "source": "parent-project",
    "target": "child-module",
    "type": "implicit"
  },
  {
    "source": "web-app", 
    "target": "shared-lib",
    "type": "static",
    "sourceFile": "web-app/pom.xml"
  },
  {
    "source": "integration-tests",
    "target": "core-service", 
    "type": "static",
    "sourceFile": "integration-tests/pom.xml"
  }
]
```

## Integration with Main Analyzer

```java
public Map<String, Object> analyze(File projectRoot) throws Exception {
    List<Map<String, Object>> projects = new ArrayList<>();
    collectProjects(projectRoot, projects);
    
    Map<String, Object> analysis = new LinkedHashMap<>();
    analysis.put("rootProject", projectRoot.getAbsolutePath());
    analysis.put("projects", projects);
    
    // Generate Nx-compatible outputs
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("createNodesResults", generateCreateNodesV2Results(analysis));
    result.put("createDependencies", generateCreateDependencies(analysis));
    
    return result;
}
```

This approach ensures Maven project relationships are properly represented as Nx project graph dependencies.