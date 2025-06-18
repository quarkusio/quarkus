# Nx Maven Plugin Development

This repository contains an Nx plugin for Maven integration that enables Nx to work seamlessly with Maven projects while providing enhanced caching and parallelism.

## Project Overview

**Goal**: Develop a Maven plugin that allows Nx to invoke Maven goals exactly as Maven does, but with Nx's advanced caching and parallel execution capabilities.

## Architecture

### Core Components

1. **TypeScript Graph Plugin** (`maven-plugin/maven-plugin.ts`)
   - Main entry point for Nx integration
   - Generates project graph using Maven workspace analysis
   - Interfaces with Maven APIs through the mojo analyzer

2. **Mojo Analyzer** (`./maven-plugin/`)
   - Java-based component using official Maven APIs
   - Analyzes Maven projects and dependencies
   - Provides structured data about Maven workspace to Nx

3. **Batch Maven Executor**
   - Uses Maven APIs to execute Maven targets
   - Handles parallel execution with proper Maven session context
   - Maintains Maven's exact execution behavior

## Development Principles

- **Never hardcode**: Always use Maven APIs to retrieve project information
- **Maven Compatibility**: Execute Maven goals exactly as Maven would
- **API-First**: Leverage official Maven APIs for all Maven interactions
- **Nx Enhancement**: Add caching and parallelism without changing Maven behavior

## Testing Commands

```bash
# Verify plugin functionality
nx show projects

# Generate project graph
nx graph --file graph.json

# View detailed project information
nx show projects --verbose
```

## Development Workflow

When making changes to the Maven plugin:

```bash
# 1. Recompile the Java components
npm run compile-java

# 2. Reset Nx state to pick up changes
nx reset

# 3. Test your changes
nx show projects
```

## Prerequisites

- Nx CLI (v21.1.3+)
- Maven (3.9.9+)
- Java Development Kit
- Compiled plugin: `/maven-plugin/target/classes/`

## Test Project Context

The Quarkus application code in this repository serves solely as a test case for the Maven plugin. The actual Quarkus implementation details are not relevant to the plugin development - it's simply a real-world Maven project used to validate that the Nx Maven plugin works correctly with various Maven configurations and dependencies.