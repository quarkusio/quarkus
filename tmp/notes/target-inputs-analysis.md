# Maven Plugin Target Inputs Analysis

## Overview
This document analyzes the input patterns used across generated Maven targets in the Nx workspace. The analysis examines how the Maven plugin determines what files should trigger target re-execution when changed.

## Key Input Patterns Observed

### 1. Universal Inputs
All targets include these basic inputs:
- `{projectRoot}/pom.xml` - Maven project descriptor

### 2. Source-Dependent Targets
Targets that work with source code include:
- `{projectRoot}/src/**/*` - All source files (main and test)

Examples:
- `maven-compiler:compile`
- `maven-compiler:testCompile` 
- `quarkus-extension:dev`
- `quarkus-extension:build`
- `maven-surefire:test`
- `maven-resources:testResources`

### 3. POM-Only Targets
Some targets only depend on the POM file:
- `maven-enforcer:enforce`
- `buildnumber:create`
- `maven-source:jar-no-fork`
- `maven-clean:clean`
- `formatter:format`
- `impsort:sort`
- Most Maven lifecycle goals (install, deploy, etc.)

## Input Categorization

### Build Lifecycle Targets
- **Compilation**: Include `src/**/*` - need source files
- **Testing**: Include `src/**/*` - need both main and test sources
- **Packaging**: Typically POM-only - work with already compiled artifacts
- **Validation**: POM-only - validate project configuration

### Plugin-Specific Patterns
- **Quarkus Extension Plugin**: Always includes `src/**/*` for dev and build goals
- **Maven Resources Plugin**: Includes `src/**/*` for resource processing
- **Code Formatters**: POM-only - configuration driven
- **Site Generation**: POM-only - uses project metadata

## Insights

1. **Smart Caching**: The plugin intelligently determines which files affect each target
2. **Granular Dependencies**: Targets only depend on files they actually need
3. **Consistent Patterns**: Similar Maven goals follow consistent input patterns across projects

## Examples from Sample Projects

All three analyzed projects (hibernate-orm-panache, messaging-kafka, rest-client-jackson) show identical input patterns:

- **Compilation targets**: `{projectRoot}/pom.xml` + `{projectRoot}/src/**/*`
- **Test targets**: `{projectRoot}/pom.xml` + `{projectRoot}/src/**/*` 
- **Package targets**: `{projectRoot}/pom.xml` only
- **Lifecycle phases**: No direct inputs (depend on underlying goals)

This consistency demonstrates the Maven plugin's systematic approach to input detection.