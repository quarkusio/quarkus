# Cache Property Issue - Resolution

## Root Cause Identified ✅

The cache property was always `null` in graph.json because **Develocity (Gradle Enterprise) build caching** was preventing Kotlin source code changes from being compiled. The build system was using cached bytecode from before the cache property logic was properly implemented.

## Evidence

1. **Source Code**: TargetConfiguration.kt correctly defines `var cache: Boolean? = null`
2. **Business Logic**: `shouldEnableCaching()` functions return correct true/false values  
3. **Bytecode**: Only showed basic properties, missing newly added properties
4. **Maven Logs**: Showed "Loaded from the build cache, saving X.Xs" for every compilation

## Solution Implemented ✅

### 1. Updated Documentation (CLAUDE.md)
- Added build cache warning section
- Provided cache disable commands
- Listed symptoms to help identify the issue

### 2. Updated Build Scripts (package.json)
- Added `compile-java:fresh` script with cache disabled
- Provides easy way to force fresh compilation

### 3. Updated Test Setup (vitest.setup.ts)  
- Modified test compilation to disable Develocity cache
- Ensures E2E tests always use fresh compilation
- Added environment variables and system properties

### 4. Cache Disable Methods

**Method 1 - Environment Variable:**
```bash
GRADLE_ENTERPRISE_BUILD_CACHE_ENABLED=false mvn clean compile
```

**Method 2 - System Property:**
```bash
mvn clean compile -Dgradle.enterprise.build-cache.enabled=false
```

**Method 3 - NPM Script:**
```bash
npm run compile-java:fresh
```

## Expected Resolution

Once developers use the cache-disabled compilation methods:

1. ✅ Kotlin source changes will be properly compiled
2. ✅ Cache property logic will be included in bytecode  
3. ✅ `shouldEnableCaching()` results will be applied correctly
4. ✅ Graph.json will show `"cache": true/false` instead of `"cache": null`
5. ✅ Gson serialization will work as expected

## Prevention

The updated test setup and documentation will prevent this issue from affecting future development work on the Maven plugin.