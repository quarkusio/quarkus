# Maven Enforcer Execution ID Fix

## Problem
Our Maven plugin generates goals like:
```
"org.apache.maven.plugins:maven-enforcer-plugin:enforce"
```

But this fails because it doesn't include the execution ID. Maven needs:
```
"org.apache.maven.plugins:maven-enforcer-plugin:enforce@enforce"
```

## Root Cause
In `TargetGenerationService.kt` line 230, we create goals as:
```kotlin
"goals" to listOf("$pluginKey:$goal"),
```

But we should include the execution ID when available:
```kotlin
"goals" to listOf("$pluginKey:$goal@${execution.id}"),
```

## Solution
Modify the goal string construction in both:
1. `createGoalTarget()` method (line 230)
2. `createSimpleGoalTarget()` method (line 312)

The format should be:
- If execution.id exists and is not null/empty: `"$pluginKey:$goal@${execution.id}"`
- Otherwise: `"$pluginKey:$goal"`

## Test Commands
- `mvn org.apache.maven.plugins:maven-enforcer-plugin:enforce` ❌ (fails)
- `mvn org.apache.maven.plugins:maven-enforcer-plugin:enforce@enforce` ✅ (works)
- `mvn validate` ✅ (works, uses build cache)

## Files to Modify
- `TargetGenerationService.kt` lines 230 and 312