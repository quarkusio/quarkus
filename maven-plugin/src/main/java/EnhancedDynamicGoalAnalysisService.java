import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced version of DynamicGoalAnalysisService that uses Maven APIs for dynamic
 * lifecycle phase analysis instead of hardcoded phase categorization.
 * 
 * This service demonstrates how to replace hardcoded switch statements with
 * intelligent Maven API-based phase analysis using DefaultLifecycles and Lifecycle APIs.
 */
public class EnhancedDynamicGoalAnalysisService {
    
    private final MavenSession session;
    private final ExecutionPlanAnalysisService executionPlanAnalysis;
    private final MavenPluginIntrospectionService introspectionService;
    private final LifecyclePhaseAnalyzer phaseAnalyzer;
    private final Log log;
    private final boolean verbose;
    private final Map<String, GoalBehavior> analysisCache = new ConcurrentHashMap<>();
    
    public EnhancedDynamicGoalAnalysisService(MavenSession session, ExecutionPlanAnalysisService executionPlanAnalysis, 
                                            LifecycleExecutor lifecycleExecutor, DefaultLifecycles defaultLifecycles,
                                            Log log, boolean verbose) {
        this.session = session;
        this.executionPlanAnalysis = executionPlanAnalysis;
        this.introspectionService = new MavenPluginIntrospectionService(session, lifecycleExecutor, log, verbose);
        this.phaseAnalyzer = new LifecyclePhaseAnalyzer(defaultLifecycles, log, verbose);
        this.log = log;
        this.verbose = verbose;
    }
    
    /**
     * Analyze a goal to determine its behavior and requirements using dynamic Maven API analysis.
     */
    public GoalBehavior analyzeGoal(String goal, MavenProject project) {
        String cacheKey = project.getId() + ":" + goal;
        
        return analysisCache.computeIfAbsent(cacheKey, k -> {
            if (verbose && log != null) {
                log.debug("Analyzing goal behavior using dynamic Maven APIs: " + goal);
            }
            
            GoalBehavior behavior = new GoalBehavior();
            
            // 1. Maven API-based plugin introspection (primary analysis)
            MavenPluginIntrospectionService.GoalIntrospectionResult introspectionResult = 
                introspectionService.analyzeGoal(goal, project);
            behavior = behavior.merge(introspectionResult.toGoalBehavior());
            
            // 2. Dynamic lifecycle phase analysis using Maven APIs (replaces hardcoded switch)
            String phase = executionPlanAnalysis.findPhaseForGoal(project, goal);
            if (phase != null) {
                GoalBehavior phaseBehavior = analyzeByPhaseUsingMavenAPIs(phase);
                behavior = behavior.merge(phaseBehavior);
            }
            
            // 3. Conservative fallback only if no analysis succeeded
            if (!behavior.hasAnyBehavior()) {
                behavior = analyzeMinimalFallback(goal);
            }
            
            if (verbose && log != null) {
                log.debug("Enhanced goal analysis for " + goal + ": sources=" + behavior.processesSources() + 
                         ", test=" + behavior.isTestRelated() + ", resources=" + behavior.needsResources());
            }
            
            return behavior;
        });
    }
    
    /**
     * Analyze goal behavior based on Maven lifecycle phase using Maven APIs instead of hardcoded switch.
     * This method demonstrates the replacement of hardcoded phase categorization with dynamic analysis.
     */
    private GoalBehavior analyzeByPhaseUsingMavenAPIs(String phase) {
        if (phase == null) {
            return new GoalBehavior();
        }
        
        if (verbose && log != null) {
            log.debug("Performing dynamic phase analysis using Maven APIs for phase: " + phase);
        }
        
        // Use LifecyclePhaseAnalyzer instead of hardcoded switch statement
        return phaseAnalyzer.toGoalBehavior(phase);
    }
    
