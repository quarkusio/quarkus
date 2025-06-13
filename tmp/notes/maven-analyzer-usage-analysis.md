# Maven Analyzer Usage Analysis

## Current Implementation Status

The codebase has transitioned from using the standalone MavenAnalyzer.java to the NxAnalyzerMojo Maven plugin approach.

## Key Findings

### 1. Current Active Implementation
- **Primary**: Uses `io.quarkus:maven-plugin-v2:analyze` (NxAnalyzerMojo)
- **Location**: Line 325 in maven-plugin2.ts
- **Command**: `mvn io.quarkus:maven-plugin-v2:analyze -Dnx.outputFile=${outputFile}`

### 2. Standalone MavenAnalyzer Status
- **File exists**: `/maven-plugin-v2/src/main/java/MavenAnalyzer.java` 
- **Not actively used**: The `generateNxConfigFromMavenAsync()` function exists but is NOT called
- **Legacy code**: Contains old implementation using `exec:java -Dexec.mainClass="MavenAnalyzer"`

### 3. Architecture Transition
- **From**: Standalone Java application with main() method
- **To**: Maven plugin (Mojo) integrated into Maven lifecycle
- **Benefit**: Better access to Maven session, reactor projects, and resolved dependencies

### 4. Code References
- **Active function**: `generateBatchNxConfigFromMavenAsync()` (line 297)
- **Unused function**: `generateNxConfigFromMavenAsync()` (line 178) 
- **Search patterns**: References to "MavenAnalyzer" mostly in comments and unused code

## Conclusion

The standalone MavenAnalyzer.java is **NOT currently being used**. The implementation has migrated to use the NxAnalyzerMojo plugin approach, which provides better Maven integration and dependency resolution capabilities.

The standalone analyzer exists as legacy code that could potentially be removed, though it might be kept for debugging or alternative execution scenarios.