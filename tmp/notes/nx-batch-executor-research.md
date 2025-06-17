# Nx Batch Executor Research Findings

## Overview

Research conducted on how Nx implements batch executors by examining the official Nx source code (nrwl/nx repository). Batch executors allow running multiple tasks in a single process for better performance.

## Key Findings

### 1. Executor Configuration (executors.json)

Batch executors are configured using `batchImplementation` alongside regular `implementation`:

```json
{
  "executors": {
    "counter": {
      "implementation": "./src/executors/counter/counter.impl",
      "batchImplementation": "./src/executors/counter/counter.impl#batchCounter",
      "schema": "./src/executors/counter/schema.json",
      "description": "A dummy executor useful for E2E tests.",
      "hidden": true
    }
  }
}
```

### 2. Function Signatures

**Regular Executor:**
```typescript
export default async function* counter(opts: CounterOptions) {
  // Generator function that yields results over time
  for (let i = 0; i < opts.to; ++i) {
    console.log(i);
    yield { success: false };
    await wait();
  }
  yield { success: opts.result };
}
```

**Batch Executor:**
```typescript
export async function batchCounter(
  taskGraph: TaskGraph,
  inputs: Record<string, CounterOptions>
): Promise<Record<string, { success: boolean; terminalOutput: string }>> {
  // Process multiple tasks at once
  const result: Record<string, { success: boolean; terminalOutput: string }> = {};
  
  const results = await Promise.all(
    (taskGraph.roots as string[])
      .map((rootTaskId) => [rootTaskId, inputs[rootTaskId]] as const)
      .map(async ([taskId, options]) => {
        let terminalOutput = '';
        // Process task
        return [taskId, options.result, terminalOutput] as const;
      })
  );

  // Map results back to task IDs
  for (const [taskId, taskResult, terminalOutput] of results) {
    result[taskId] = {
      success: taskResult,
      terminalOutput,
    };
  }
  
  return result;
}
```

### 3. Key Differences

**Regular vs Batch Executors:**

| Aspect | Regular Executor | Batch Executor |
|--------|------------------|----------------|
| Input | Single options object | TaskGraph + Record of inputs |
| Output | Generator yielding results | Promise of results map |
| Processing | One task at a time | Multiple tasks in parallel |
| Function Type | Async Generator | Async Function |
| Performance | One process per task | Multiple tasks per process |

### 4. TypeScript Interfaces

**ExecutorContext Interface:**
```typescript
interface ExecutorContext {
  projectName?: string;
  projectsConfigurations: ProjectsConfigurations;
  root: string;
  target?: TargetConfiguration<any>;
  targetName?: string;
  taskGraph?: TaskGraph;
}
```

**Batch Executor Return Type:**
```typescript
Record<string, { 
  success: boolean; 
  terminalOutput: string 
}>
```

### 5. Real-World Example: @nx/js:tsc

The TypeScript compiler executor supports batch execution:

**executors.json:**
```json
{
  "tsc": {
    "implementation": "./src/executors/tsc/tsc.impl",
    "batchImplementation": "./src/executors/tsc/tsc.batch-impl",
    "schema": "./src/executors/tsc/schema.json"
  }
}
```

**Batch Implementation Signature:**
```typescript
export async function* tscBatchExecutor(
  taskGraph: TaskGraph,
  inputs: Record<string, ExecutorOptions>,
  overrides: ExecutorOptions,
  context: ExecutorContext
): AsyncGenerator<BatchExecutorTaskResult>
```

### 6. Performance Benefits

- **1.16x to 7.73x faster** execution times depending on use case
- Reduces process startup overhead
- Enables incremental compilation for TypeScript
- Creates TypeScript project references automatically based on project graph

### 7. Usage Requirements

**For @nx/js:tsc batch mode:**
- All dependent projects must use `@nx/js:tsc` executor
- Batch mode is experimental feature
- Use `--batch` flag to enable: `nx run-many --target=build --batch`

### 8. Batch Executor Limitations

- Currently only implemented for specific executors (@nx/js:tsc, counter example)
- Experimental feature with limited ecosystem support
- Requires all dependencies to use compatible executors
- More complex implementation than regular executors

## Implementation Pattern Summary

1. **Export named function** (not default) from executor file
2. **Use `batchImplementation`** in executors.json pointing to named function
3. **Accept TaskGraph and inputs Record** as parameters
4. **Return Promise or AsyncGenerator** of task results mapped by task ID
5. **Process tasks in parallel** using Promise.all or similar patterns
6. **Handle terminal output** collection for each task
7. **Map results back** to original task IDs

## Best Practices

- Use batch executors for CPU-intensive tasks that benefit from shared process overhead
- Consider memory usage when processing many tasks simultaneously
- Implement proper error handling for individual tasks within the batch
- Provide meaningful terminal output for debugging
- Test both single task and multi-task scenarios