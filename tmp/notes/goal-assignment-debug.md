# Goal Assignment Debug Analysis

## Issue Found
The `maven-compiler:testCompile` goal is being assigned to the `clean` phase instead of `test-compile` because:

1. Java goal assignment logic correctly identifies `testCompile` should go to `test-compile` phase
2. However, the `quarkus-parent` project's `relevantPhases` doesn't include `test-compile`
3. So it falls back to the first available phase (`clean`)

## Root Cause
The logic for detecting relevant phases in parent POM projects is too conservative. Parent POMs with `<packaging>pom</packaging>` are only getting basic phases like `[clean, validate, compile, install, deploy]` and missing intermediate phases like `test-compile`.

## Current Fallback Logic (Problematic)
```java
// Line 1624 in organizeGoalsByPhase()
} else if (!relevantPhases.isEmpty()) {
    // Fallback to first available phase - BAD!
    goalsByPhase.get(relevantPhases.get(0)).add(targetName);
}
```

## Better Solution
Instead of falling back to first phase, we should:
1. Add missing phases to `relevantPhases` when goals need them
2. Or skip goals that don't have appropriate phases
3. Or use better fallback logic (compile instead of clean for build goals)

## Fix Strategy
Update the Java goal assignment logic to handle missing phases more intelligently.