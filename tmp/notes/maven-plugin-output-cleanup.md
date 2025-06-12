# Maven Plugin Output File Cleanup

## Problem
The Maven plugin was creating multiple temporary JSON files with timestamps like:
- `maven-results-1234567890.json`
- `maven-results-final.json`
- `maven-results-fixed.json`
etc.

This cluttered the workspace root with temporary files.

## Solution
Changed the output file path from:
```typescript
const outputFile = join(workspaceRoot, `maven-results-${Date.now()}.json`);
```

To a consistent path:
```typescript
const outputFile = join(workspaceRoot, 'maven-script/maven-results.json');
```

## Result
- Only one consistent output file: `maven-script/maven-results.json`
- No more timestamp-based temporary files cluttering the workspace
- Cleaner workspace structure
- File location matches the maven-script directory structure

## File Changed
- `maven-plugin2.ts:279` - Updated output file path to use consistent location