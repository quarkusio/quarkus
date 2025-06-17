# Dynamic Phase Analysis Implementation

## LifecyclePhaseAnalyzer Service

This service replaces hardcoded phase behavior with dynamic Maven API-based analysis.

### Key Features
1. Uses Maven's DefaultLifecycles API for phase metadata
2. Analyzes phase names semantically  
3. Considers phase position within lifecycle
4. Caches results for performance
5. Provides fallback to hardcoded logic

### Implementation Strategy

#### Phase Categorization Logic
1. **Semantic Analysis**: Examine phase name patterns
2. **Position Analysis**: Use phase order in lifecycle
3. **Lifecycle Context**: Consider which lifecycle contains the phase
4. **Caching**: Cache analysis results per phase

#### Phase Categories
- **SOURCE_PROCESSING**: Phases that work with source code
- **TEST_RELATED**: Phases for testing
- **RESOURCE_PROCESSING**: Phases that handle resources
- **GENERATION**: Phases that generate code/resources
- **COMPILATION**: Phases that compile code
- **PACKAGING**: Phases that package artifacts
- **DEPLOYMENT**: Phases that deploy/install artifacts
- **VALIDATION**: Phases that validate project state

### Code Structure
```java
public class LifecyclePhaseAnalyzer {
    private final DefaultLifecycles defaultLifecycles;
    private final Map<String, PhaseAnalysis> analysisCache;
    
    public PhaseAnalysis analyzePhase(String phase) {
        // 1. Check cache
        // 2. Get lifecycle for phase
        // 3. Analyze phase name semantically
        // 4. Consider phase position
        // 5. Return comprehensive analysis
    }
}
```

### Integration Points
- Replace DynamicGoalAnalysisService.analyzeByPhase() hardcoded switch
- Use existing ExecutionPlanAnalysisService for lifecycle access
- Maintain backward compatibility with GoalBehavior