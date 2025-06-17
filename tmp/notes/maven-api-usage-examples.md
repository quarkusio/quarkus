# Maven API Usage Examples for Dynamic Lifecycle Analysis

## Overview
These code examples show how to use Maven's DefaultLifecycles and Lifecycle APIs to replace hardcoded phase behavior with dynamic analysis.

## Key Maven APIs Used

### 1. DefaultLifecycles Class
```java
@Component(role = DefaultLifecycles.class)
public class DefaultLifecycles {
    // Key methods:
    public Map<String, Lifecycle> getPhaseToLifecycleMap()
    public List<Lifecycle> getLifeCycles()
}
```

### 2. Lifecycle Class  
```java
public class Lifecycle {
    // Key methods:
    public String getId()           // Returns "default", "clean", "site"
    public List<String> getPhases()  // Returns ordered list of phases
    public Map<String, String> getDefaultPhases()  // @Deprecated but useful
}
```

## Complete Implementation Examples

### Example 1: Basic Phase-to-Lifecycle Mapping
```java
@Inject
private DefaultLifecycles defaultLifecycles;

public String getLifecycleForPhase(String phase) {
    try {
        Map<String, Lifecycle> phaseMap = defaultLifecycles.getPhaseToLifecycleMap();
        Lifecycle lifecycle = phaseMap.get(phase);
        return lifecycle != null ? lifecycle.getId() : null;
    } catch (Exception e) {
        log.warn("Could not get lifecycle for phase: " + e.getMessage());
        return null;
    }
}
```

### Example 2: Getting All Phases from All Lifecycles
```java
public Set<String> getAllLifecyclePhases() {
    Set<String> allPhases = new LinkedHashSet<>();
    
    try {
        for (Lifecycle lifecycle : defaultLifecycles.getLifeCycles()) {
            if (lifecycle.getPhases() != null) {
                allPhases.addAll(lifecycle.getPhases());
            }
        }
    } catch (Exception e) {
        log.warn("Could not retrieve lifecycle phases: " + e.getMessage());
    }
    
    return allPhases;
}
```

### Example 3: Phase Position Analysis
```java
public int getPhasePosition(String phase) {
    try {
        Lifecycle lifecycle = defaultLifecycles.getPhaseToLifecycleMap().get(phase);
        if (lifecycle != null && lifecycle.getPhases() != null) {
            return lifecycle.getPhases().indexOf(phase);
        }
    } catch (Exception e) {
        log.warn("Could not get phase position: " + e.getMessage());
    }
    return -1;
}
```

### Example 4: Dynamic Phase Categorization
```java
public PhaseCategory categorizePhase(String phase) {
    // Get lifecycle context
    Lifecycle lifecycle = defaultLifecycles.getPhaseToLifecycleMap().get(phase);
    
    if (lifecycle == null) {
        return PhaseCategory.UNKNOWN;
    }
    
    String lifecycleId = lifecycle.getId();
    List<String> phases = lifecycle.getPhases();
    int position = phases.indexOf(phase);
    
    // Lifecycle-based categorization
    if ("clean".equals(lifecycleId)) {
        return PhaseCategory.CLEANUP;
    } else if ("site".equals(lifecycleId)) {
        return PhaseCategory.DOCUMENTATION;
    }
    
    // Position-based categorization for default lifecycle
    if ("default".equals(lifecycleId)) {
        int totalPhases = phases.size();
        
        if (position < totalPhases / 3) {
            return PhaseCategory.SETUP;  // Early phases
        } else if (position < (2 * totalPhases) / 3) {
            return PhaseCategory.BUILD;  // Middle phases
        } else {
            return PhaseCategory.DEPLOY; // Late phases
        }
    }
    
    return PhaseCategory.UNKNOWN;
}
```

### Example 5: Complete Phase Analysis Service
```java
public class DynamicPhaseAnalyzer {
    private final DefaultLifecycles defaultLifecycles;
    private final Map<String, PhaseAnalysis> cache = new ConcurrentHashMap<>();
    
    public PhaseAnalysis analyzePhase(String phase) {
        return cache.computeIfAbsent(phase, this::performAnalysis);
    }
    
    private PhaseAnalysis performAnalysis(String phase) {
        PhaseAnalysis analysis = new PhaseAnalysis(phase);
        
        // Get lifecycle using Maven API
        Lifecycle lifecycle = defaultLifecycles.getPhaseToLifecycleMap().get(phase);
        if (lifecycle != null) {
            analysis.setLifecycleId(lifecycle.getId());
            analysis.setPhases(lifecycle.getPhases());
            analysis.setPosition(lifecycle.getPhases().indexOf(phase));
        }
        
        // Semantic analysis
        analyzePhaseSemantics(analysis);
        
        // Position analysis
        analyzePhasePosition(analysis);
        
        return analysis;
    }
    
    private void analyzePhaseSemantics(PhaseAnalysis analysis) {
        String phase = analysis.getPhase().toLowerCase();
        
        if (phase.contains("source") || phase.equals("compile")) {
            analysis.addBehavior(PhaseBehavior.PROCESSES_SOURCES);
        }
        
        if (phase.contains("test")) {
            analysis.addBehavior(PhaseBehavior.TEST_RELATED);
        }
        
        if (phase.contains("resource")) {
            analysis.addBehavior(PhaseBehavior.NEEDS_RESOURCES);
        }
        
        // etc...
    }
}
```

## Integration with Existing Code

### Replacing Hardcoded Switch Statement
```java
// OLD APPROACH (hardcoded):
private GoalBehavior analyzeByPhase(String phase) {
    switch (phase) {
        case "compile":
        case "process-sources":
            behavior.setProcessesSources(true);
            break;
        // ... more hardcoded cases
    }
}

// NEW APPROACH (dynamic Maven APIs):
private GoalBehavior analyzeByPhase(String phase) {
    return phaseAnalyzer.toGoalBehavior(phase);
}
```

### Constructor Injection
```java
public DynamicGoalAnalysisService(
    // ... existing parameters
    DefaultLifecycles defaultLifecycles
) {
    // ... existing initialization
    this.phaseAnalyzer = new LifecyclePhaseAnalyzer(defaultLifecycles, log, verbose);
}
```

## Benefits of Maven API Approach

1. **Automatic adaptation**: Works with custom phases and future Maven changes
2. **Comprehensive coverage**: Discovers all phases dynamically
3. **Reduced maintenance**: No need to update hardcoded phase lists
4. **Better accuracy**: Uses Maven's own understanding of phase relationships
5. **Extensibility**: Can be enhanced with additional analysis patterns

## Testing the Implementation
```java
@Test
public void testDynamicPhaseAnalysis() {
    // Test with known phases
    PhaseAnalysis compileAnalysis = analyzer.analyzePhase("compile");
    assertTrue(compileAnalysis.hasCategory(PhaseCategory.SOURCE_PROCESSING));
    assertEquals("default", compileAnalysis.getLifecycleId());
    
    // Test with custom phases
    PhaseAnalysis customAnalysis = analyzer.analyzePhase("custom-generate-sources");
    assertTrue(customAnalysis.hasCategory(PhaseCategory.GENERATION));
}
```