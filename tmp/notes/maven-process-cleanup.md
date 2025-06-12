# Maven Process Cleanup Implementation

## Problem
The TypeScript Maven plugin (`maven-plugin2.ts`) was spawning Java Maven reader processes without proper cleanup handlers, potentially leaving orphaned processes when the plugin exits.

## Solution
Added comprehensive process cleanup to `maven-plugin2.ts` similar to the existing `maven-plugin.ts`:

### Changes Made

1. **Process Tracking**: Added `activeMavenProcesses` Set to track all spawned Maven processes
2. **Cleanup Function**: Created `cleanupMavenProcesses()` that properly terminates all tracked processes
3. **Signal Handlers**: Added handlers for:
   - `exit` - Normal process termination
   - `SIGINT` - Ctrl+C interrupt 
   - `SIGTERM` - Termination signal
   - `uncaughtException` - Unhandled errors
   - `unhandledRejection` - Unhandled promise rejections

4. **Process Lifecycle Management**:
   - Track processes when spawned (`activeMavenProcesses.add(child)`)
   - Remove when complete (`activeMavenProcesses.delete(child)`)
   - Enhanced timeout handling with proper process termination

### Files Modified
- `/home/jason/projects/triage/java/quarkus/maven-plugin2.ts` (lines 199-250, 294-353)

### Key Features
- Graceful SIGTERM followed by SIGKILL if needed
- Automatic cleanup on all exit scenarios
- Proper process removal from tracking set
- Enhanced logging for debugging

This ensures Maven reader Java processes are properly shut down when the TypeScript Maven plugin exits, preventing orphaned processes.