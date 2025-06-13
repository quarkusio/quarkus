# Implementation Strategy for MavenAnalyzer Nx Integration

## Overview
Complete strategy for modifying the Java MavenAnalyzer to output CreateNodesV2 and CreateDependencies compatible results.

## Key Implementation Changes

### 1. Main Method Modifications

#### Current Flow:
```java
public static void main(String[] args) {
    // Single project path input
    String projectPath = args[0];
    MavenAnalyzer analyzer = new MavenAnalyzer();
    Map<String, Object> result = analyzer.analyze(projectRoot);
    analyzer.writeResult(result, outputPath);
}
```

#### Required Flow:
```java
public static void main(String[] args) {
    // Multiple pom.xml files as input (batch processing)
    List<String> pomFiles = Arrays.asList(args);
    MavenAnalyzer analyzer = new MavenAnalyzer();
    Map<String, Object> result = analyzer.analyzeForNx(pomFiles);
    analyzer.writeResult(result, outputPath);
}
```

### 2. New Core Methods

#### analyzeForNx Method
```java
public Map<String, Object> analyzeForNx(List<String> pomFiles) throws Exception {
    // Find common workspace root
    File workspaceRoot = findWorkspaceRoot(pomFiles);
    
    // Analyze all projects
    List<Map<String, Object>> projects = new ArrayList<>();
    for (String pomFile : pomFiles) {
        File projectDir = new File(pomFile).getParentFile();
        collectProjects(projectDir, projects);
    }
    
    // Generate Nx-compatible outputs
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("createNodesResults", generateCreateNodesV2Results(projects, workspaceRoot));
    result.put("createDependencies", generateCreateDependencies(projects, workspaceRoot));
    
    return result;
}
```

#### generateCreateNodesV2Results Method
```java
private List<Object[]> generateCreateNodesV2Results(List<Map<String, Object>> projects, File workspaceRoot) {
    List<Object[]> results = new ArrayList<>();
    
    for (Map<String, Object> project : projects) {
        String basedir = (String) project.get("basedir");
        File projectDir = new File(basedir);
        File pomFile = new File(projectDir, "pom.xml");
        
        // Create tuple: [pomFilePath, CreateNodesResult]
        String pomPath = getRelativePath(workspaceRoot, pomFile);
        Map<String, Object> createNodesResult = generateCreateNodesResult(project, workspaceRoot);
        
        results.add(new Object[]{pomPath, createNodesResult});
    }
    
    return results;
}
```

#### generateCreateNodesResult Method
```java
private Map<String, Object> generateCreateNodesResult(Map<String, Object> project, File workspaceRoot) {
    String basedir = (String) project.get("basedir");
    File projectDir = new File(basedir);
    String projectName = projectDir.getName();
    String projectRoot = getRelativePath(workspaceRoot, projectDir);
    
    // Create ProjectConfiguration
    Map<String, Object> projectConfig = new LinkedHashMap<>();
    projectConfig.put("root", projectRoot.isEmpty() ? "." : projectRoot);
    
    // Add targets (already in correct format)
    Map<String, Object> targets = (Map<String, Object>) project.get("targets");
    if (targets != null && !targets.isEmpty()) {
        projectConfig.put("targets", targets);
    }
    
    // Add metadata
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("groupId", project.get("groupId"));
    metadata.put("artifactId", project.get("artifactId"));
    metadata.put("version", project.get("version"));
    metadata.put("packaging", project.get("packaging"));
    projectConfig.put("metadata", metadata);
    
    // Create CreateNodesResult
    Map<String, Object> result = new LinkedHashMap<>();
    Map<String, Object> projectsMap = new LinkedHashMap<>();
    projectsMap.put(projectName, projectConfig);
    result.put("projects", projectsMap);
    
    return result;
}
```

### 3. Helper Methods

