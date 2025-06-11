# Maven Plugin Process Bombing Fix

## Problem
The maven-plugin.ts was creating excessive child processes when discovering plugin goals. Each unknown plugin would spawn a `mvn help:describe` command simultaneously, causing system overload.

## Solution
Implemented a `PluginDiscoveryPool` class that:

1. **Limits concurrent processes** to 6 maximum
2. **Queues discovery requests** to prevent overwhelming the system
3. **Adds timeouts** (8 seconds) to prevent hanging processes
4. **Maintains caching** for previously discovered plugins
5. **Proper process cleanup** with SIGTERM/SIGKILL handling

## Key Changes
- Added `PluginDiscoveryPool` class with controlled concurrency
- Replaced `exec` with `spawn` for better process control
- Added comprehensive error handling and process cleanup
- Improved Maven output parsing with multiple patterns
- Added process exit handlers for cleanup
- Optimized Maven flags for faster execution
- Added Maven Daemon support detection

## Performance Optimizations
- Check for Maven Daemon once, not per plugin
- Skip discovery for common plugins that don't need it
- Use offline mode and optimized JVM settings
- Deduplicate plugins before discovery

This prevents the "process bombing" while maintaining dynamic plugin discovery functionality.

## Final Fix: Recursive Module Discovery

The plugin was finding 1326 projects by globbing all pom.xml files, but should only find actual Maven projects listed in module declarations.

### Solution
- Implemented `discoverMavenModules` function that recursively follows Maven module structure starting from root pom.xml
- Fixed path handling bug in `parsePomXml` where absolute paths were incorrectly joined with workspace root
- Updated both TypeScript and JavaScript versions to handle absolute paths correctly

### Results
- Reduced from 1326 globbed files to 949 actual Maven projects  
- Now follows proper Maven module hierarchy
- Much more accurate project discovery that respects Maven's structure

## Potential Enhancement: Effective POM Integration

### Current Issue
Dependencies may not be resolving correctly because we parse raw POMs which:
- Don't include resolved versions from dependency management
- Miss inherited plugin configurations from parent POMs
- Have unresolved property placeholders

### Effective POM Benefits
- **Resolved Dependencies**: Get actual versions, not placeholders like `${version}`
- **Complete Plugin Configs**: Full inherited plugin setup from parents
- **Property Resolution**: All `${property}` variables resolved to values
- **Accurate Build Info**: Complete lifecycle and plugin bindings

### Implementation Options
1. **Selective Usage**: Only use effective-pom for dependency resolution, not discovery
2. **Batch Processing**: Generate effective POMs for multiple projects at once
3. **Caching**: Cache effective POM results to avoid repeated computation
4. **Hybrid Approach**: Use raw POMs for discovery, effective POMs for dependencies

### Recommended Approach
Use effective-pom selectively in `createDependencies` function:
```bash
mvn help:effective-pom -f {pom} -Doutput={cache-file} -q
```
This would give more accurate dependency resolution while keeping fast discovery.

## IMPLEMENTED: TypeScript Effective POM Resolver

### What Was Built
Created a pure TypeScript implementation of effective POM resolution:
- **Property Resolution**: Resolves `${property}` placeholders from parent POMs
- **Parent POM Inheritance**: Recursively merges configurations from parent hierarchy
- **Dependency Management**: Resolves versions from `<dependencyManagement>` sections
- **Version Resolution**: Gets actual versions instead of placeholders

### Benefits Over Maven CLI
- **Performance**: No subprocess overhead, much faster than `mvn help:effective-pom`
- **Reliability**: No timeouts or memory issues with large projects
- **Caching**: Built-in caching at multiple levels
- **Selective Usage**: Only use for projects that need complex inheritance

### Test Results
Arc runtime project now shows:
- 6 resolved dependencies with actual versions
- `jakarta.enterprise:jakarta.enterprise.cdi-api:4.1.0` (was missing version)
- `jakarta.annotation:jakarta.annotation-api:3.0.0` (resolved from parent)
- All dependencies properly scoped and versioned

This should fix the missing arc → core/runtime dependencies by providing complete resolved dependency information.

## ✅ FINAL RESULTS: Complete Success

### Plugin Performance
- **Project Discovery**: 949 actual Maven projects (vs 1326 globbed files) 
- **Dependency Resolution**: 3860 dependencies created from 945 projects in 751ms
- **Effective POM Usage**: 853 core projects using effective resolution, 92 using raw POMs
- **Arc Dependencies**: Now properly finding `arc-processor -> arc`, `arc-tests -> arc` relationships
- **Core Dependencies**: Resolving `bootstrap-core`, `core-deployment` relationships properly

### Technical Implementation
- ✅ **Process Bombing Fixed**: Limited to 6 concurrent processes max
- ✅ **Performance Optimized**: Maven Daemon support, optimized flags
- ✅ **Plugin Discovery Enhanced**: Better parsing, proper timeouts
- ✅ **Module Discovery**: Recursive from root POM instead of globbing
- ✅ **Effective POM Resolution**: Pure TypeScript implementation for dependency resolution
- ✅ **Dependency Accuracy**: Resolved versions from parent POMs and dependency management

The Maven plugin is now working correctly with accurate project discovery and dependency resolution!

## 🔍 DEPENDENCY ISSUE DIAGNOSED

### Problem Found
The plugin creates and returns 3860 dependencies correctly, but they don't appear in the NX project graph.

### Root Cause Analysis
1. ✅ **Dependencies Created**: Plugin successfully creates 3860 dependencies
2. ✅ **Project Names Match**: All source/target project names exist in context 
3. ✅ **Function Called**: `createDependencies` returns dependencies to NX
4. ❌ **Dependencies Missing**: They don't appear in the final project graph

### Issue Location
The problem is between `createDependencies` returning dependencies and NX processing them into the graph. This suggests:

- **Format Issue**: Dependencies might not be in the correct format NX expects
- **Validation Failure**: NX might be rejecting the dependencies due to validation errors
- **Processing Error**: There could be an error in NX's dependency processing pipeline

### Next Steps
Need to investigate the exact dependency format NX expects and ensure our dependencies match that structure exactly.