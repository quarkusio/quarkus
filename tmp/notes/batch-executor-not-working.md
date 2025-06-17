# Batch Executor Not Working - Root Cause Analysis

## Problem

The Maven batch executor is not maintaining Maven session context because **Nx is not using the batch implementation**. Instead, it's executing each goal individually in separate Maven sessions.

## Evidence

From the execution log, each goal runs separately:

```
✅ Goal 1: org.apache.maven.plugins:maven-resources-plugin:resources (across 1 projects) (6625ms)
✅ Goal 1: org.apache.maven.plugins:maven-compiler-plugin:compile (across 1 projects) (9282ms)
✅ Goal 1: org.apache.maven.plugins:maven-plugin-plugin:descriptor (across 1 projects) (10322ms)
...
```

Each shows "Goal 1" and "across 1 projects", indicating individual execution rather than batch.

## Expected Behavior

If the batch executor were working, we should see:
```
✅ Goal 1: org.apache.maven.plugins:maven-resources-plugin:resources
✅ Goal 2: org.apache.maven.plugins:maven-compiler-plugin:compile  
✅ Goal 3: org.apache.maven.plugins:maven-plugin-plugin:descriptor
...
(across X projects) - all goals in a single Maven session
```

## Root Cause

Nx is using the regular executor implementation for each task instead of the batch implementation. This happens even though we have:

```json
{
  "batchImplementation": "./src/executors/maven-batch/impl.ts#batchMavenExecutor"
}
```

## Possible Solutions

1. **Check Nx version compatibility** - The batch implementation feature might not be enabled
2. **Add executor configuration** - May need additional configuration to trigger batching
3. **Check executor registration** - The batch function export might not be correct
4. **Force batch mode** - May need to explicitly enable batch mode in Nx configuration

## Next Steps

Need to investigate:
1. How Nx determines when to use batch vs individual execution
2. If there are any missing configuration requirements
3. Whether the current Nx version supports this feature properly