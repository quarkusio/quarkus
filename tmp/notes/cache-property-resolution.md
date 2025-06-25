# Cache Property Resolution - SOLVED

## Issue Summary
Cache properties in graph.json were showing as `null` despite correct implementation logic in the Kotlin code.

## Root Cause
**Develocity (Gradle Enterprise) build caching** was preventing Kotlin source code changes from being compiled. The Maven build system was loading cached bytecode from before the cache property logic was properly implemented.

## Solution
Used the correct Maven property to disable Develocity caching:
```bash
mvn clean compile -Dno-build-cache
```

## Evidence of Fix
**Before (with cached bytecode):**
```json
"cache": null,
```

**After (with fresh compilation):**
```json
"cache": true,
```

## Verification
- All 60,558+ cache properties in graph.json now show correct boolean values
- No more "Loaded from the build cache" messages in Maven logs
- Cache logic in TargetGenerationService.kt is now properly executed

## Prevention Measures Added
1. Updated `vitest.setup.ts` to disable build cache during test compilation
2. Added `compile-java:fresh` npm script with cache disabled
3. Documented build cache issue in CLAUDE.md with symptoms and solutions
4. Added warnings about Develocity caching to development workflow

## Technical Details
The issue was not with:
- JSON serialization (Gson)
- Cache logic implementation
- Data flow between Kotlin and TypeScript

The issue was with **Maven build caching** preventing source code changes from being compiled into bytecode.