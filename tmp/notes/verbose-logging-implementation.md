# Verbose Logging Implementation for Maven Plugin v2

## Overview

Added comprehensive progress logging to the Maven plugin v2 that integrates with Nx's `--verbose` flag to provide detailed visibility into Maven project analysis for large workspaces.

## Implementation Details

### Phase 1: TypeScript Plugin Integration (✅ Completed)

**File**: `maven-plugin2.ts`

- **Nx Verbose Detection**: Added detection of Nx verbose mode via `process.env.NX_VERBOSE === 'true'` or `--verbose` in process arguments
- **Maven Arguments**: Dynamically builds Maven command arguments to pass verbose flag: `-Dnx.verbose=${isVerbose}`
- **Quiet Mode Control**: Only adds `-q` (quiet) flag when NOT in verbose mode
- **Enhanced Output Filtering**: In verbose mode, shows all Maven output except debug spam and download messages

```typescript
// Check if Nx is running in verbose mode
const isVerbose = process.env.NX_VERBOSE === 'true' || process.argv.includes('--verbose');

// Build Maven command arguments
const mavenArgs = [
  'io.quarkus:maven-plugin-v2:analyze',
  `-Dnx.outputFile=${outputFile}`,
  `-Dnx.verbose=${isVerbose}`
];

// Only add quiet flag if not in verbose mode
if (!isVerbose) {
  mavenArgs.push('-q');
}
```

### Phase 2: Maven Plugin Verbose Parameter (✅ Completed)

**File**: `NxAnalyzerMojo.java`

- **Parameter Declaration**: Added `@Parameter(property = "nx.verbose", defaultValue = "false")` for Maven integration
- **String-to-Boolean Conversion**: Implemented `isVerbose()` method to handle string parameter conversion and system property fallback
- **Performance Timing**: Added comprehensive timing for all major phases with millisecond precision
- **Project-by-Project Tracking**: Added progress indicators for large workspaces (>50 projects)

```java
@Parameter(property = "nx.verbose", defaultValue = "false")
private String verboseStr;

private boolean isVerbose() {
    // Check both the parameter and system property
    String systemProp = System.getProperty("nx.verbose");
    boolean fromParam = "true".equalsIgnoreCase(verboseStr);
    boolean fromSystem = "true".equalsIgnoreCase(systemProp);
    return fromParam || fromSystem;
}
```

### Phase 3: Generator Classes Logging (✅ Completed)

**File**: `CreateDependenciesGenerator.java`

- **Overloaded Methods**: Added logging-enabled versions of generator methods that accept `Log` and `boolean verbose` parameters
- **Progress Tracking**: Added dependency counting (static vs implicit) with detailed breakdown
- **Batch Progress**: Shows progress every 100 projects for large workspaces
- **Per-Project Details**: Shows individual project dependency counts for smaller workspaces (≤20 projects)

```java
public static List<RawProjectGraphDependency> generateCreateDependencies(
    List<MavenProject> projects, File workspaceRoot, Log log, boolean verbose) {
    
    if (verbose && log != null && projects.size() > 100 && i % 100 == 0) {
        log.info("Dependency analysis progress: " + (i + 1) + "/" + projects.size() + " projects");
    }
}
```

### Phase 4: Performance Metrics (✅ Completed)

**Enhanced Timing and Statistics**:

- **Total Execution Time**: End-to-end processing time with projects/second calculation
- **Phase Breakdown**: Separate timing for target generation, CreateNodes generation, and dependency analysis
- **Progress Indicators**: ETA calculation and throughput metrics for large workspaces
- **Memory Awareness**: Foundation for memory usage reporting (not yet implemented)

**Example Verbose Output**:
```
[INFO] Starting Nx analysis using MavenSession with dependency resolution...
[INFO] Found 1362 projects in reactor
[INFO] Verbose mode enabled - detailed progress will be shown
[INFO] Target generation phase: Processing 1362 projects...
[INFO] Target generation progress: 250/1362 (18.4%) - 32.1 projects/sec - ETA: 34s
[INFO] Target generation complete: 12,847 targets created in 42.3s
[INFO] Generating CreateNodes results for 1362 projects...
[INFO] Generating dependency graph for 1362 projects...
[INFO] Built artifact mapping for 1362 workspace projects
[INFO] Dependency analysis progress: 500/1362 projects
[INFO] Dependency analysis complete: 247 static, 45 implicit dependencies
[INFO] CreateNodes generation: 1.2s
[INFO] Dependencies generation: 2.8s
[INFO] Found 292 workspace dependencies
[INFO] Performance: Processed 1362 projects in 46.3s (29.4 projects/sec)
[INFO] SUCCESS: Maven analysis completed successfully
```

## Usage

### Command Line
```bash
# Normal mode (minimal output)
mvn io.quarkus:maven-plugin-v2:analyze

# Verbose mode (detailed progress)
mvn io.quarkus:maven-plugin-v2:analyze -Dnx.verbose=true
```

### With Nx Integration
```bash
# Normal mode
nx graph

# Verbose mode (shows detailed Maven analysis progress)
NX_DAEMON=false nx graph --verbose
```

## Benefits

### For Large Workspaces (1000+ projects)
- **Progress Visibility**: Clear indication of processing progress with ETA
- **Performance Metrics**: Understanding of bottlenecks and processing rates
- **Error Context**: Better debugging information when issues occur
- **User Confidence**: No more wondering if the process is hung or making progress

### For Development and Debugging
- **Phase Timing**: Identify which phases are slow (target generation vs dependency analysis)
- **Detailed Counts**: Understanding of target and dependency generation statistics
- **Progress Tracking**: Fine-grained visibility into processing for troubleshooting

### Integration Benefits
- **Nx Consistency**: Follows Nx's existing `--verbose` flag pattern
- **Minimal Impact**: No performance overhead in normal mode
- **Configurable**: Can be enabled via Maven parameter or environment variable

## Technical Notes

### Parameter Handling
- Maven parameters are received as strings, requiring explicit boolean conversion
- Fallback to system properties ensures compatibility across different Maven invocation methods
- Default to `false` to maintain existing behavior when not specified

### Performance Considerations
- Logging checks are wrapped in `isVerbose()` conditions to avoid string concatenation overhead
- Progress indicators use modulo operations to avoid excessive logging in large workspaces
- Timing uses `System.currentTimeMillis()` for minimal overhead

### Future Enhancements
- Memory usage reporting during processing
- Configurable progress interval (currently every 50 projects for large workspaces)
- JSON-structured progress output for machine consumption
- Integration with Maven's own progress reporting APIs

## Testing

Successfully tested on:
- ✅ Small projects (1 project): Individual project progress
- ✅ Large workspaces: Batch progress indicators and performance metrics
- ✅ Nx integration: Respects `--verbose` flag
- ✅ Parameter parsing: Handles string-to-boolean conversion correctly

The implementation provides comprehensive progress visibility while maintaining clean output for normal usage.