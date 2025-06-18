# Maven Execution Plan Analysis Performance Optimization

## Problem
The target dependency analysis was taking a long time due to repeated Maven analysis during the dependency calculation phase. The `ExecutionPlanAnalysisService` was being called multiple times with the same lifecycle and project data.

## Solution - Pre-computed Analysis Architecture

### 1. Enhanced ExecutionPlanAnalysisService
**File**: `ExecutionPlanAnalysisService.java`

**Key Changes**:
- Added pre-computed lifecycle information (cached at service initialization)
- Added `preAnalyzeAllProjects()` method to analyze all reactor projects upfront
- Optimized methods to use pre-computed data instead of recalculating

**Pre-computed Data**:
```java
// Pre-computed lifecycle information (shared across all projects)
private final Set<String> allLifecyclePhases;
private final List<String> defaultLifecyclePhases;
private final List<String> cleanLifecyclePhases;
private final List<String> siteLifecyclePhases;
private final Map<String, org.apache.maven.lifecycle.Lifecycle> phaseToLifecycleMap;
```

**Performance Benefits**:
- Lifecycle phases computed once instead of repeatedly
- Phase-to-lifecycle mapping cached for O(1) lookups
- All project execution plans analyzed upfront instead of on-demand

### 2. Optimized TargetDependencyService
**File**: `TargetDependencyService.java`

**Key Changes**:
- Removed redundant `getCommonGoalsForPhase()` method
- Updated `getPrecedingGoalsInLifecycle()` to use pre-computed execution plan data
- All calls now leverage cached analysis results

### 3. Integration Point
**File**: `NxAnalyzerMojo.java`

**Integration**:
```java
private void initializeServices() {
    this.executionPlanAnalysisService = new ExecutionPlanAnalysisService(
        getLog(), isVerbose(), lifecycleExecutor, session, defaultLifecycles);
    
    // Pre-analyze all projects upfront to avoid performance bottlenecks
    executionPlanAnalysisService.preAnalyzeAllProjects(reactorProjects);
    
    // ... other services
}
```

## Performance Impact

### Before Optimization
- Lifecycle phases calculated repeatedly for each dependency analysis
- Maven execution plans computed on-demand during dependency calculation
- Multiple calls to Maven APIs for the same project/phase combinations

### After Optimization
- All lifecycle information computed once at service initialization
- All project execution plans analyzed upfront in batch
- Dependency analysis uses cached data with O(1) lookups

## Technical Details

### Memory vs Performance Trade-off
- **Memory**: Slightly increased memory usage to store pre-computed analysis
- **Performance**: Significant performance improvement by eliminating redundant Maven API calls

### Caching Strategy
- **Lifecycle data**: Computed once and shared across all projects
- **Project analysis**: Computed once per project and cached for reuse
- **Thread-safe**: Uses `ConcurrentHashMap` for thread-safe caching

### Maven API Usage
- Leverages official Maven APIs: `LifecycleExecutor.calculateExecutionPlan()`
- Uses `DefaultLifecycles.getPhaseToLifecycleMap()` for efficient phase lookups
- Maintains full Maven compatibility while optimizing performance

## Result
The optimization successfully eliminates the performance bottleneck in target dependency analysis by front-loading all Maven analysis work and using cached results throughout the dependency calculation process.