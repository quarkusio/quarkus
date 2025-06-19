# Maven Clean Install Implementation

## Solution: Use Separate Targets

Instead of creating a combination `clean-install` target, we use separate `clean` and `install` targets that can be composed.

## Usage Options

### Option 1: Sequential Commands
```bash
nx run project:clean && nx run project:install
```

### Option 2: Package.json Script
```json
{
  "scripts": {
    "clean-install": "nx run project:clean && nx run project:install"
  }
}
```

### Option 3: Maven Batch (when session preservation needed)
```bash
nx run project:maven-batch --goals="clean,install"
```

## Tradeoffs Accepted

- **Separate Maven sessions**: Each target runs in its own Maven session
- **Slight performance cost**: Two JVM startups instead of one
- **Nx benefits gained**: Better composability, caching, and parallelization

## Why This Works Well

For `clean + install` specifically:
- Clean wipes all state anyway, so session preservation provides minimal benefit
- The flexibility of separate targets outweighs the small performance cost
- Maintains Nx's design principles of composable, cacheable targets

## When to Use Maven Batch

Use the maven-batch executor for goal combinations where session preservation matters:
- `compile test package` - Reuses compiled classes
- `test integration-test` - Preserves test setup
- Complex plugin workflows that depend on session state