#### Workspace Root Detection
```java
private File findWorkspaceRoot(List<String> pomFiles) {
    File commonRoot = null;
    
    for (String pomFile : pomFiles) {
        File file = new File(pomFile).getAbsoluteFile();
        File dir = file.getParentFile();
        
        if (commonRoot == null) {
            commonRoot = dir;
        } else {
            commonRoot = findCommonAncestor(commonRoot, dir);
        }
    }
    
    return commonRoot;
}

private File findCommonAncestor(File dir1, File dir2) {
    try {
        String path1 = dir1.getCanonicalPath();
        String path2 = dir2.getCanonicalPath();
        
        while (!path2.startsWith(path1)) {
            dir1 = dir1.getParentFile();
            if (dir1 == null) return new File("/");
            path1 = dir1.getCanonicalPath();
        }
        
        return dir1;
    } catch (IOException e) {
        return new File("/");
    }
}
```

#### Path Utilities
```java
private String getRelativePath(File workspaceRoot, File target) {
    try {
        String workspacePath = workspaceRoot.getCanonicalPath();
        String targetPath = target.getCanonicalPath();
        
        if (targetPath.startsWith(workspacePath)) {
            String relative = targetPath.substring(workspacePath.length());
            if (relative.startsWith("/")) {
                relative = relative.substring(1);
            }
            return relative;
        }
        
        return targetPath;
    } catch (IOException e) {
        return target.getAbsolutePath();
    }
}
```

### 4. Target Path Adjustments

Update target generation to use workspace-relative paths:

```java
private Map<String, Object> generatePhaseTargets(Model model, File workspaceRoot, File projectDir) {
    Map<String, Object> phaseTargets = new LinkedHashMap<>();
    String projectRoot = getRelativePath(workspaceRoot, projectDir);
    String projectRootToken = projectRoot.isEmpty() ? "." : projectRoot;
    
    for (String phase : phases) {
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("executor", "@nx/run-commands:run-commands");
        
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("command", "mvn " + phase);
        options.put("cwd", projectRootToken);
        target.put("options", options);
        
        // Update input/output paths to use project root token
        List<String> inputs = new ArrayList<>();
        inputs.add(projectRootToken + "/pom.xml");
        // ... rest of inputs/outputs with proper paths
        
        phaseTargets.put(phase, target);
    }
    
    return phaseTargets;
}
```

### 5. Integration Points

#### TypeScript Integration
The existing `maven-plugin2.ts` will receive the new format:

```typescript
const result = await runMavenAnalysis(pomFiles);
const { createNodesResults, createDependencies } = JSON.parse(result);

// Process createNodesResults (already tuples)
return createNodesResults;

// For createDependencies function
return createDependencies;
```

#### Command Line Interface
```bash
java -jar maven-analyzer.jar \
  /workspace/pom.xml \
  /workspace/module1/pom.xml \
  /workspace/module2/pom.xml \
  --output /workspace/maven-analysis.json
```

### 6. Output Format Example

```json
{
  "createNodesResults": [
    [
      "pom.xml",
      {
        "projects": {
          "root-project": {
            "root": ".",
            "targets": { "clean": {...}, "compile": {...} },
            "metadata": { "groupId": "com.example", "artifactId": "root-project" }
          }
        }
      }
    ],
    [
      "module1/pom.xml", 
      {
        "projects": {
          "module1": {
            "root": "module1",
            "targets": { "clean": {...}, "test": {...} },
            "metadata": { "groupId": "com.example", "artifactId": "module1" }
          }
        }
      }
    ]
  ],
  "createDependencies": [
    {
      "source": "module1",
      "target": "root-project", 
      "type": "implicit"
    }
  ]
}
```

## Benefits

1. **Exact Nx Compliance**: Output matches TypeScript interfaces perfectly
2. **Efficient Batch Processing**: Single Java invocation for all projects
3. **Proper Path Handling**: Workspace-relative paths throughout
4. **Minimal TypeScript Changes**: Leverages existing integration patterns
5. **Complete Maven Analysis**: Preserves all current functionality

## Implementation Priority

1. **High**: Core method restructuring (analyzeForNx, generateCreateNodesV2Results)
2. **High**: Path calculation utilities (workspace root detection, relative paths)
3. **Medium**: CreateDependencies implementation
4. **Low**: Command line interface adjustments