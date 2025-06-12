# Maven Process Cleanup Fixes

## Problems Fixed

### 1. Java Subprocess Not Being Killed
- **Problem**: Maven `exec:java` spawns a Java subprocess, but we were only killing the Maven process
- **Solution**: Added process group killing using `detached: true` and `process.kill(-pid, signal)` to kill entire process groups

### 2. Duplicate Event Handlers
- **Problem**: Two `child.on('close')` handlers caused timeout not to be cleared properly
- **Solution**: Consolidated into single close handler that properly clears timeout

### 3. Ineffective Process Cleanup
- **Problem**: Cleanup function used `!process.killed` which doesn't work reliably for child processes
- **Solution**: Simplified to just check `process.pid` and use process group killing

### 4. Unnecessary File Deletion
- **Problem**: Deleting the output file each time, but now using consistent path
- **Solution**: Keep the file at `maven-script/maven-results.json` for consistency

## Key Changes Made

### Process Group Killing
```typescript
// Create detached process group
const child = spawn(command, args, {
  detached: true,  // Creates new process group
  // ...
});

// Kill entire process group
process.kill(-child.pid, 'SIGTERM');  // Negative PID kills process group
```

### Consolidated Event Handlers
```typescript
child.on('close', (code) => {
  activeMavenProcesses.delete(child);
  clearTimeout(timeoutId);  // Always clear timeout
  // handle result...
});
```

### Improved Cleanup Function
- Removes reliance on `process.killed` flag
- Uses process group killing for both SIGTERM and SIGKILL
- Better error handling for already-dead processes

## Result
- Java subprocesses should now be properly terminated
- No more hanging Maven/Java processes
- Cleaner timeout handling
- Consistent output file management