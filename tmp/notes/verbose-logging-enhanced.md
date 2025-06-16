# Enhanced Verbose Logging in Maven Plugin

## Summary
Enhanced the Maven plugin to properly support verbose logging during tests, providing better visibility into what the plugin is doing during execution.

## Changes Made

### 1. Updated Interface
- Added `verbose?: boolean` to `MavenPluginOptions` interface
- This allows tests to explicitly enable verbose logging

### 2. Fixed Verbose Logic
- **Before**: Plugin was checking `(options as any)?.verbose !== false` which didn't work correctly
- **After**: Plugin now properly checks `opts.verbose` using the typed options

### 3. Enhanced Verbose Logging in `runMavenAnalysis`
- Now checks `options.verbose` in addition to environment variables and CLI flags
- Added comprehensive logging messages:
  ```typescript
  if (isVerbose) {
    console.log(`Running Maven analysis with verbose logging enabled...`);
    console.log(`Maven executable: ${options.mavenExecutable}`);
    console.log(`Output file: ${outputFile}`);
    console.log(`Executing Maven command: ${options.mavenExecutable} ${mavenArgs.join(' ')}`);
    console.log(`Working directory: ${workspaceRoot}`);
  }
  ```

### 4. Added Progress Logging
- Logs when Maven process starts
- Logs when Maven process completes with exit code
- Logs success message when analysis completes successfully

### 5. Consistent Verbose Checks
Updated all verbose checks throughout the plugin:
- `createNodesV2`: Now uses `opts.verbose` instead of casting
- `createDependencies`: Now uses `opts.verbose` instead of casting  
- `runMavenAnalysis`: Now includes `options.verbose` in verbose check

## Test Integration
The tests already had `verbose: true` in the options, but now this will actually work because:
1. The interface properly defines the verbose option
2. The plugin respects the verbose option from the test configuration
3. More detailed logging will show progress during test execution

## Expected Output During Tests
When running tests with verbose enabled, you'll see:
```
Maven plugin found 1 pom.xml files
Running Maven analysis with verbose logging enabled...
Maven executable: mvn
Output file: /path/to/maven-analysis.json
Executing Maven command: mvn dependency:tree -DoutputType=json -DoutputFile=/path/to/maven-analysis.json -Dverbose=true -q
Working directory: /path/to/workspace
Maven process completed with exit code: 0
Maven analysis completed successfully
```

## Benefits
1. **Better debugging**: Can see exactly what Maven commands are being executed
2. **Progress visibility**: Shows when analysis starts, runs, and completes
3. **Test transparency**: Tests now provide clear feedback about what's happening
4. **Proper configuration**: Verbose option is now properly typed and respected