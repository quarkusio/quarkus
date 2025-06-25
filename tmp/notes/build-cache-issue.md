# Build Cache Issue - Root Cause Found

## The Real Problem

The cache property isn't making it to the graph.json file because **our source code changes aren't being compiled**. Maven is using a persistent build cache that prevents recompilation of the Kotlin files.

## Evidence

1. **Source Code**: TargetConfiguration.kt contains both `cache` and `debugField` properties
2. **Compiled Bytecode**: Only shows `cache` property, missing `debugField` entirely
3. **Maven Logs**: Show "Loaded from the build cache, saving X.Xs" for every compilation attempt

## Build Cache Behavior

Every Maven compilation shows:
```
[INFO] --- kotlin:1.9.20:compile (compile) @ maven-plugin ---
[INFO] Loaded from the build cache, saving 4.819s
```

This means Maven is using cached bytecode instead of recompiling our source changes.

## Why Cache Property is Null

1. The `cache` property was added to the source code at some point
2. But the build cache contains old bytecode from before the property existed  
3. So the running code has an old version of TargetConfiguration without proper cache support
4. Even forcing `cache = true` doesn't work because the old bytecode doesn't have the logic

## Next Steps

Need to disable the persistent build cache to get a fresh compilation that includes all our source code changes.