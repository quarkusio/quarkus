# Goal Dependencies Implementation

## What Was Done

### Fixed Task Graph Architecture
- Updated `TargetDependencyService.java` to implement goal-to-goal dependencies instead of goal-to-phase dependencies
- Goals now depend on other goals within the same project based on Maven lifecycle ordering
- Cross-module dependencies still use phase names (e.g., `^install`)

### Key Changes Made
1. **Removed phase dependencies**: Goals no longer depend on phases like `^install`
2. **Added goal-to-goal dependencies**: Goals now depend on preceding goals from earlier lifecycle phases
3. **Implemented `getPrecedingGoalsInLifecycle()`**: Method finds all goals from preceding phases in Maven lifecycle
4. **Maintained cross-module dependencies**: Goals still depend on same phase across other projects (e.g., `^install`)

### Test Results
- All unit tests now pass
- Goal dependency logic correctly identifies preceding goals
- Example: `maven-install:install` now depends on goals from earlier phases like `maven-jar:jar`

## Current Issue

The install goal still fails with: "The packaging plugin did not assign a file to the build artifact"

### Root Cause
Even though dependency ordering is correct, each goal runs in a separate Maven session:
- `maven-jar:jar` creates JAR file in its session ✅
- `maven-install:install` runs in separate session and can't find the JAR ❌

### What Happens
1. `maven-jar:jar` runs and completes successfully
2. `maven-install:install` runs but in isolated session
3. Install goal can't access artifact created by jar goal
4. Build fails

## Solution Needed

Goals that share artifacts need to run in the same Maven session. This requires:

1. **Identify artifact-sharing goal groups**: Groups like `[compile, jar, install]` that need shared session
2. **Batch related goals**: Execute artifact-sharing goals together in single Maven session  
3. **Keep independent goals separate**: Goals that don't share artifacts can run independently for better caching

### Example Batching Strategy
- **Session 1**: `maven-compiler:compile` + `maven-jar:jar` + `maven-install:install`
- **Session 2**: `maven-surefire:test` (independent)
- **Session 3**: `maven-site:site` (independent)

## Progress Summary
✅ Goal dependency ordering fixed
✅ Task graph architecture corrected
✅ Unit tests passing
❌ Maven session context sharing still needed for artifact dependencies