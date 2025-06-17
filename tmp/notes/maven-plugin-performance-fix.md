# Maven Plugin Performance Fix

## Issue
The Maven plugin was taking too long to generate the graph because it was running the same Maven analysis twice - once for nodes and once for dependencies.

## Root Cause
Cache keys were different between `createNodesV2` and `createDependencies`:
- `createNodesV2` used: `nodes-${projectHash}-${optionsHash}`
- `createDependencies` used: `deps-${projectHash}-${optionsHash}`

Both functions call the same `runMavenAnalysis()` but couldn't share cached results due to different keys.

## Solution
1. **Unified cache key**: Both functions now use the same key: `projectHash`
2. **Removed redundant hashing**: Since `calculateHashForCreateNodes` already includes options in its calculation, we don't need to hash options separately
3. **Cleaned up imports**: Removed unused `hashObject` import

## Files Changed
- `/maven-plugin.ts` - Fixed cache key inconsistency

## Expected Impact
Graph generation should be ~50% faster since Maven analysis runs once instead of twice.