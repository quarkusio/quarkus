# Maven Analyzer Nx Integration Plan

## Overview
Plan to modify the Java MavenAnalyzer to output data structures that match Nx's CreateNodesV2 and CreateDependencies TypeScript interfaces exactly.

## Current State Analysis

### Java MavenAnalyzer Structure (Good Foundation)
- **Location**: `maven-plugin-v2/src/main/java/MavenAnalyzer.java`
- **Current Output**: Custom JSON format with projects array
- **Strengths**: 
  - Already generates proper target configurations
  - Handles Maven lifecycle phases and plugin goals
  - Smart input/output detection
  - Proper dependency analysis

### Required TypeScript Interface Compliance

#### CreateNodesV2 Return Format
```typescript
Array<readonly [configFileSource: string, result: CreateNodesResult]>
```

Where `CreateNodesResult` contains:
```typescript
{
  projects?: Record<string, ProjectConfiguration>;
  externalNodes?: Record<string, ProjectGraphExternalNode>;
}
```

#### CreateDependencies Return Format
```typescript
RawProjectGraphDependency[]
```

## Implementation Plan

### Phase 1: Modify Java Output Structure

#### Current Java Output:
```json
{
  "rootProject": "/workspace/path",
  "projects": [
    {
      "id": "groupId:artifactId",
      "artifactId": "project-name", 
      "groupId": "org.example",
      "dependencies": [...],
      "plugins": [...],
      "targets": { ... }
    }
  ]
}
```

#### Required CreateNodesV2 Output:
```json
[
  [
    "/workspace/path/pom.xml",
    {
      "projects": {
        "project-name": {
          "root": ".",
          "targets": { ... },
          "metadata": {
            "groupId": "org.example",
            "artifactId": "project-name"
          }
        }
      }
    }
  ],
  [
    "/workspace/path/sub-module/pom.xml", 
    {
      "projects": {
        "sub-module": {
          "root": "sub-module",
          "targets": { ... }
        }
      }
    }
  ]
]
```

### Phase 2: Key Changes Required

1. **Output Format Transformation**:
   - Change from projects array to tuple array format
   - Each tuple: `[pomFilePath, CreateNodesResult]`
   - Project name as key instead of nested object

2. **Project Root Calculation**:
   - Convert absolute paths to workspace-relative paths
   - Use project directory name as project identifier
   - Calculate proper `root` field relative to workspace

3. **Metadata Restructuring**:
   - Move Maven-specific data (groupId, artifactId, version) to `metadata` field
   - Keep only Nx-required fields at top level

4. **Dependencies Separation**:
   - Extract inter-project dependencies as separate CreateDependencies output
   - Convert Maven dependencies to RawProjectGraphDependency format

### Phase 3: Target Structure Compliance

Current target structure is already compliant:
```json
{
  "executor": "@nx/run-commands:run-commands",
  "options": { "command": "mvn compile", "cwd": "{projectRoot}" },
  "inputs": ["{projectRoot}/pom.xml", "{projectRoot}/src/**/*"],
  "outputs": ["{projectRoot}/target/classes/**/*"],
  "metadata": { "type": "phase", "technologies": ["maven"] }
}
```

### Phase 4: CreateDependencies Implementation

Extract cross-project Maven dependencies:
```json
[
  {
    "source": "parent-project",
    "target": "child-module", 
    "type": "static",
    "sourceFile": "pom.xml"
  }
]
```

## Implementation Details

### Java Class Modifications

1. **Main Method Changes**:
   - Accept multiple pom.xml files as input (batch processing)
   - Output CreateNodesV2 format directly

2. **New Methods Needed**:
   ```java
   // Convert current format to CreateNodesV2 tuples
   private List<Object[]> generateCreateNodesV2Results(Map<String, Object> analysis)
   
   // Extract cross-project dependencies
   private List<Map<String, Object>> generateCreateDependencies(Map<String, Object> analysis)
   
   // Calculate workspace-relative paths
   private String calculateRelativePath(File workspaceRoot, File projectDir)
   ```

3. **Output Structure**:
   ```java
   Map<String, Object> output = new LinkedHashMap<>();
   output.put("createNodesResults", generateCreateNodesV2Results(analysis));
   output.put("createDependencies", generateCreateDependencies(analysis));
   ```

### TypeScript Integration

The existing `maven-plugin2.ts` will need minimal changes since it already processes Java output correctly. Main adjustment will be reading the new JSON structure format.

## Benefits of This Approach

1. **Exact Nx Compliance**: Output matches TypeScript interfaces exactly
2. **Batch Efficiency**: Java processes multiple pom.xml files at once
3. **Minimal TypeScript Changes**: Leverage existing integration layer
4. **Comprehensive Analysis**: Maintains all current Maven analysis capabilities

## Next Steps

1. Modify MavenAnalyzer.java output format
2. Test with existing Maven projects 
3. Update TypeScript integration if needed
4. Validate with Nx workspace requirements