    /**
     * Legacy method using hardcoded switch for comparison/fallback.
     * This shows the old approach that is being replaced by dynamic Maven API analysis.
     */
    @Deprecated
    private GoalBehavior analyzeByPhaseHardcoded(String phase) {
        GoalBehavior behavior = new GoalBehavior();
        
        if (phase == null) return behavior;
        
        // This is the OLD hardcoded approach being replaced
        switch (phase) {
            // Source processing phases
            case "generate-sources":
            case "process-sources":
            case "compile":
            case "process-classes":
                behavior.setProcessesSources(true);
                break;
                
            // Test phases
            case "generate-test-sources":
            case "process-test-sources":
            case "test-compile":
            case "process-test-classes":
            case "test":
            case "integration-test":
                behavior.setTestRelated(true);
                behavior.setProcessesSources(true);
                break;
                
            // Resource phases
            case "generate-resources":
            case "process-resources":
                behavior.setNeedsResources(true);
                break;
                
            case "process-test-resources":
                behavior.setNeedsResources(true);
                behavior.setTestRelated(true);
                break;
                
            // Packaging phases typically don't need direct source access
            case "package":
            case "verify":
            case "install":
            case "deploy":
                break;
        }
        
        return behavior;
    }
    
    /**
     * Minimal fallback analysis using goal name patterns.
     * This is much more conservative than the old hardcoded approach.
     */
    private GoalBehavior analyzeMinimalFallback(String goal) {
        GoalBehavior behavior = new GoalBehavior();
        
        // Only set flags for very obvious cases
        if (goal.equals("compile") || goal.equals("testCompile")) {
            behavior.setProcessesSources(true);
            if (goal.equals("testCompile")) {
                behavior.setTestRelated(true);
            }
        } else if (goal.equals("test")) {
            behavior.setTestRelated(true);
            behavior.setProcessesSources(true);
        } else if (goal.equals("resources") || goal.equals("testResources")) {
            behavior.setNeedsResources(true);
            if (goal.equals("testResources")) {
                behavior.setTestRelated(true);
            }
        }
        
        return behavior;
    }
    
    /**
     * Get detailed phase analysis for debugging/inspection
     */
    public LifecyclePhaseAnalyzer.PhaseAnalysis getPhaseAnalysis(String phase) {
        return phaseAnalyzer.analyzePhase(phase);
    }
    
    /**
     * Get all lifecycle phases using Maven APIs
     */
    public java.util.Set<String> getAllLifecyclePhases() {
        return phaseAnalyzer.getAllLifecyclePhases();
    }
    
    /**
     * Get phases for a specific lifecycle using Maven APIs
     */
    public java.util.List<String> getPhasesForLifecycle(String lifecycleId) {
        return phaseAnalyzer.getPhasesForLifecycle(lifecycleId);
    }
    
    /**
     * Clear all caches (useful for testing)
     */
    public void clearCaches() {
        analysisCache.clear();
        phaseAnalyzer.clearCache();
    }
    
    /**
     * Get cache statistics for monitoring
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("goalAnalysisCache", analysisCache.size());
        stats.put("phaseAnalysisCache", phaseAnalyzer.getCacheStats());
        return stats;
    }
    
    /**
     * Demonstration method showing Maven API usage for lifecycle introspection.
     * This shows various ways to use DefaultLifecycles and Lifecycle APIs.
     */
    public void demonstrateMavenLifecycleAPIs() {
        if (log != null) {
            log.info("=== Maven Lifecycle API Demonstration ===");
            
            // Show all lifecycle phases using Maven APIs
            java.util.Set<String> allPhases = getAllLifecyclePhases();
            log.info("Total phases found using Maven APIs: " + allPhases.size());
            
            // Show phases by lifecycle
            for (String lifecycleId : java.util.Arrays.asList("default", "clean", "site")) {
                java.util.List<String> phases = getPhasesForLifecycle(lifecycleId);
                log.info(lifecycleId + " lifecycle phases (" + phases.size() + "): " + phases);
            }
            
            // Show phase analysis examples
            String[] samplePhases = {"compile", "test", "package", "process-resources", "clean"};
            for (String phase : samplePhases) {
                LifecyclePhaseAnalyzer.PhaseAnalysis analysis = getPhaseAnalysis(phase);
                log.info("Phase '" + phase + "' analysis: " + analysis.getSummary());
            }
        }
    }
}