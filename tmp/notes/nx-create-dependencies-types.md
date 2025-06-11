# Nx DevKit CreateDependencies Type Definitions

## Overview
This document outlines the complete type definitions for the `CreateDependencies` function in Nx DevKit. This is essential for creating Nx plugins that analyze workspace files and establish project dependencies.

## Core Type Signature

```typescript
export type CreateDependencies<T = unknown> = (
  options: T | undefined, 
  context: CreateDependenciesContext
) => RawProjectGraphDependency[] | Promise<RawProjectGraphDependency[]>
```

## CreateDependenciesContext Interface

The context provides access to all workspace information needed to analyze dependencies:

```typescript
export interface CreateDependenciesContext {
  /**
   * The external nodes that have been added to the graph.
   */
  readonly externalNodes: ProjectGraph['externalNodes'];
  
  /**
   * The configuration of each project in the workspace keyed by project name.
   */
  readonly projects: Record<string, ProjectConfiguration>;
  
  /**
   * The `nx.json` configuration from the workspace
   */
  readonly nxJsonConfiguration: NxJsonConfiguration;
  
  /**
   * All files in the workspace
   */
  readonly fileMap: FileMap;
  
  /**
   * Files changes since last invocation
   */
  readonly filesToProcess: FileMap;
  
  readonly workspaceRoot: string;
}
```

## RawProjectGraphDependency Types

A union type of three dependency types:

```typescript
export type RawProjectGraphDependency = ImplicitDependency | StaticDependency | DynamicDependency;
```

### StaticDependency
For dependencies that ALWAYS load the target project:

```typescript
export type StaticDependency = {
  source: string;     // Source project name
  target: string;     // Target project name  
  sourceFile?: string; // File path where dependency is made
  type: typeof DependencyType.static;
};
```

### DynamicDependency  
For dependencies that MAY OR MAY NOT load the target project:

```typescript
export type DynamicDependency = {
  source: string;     // Source project name
  target: string;     // Target project name
  sourceFile: string; // Required for dynamic deps
  type: typeof DependencyType.dynamic;
};
```

### ImplicitDependency
For inferred connections without explicit code references:

```typescript
export type ImplicitDependency = {
  source: string;     // Source project name
  target: string;     // Target project name
  type: typeof DependencyType.implicit;
};
```

## DependencyType Enum

```typescript
export declare enum DependencyType {
  static = "static",    // Tied to module loading
  dynamic = "dynamic",  // Brought in at runtime
  implicit = "implicit" // Inferred dependencies
}
```

## Implementation Pattern

The function should follow this pattern:

```typescript
export const createDependencies: CreateDependencies = [
  'file-pattern',
  async (options, context) => {
    const dependencies: RawProjectGraphDependency[] = [];
    
    // Use context.filesToProcess for performance
    for (const file of context.filesToProcess) {
      // Analyze files and create dependencies
      dependencies.push({
        source: 'source-project',
        target: 'target-project', 
        dependencyType: DependencyType.static,
        sourceFile: file
      });
    }
    
    return dependencies;
  }
];
```

## Key Points

1. **Performance**: Use `context.filesToProcess` instead of analyzing all files
2. **Validation**: Use `validateDependency()` to validate dependencies
3. **Return Type**: Can return array synchronously or Promise<array> asynchronously
4. **Source Files**: StaticDependency requires sourceFile unless source is external node
5. **Dynamic Dependencies**: Always require sourceFile parameter

## Example Usage in Current Implementation

The Maven plugin implementation shows proper usage:

```typescript
export const createDependencies: CreateDependencies = [
  MAVEN_CONFIG_GLOB,
  async (options, context) => {
    const files = context.filesToProcess;
    // Process POM files to create Maven dependencies
    const dependencies = [];
    
    for (const pomFile of pomFiles) {
      dependencies.push({
        sourceProjectName: sourceProject,
        targetProjectName: targetProject, 
        dependencyType: 'static' as const,
        source: pomFile,
      });
    }
    
    return dependencies;
  }
];
```