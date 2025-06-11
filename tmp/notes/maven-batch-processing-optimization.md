# Maven Batch Processing Optimization

## Problem
Processing 1667 Maven projects simultaneously was overwhelming the system, potentially causing performance issues and memory consumption.

## Solution
Implemented configurable batch processing in the Java Maven analyzer with the following optimizations:

### 1. Batch Processing
- **Default batch size**: 50 projects per batch
- **Configurable**: Set via `MAVEN_BATCH_SIZE` environment variable or `-Dmaven.batch.size` system property
- **Progress logging**: Shows batch completion status and timing

### 2. Memory Management
- **Garbage collection**: Runs every 200 processed files (configurable via `MAVEN_GC_INTERVAL`)
- **Brief pauses**: 100ms sleep after GC to reduce system load
- **Final GC**: Runs before JSON output generation

### 3. Monitoring & Logging
- **Batch-level progress**: Shows completion time and success rate per batch
- **Overall progress**: Tracks total processed/successful/failed counts
- **Real-time updates**: Progress logged every batch and at major milestones

## Configuration Options

### Environment Variables (TypeScript)
```bash
export MAVEN_BATCH_SIZE=25        # Smaller batches for slower systems
export MAVEN_GC_INTERVAL=100      # More frequent GC for memory-constrained systems
```

### System Properties (Java)
```bash
-Dmaven.batch.size=75             # Larger batches for powerful systems
-Dmaven.gc.interval=300           # Less frequent GC for systems with more memory
```

## Example Output
```
INFO: Starting batch processing of 1667 Maven projects
INFO: Using batch size: 50, GC interval: 200
INFO: Processing batch 1/34 (files 1-50)
INFO: Batch 1 completed in 2543ms - Success: 48/50 (Failed: 2)
DEBUG: Running garbage collection after 200 files
INFO: Overall progress: 250/1667 (14%) - Total Success: 241, Total Failed: 9
```

## Benefits
1. **System stability**: Prevents memory exhaustion
2. **Monitoring**: Clear progress tracking
3. **Configurability**: Adaptable to different system capabilities
4. **Reliability**: Continues processing even if individual files fail
5. **Performance**: Optimized memory usage with strategic GC

## Usage
The batch processing is automatically enabled when using the TypeScript plugin. No changes needed to existing code - the optimizations are transparent to the user.