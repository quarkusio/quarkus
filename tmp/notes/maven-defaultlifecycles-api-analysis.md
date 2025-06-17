# Maven DefaultLifecycles API Analysis

## Current Usage in Codebase

The current implementation in `ExecutionPlanAnalysisService.java` uses:
```java
for (org.apache.maven.lifecycle.Lifecycle lifecycle : defaultLifecycles.getLifeCycles()) {
    if (lifecycleId.equals(lifecycle.getId()) && lifecycle.getPhases() != null) {
        return new ArrayList<>(lifecycle.getPhases());
    }
}
```

This approach iterates through all lifecycles and filters by ID ("default", "clean", "site").

## Available DefaultLifecycles API Methods

Based on Maven source code analysis, the `DefaultLifecycles` class provides these public methods:

### 1. `get(String phase)` - Direct Phase Lookup
```java
public Lifecycle get(String phase)
```
- **Purpose**: Returns a Lifecycle object for a given phase
- **Returns**: `Lifecycle` object or `null` if phase not found
- **Usage**: Direct lookup of lifecycle by phase name

### 2. `getPhaseToLifecycleMap()` - Phase-to-Lifecycle Mapping
```java
public Map<String, Lifecycle> getPhaseToLifecycleMap()
```
- **Purpose**: Returns a map of phases to their corresponding Lifecycles
- **Returns**: `Map<String, Lifecycle>` mapping phase names to lifecycle objects
- **Usage**: Efficient lookup when working with multiple phases

### 3. `getLifeCycles()` - All Lifecycles (Current Method)
```java
public List<Lifecycle> getLifeCycles()
```
- **Purpose**: Returns an ordered list of Lifecycle objects
- **Returns**: `List<Lifecycle>` containing all available lifecycles
- **Usage**: Current implementation uses this method

### 4. `getLifecyclePhaseList()` - Phase List String
```java
public String getLifecyclePhaseList()
```
- **Purpose**: Returns a comma-separated string of all lifecycle phases
- **Returns**: `String` with all phases (e.g., "validate,compile,test,package...")
- **Usage**: Debugging, error messages, or documentation

## Maven Core Usage Example

From `DefaultLifecycleExecutionPlanCalculator` in Maven core:

```java
Lifecycle lifecycle = defaultLifeCycles.get(lifecyclePhase);

if (lifecycle == null) {
    throw new LifecyclePhaseNotFoundException(
        "Unknown lifecycle phase \"" + lifecyclePhase + 
        "\". Available lifecycle phases are: " + 
        defaultLifeCycles.getLifecyclePhaseList() + ".", 
        lifecyclePhase);
}
```

## Alternative Implementation Options

### Option 1: Use `getPhaseToLifecycleMap()` for Direct Access
```java
private List<String> getLifecyclePhases(String lifecycleId) {
    Map<String, Lifecycle> phaseToLifecycleMap = defaultLifecycles.getPhaseToLifecycleMap();
    
    // Find phases belonging to specific lifecycle
    return phaseToLifecycleMap.entrySet().stream()
        .filter(entry -> lifecycleId.equals(entry.getValue().getId()))
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
}
```

### Option 2: Cache Phase-to-Lifecycle Map
```java
private final Map<String, Lifecycle> phaseToLifecycleMap = defaultLifecycles.getPhaseToLifecycleMap();

private List<String> getLifecyclePhases(String lifecycleId) {
    return phaseToLifecycleMap.entrySet().stream()
        .filter(entry -> lifecycleId.equals(entry.getValue().getId()))
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
}
```

### Option 3: Direct Phase Lookup
```java
public boolean isValidPhase(String phase) {
    return defaultLifecycles.get(phase) != null;
}

public Lifecycle getLifecycleForPhase(String phase) {
    return defaultLifecycles.get(phase);
}
```

## Standard Maven Lifecycles

Maven defines 3 standard lifecycles:
- **default**: Main build lifecycle (validate, compile, test, package, verify, install, deploy)
- **clean**: Cleaning lifecycle (pre-clean, clean, post-clean)  
- **site**: Site documentation lifecycle (pre-site, site, post-site, site-deploy)

## Recommendations

1. **Current Implementation is Fine**: The existing `getLifeCycles()` approach works correctly and is readable
2. **Consider Caching**: For performance, cache the phase-to-lifecycle map if called frequently
3. **Use `get(phase)` for Validation**: When validating individual phases, use direct lookup
4. **Use `getLifecyclePhaseList()` for Debugging**: Helpful for error messages showing available phases

## Performance Considerations

- `getLifeCycles()`: Returns all lifecycles, requires iteration and filtering
- `getPhaseToLifecycleMap()`: More efficient for multiple phase lookups
- `get(phase)`: Most efficient for single phase validation
- All methods are relatively lightweight since Maven defines only 3 lifecycles

The current implementation is appropriate for the use case where we need all phases from specific lifecycles.