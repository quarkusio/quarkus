# Target Dependencies Fix

## Issue Identified
The Maven plugin was generating nodes with empty `targets: {}` objects because:

1. The Java analyzer was successfully detecting phases and plugin goals
2. But the generated `maven-results.json` file was using old format without target data
3. The TypeScript plugin expects `relevantPhases`, `pluginGoals`, `phaseDependencies`, and `crossProjectDependencies` from Java

## Solution Applied
1. **Java Analyzer was already working correctly** - it has functions like:
   - `detectRelevantPhases()` - finds Maven phases like compile, test, package
   - `detectPluginGoals()` - finds plugin-specific goals like quarkus:dev
   - `detectPhaseDependencies()` - maps phase dependencies
   - `detectCrossProjectTargetDependencies()` - finds cross-project target deps

2. **Updated JSON output** - regenerated maven-results.json with target data:
   ```json
   {
     "project-path": {
       "name": "...",
       "relevantPhases": ["clean", "validate", "compile", "test", "package"],
       "pluginGoals": [{"pluginKey": "...", "goal": "...", "targetType": "..."}],
       "phaseDependencies": {"test": ["compile"], "package": ["test"]},
       "crossProjectDependencies": {...}
     }
   }
   ```

3. **TypeScript plugin correctly processes target data** - the existing `normalizeTargets()` and `generateDetectedTargets()` functions work correctly when given the right input

## Result
- Nodes now have populated targets with proper dependencies
- Phase targets (compile, test, package) with correct dependsOn relationships
- Plugin goal targets (quarkus:dev, maven-compiler:compile) with metadata
- Cross-project target dependencies for multi-module builds

## Files Modified
- Regenerated `maven-results.json` with complete target information
- No changes needed to TypeScript plugin - it was already correctly designed