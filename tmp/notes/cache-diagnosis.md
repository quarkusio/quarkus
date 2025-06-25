# Cache Property Diagnosis - Final Analysis

## Issue Summary
The cache property is always `null` in graph.json despite:
1. ✅ Correct logic in `shouldEnableCaching()` functions  
2. ✅ Correct property definition in TargetConfiguration model
3. ✅ Correct copying in CreateNodesResultGenerator 
4. ✅ Even forcing `cache = true` results in `null` in JSON

## Root Cause Discovery

**Critical Finding**: Even when we manually set `target.cache = true` in three different places in the Kotlin code, the final JSON still shows `"cache": null` for all 60,558 targets.

This indicates the issue is NOT in:
- The caching logic (`shouldEnableCaching` functions work correctly)
- The property definition (TargetConfiguration.cache is properly defined)  
- The copying logic (CreateNodesResultGenerator copies cache correctly)

## Real Issue Location

The problem must be in the **object creation or serialization pipeline**. Possible causes:

1. **Object Creation Issue**: The TargetConfiguration objects aren't being created properly
2. **Serialization Issue**: Gson isn't serializing the cache property correctly
3. **Data Pipeline Issue**: The objects are being replaced/overwritten somewhere

## Evidence
- Raw maven-analysis.json also shows `"cache": null` 
- This means the issue is in the Kotlin→JSON conversion, not TypeScript→JSON
- Forced `cache = true` values are not making it through

## Next Steps
Need to investigate the Gson serialization or object creation more deeply to find where the cache property is being lost.