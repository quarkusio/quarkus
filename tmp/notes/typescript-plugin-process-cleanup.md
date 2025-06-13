# TypeScript Plugin Process Cleanup Implementation

## Problem Fixed
The `maven-plugin2.ts` was spawning Maven analyzer processes without proper cleanup, potentially leaving orphaned processes.

## Solution Implemented
Added timeout and process cleanup to `runMavenAnalysis()` function:

### Changes Made
- **Added 30-second timeout** - Prevents hung processes
- **Added SIGTERM cleanup** - Properly terminates processes on timeout
- **Added timeout clearing** - Prevents unnecessary kills on success/error

### Code Location
File: `maven-plugin2.ts:107-124`

### Implementation Details
```typescript
const timeout = setTimeout(() => {
  child.kill('SIGTERM');
  reject(new Error('Process timeout after 30 seconds'));
}, 30000);

child.on('close', (code) => {
  clearTimeout(timeout);
  // ... rest of handler
});

child.on('error', (error) => {
  clearTimeout(timeout);
  // ... rest of handler
});
```

## Architecture
- **Single process** - Plugin spawns only 1 Maven analyzer process
- **Shared analysis** - Both `createNodesV2` and `createDependencies` use same process
- **Java does everything** - Single analyzer call processes all pom.xml files

## Benefits
- Prevents orphaned Maven processes
- Handles hung processes gracefully
- Matches cleanup pattern from other plugins
- No functional changes to analysis logic