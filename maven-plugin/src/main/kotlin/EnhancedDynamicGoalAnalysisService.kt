import org.apache.maven.execution.MavenSession
import org.apache.maven.lifecycle.LifecycleExecutor
import org.apache.maven.lifecycle.DefaultLifecycles
import org.apache.maven.plugin.logging.Log
import org.apache.maven.project.MavenProject
import java.util.concurrent.ConcurrentHashMap

/**
 * Enhanced version of DynamicGoalAnalysisService that uses Maven APIs for dynamic
 * lifecycle phase analysis instead of hardcoded phase categorization.
 * 
 * This service demonstrates how to replace hardcoded switch statements with
 * intelligent Maven API-based phase analysis using DefaultLifecycles and Lifecycle APIs.
 */
class EnhancedDynamicGoalAnalysisService(
    private val session: MavenSession,
    private val executionPlanAnalysis: ExecutionPlanAnalysisService,
    lifecycleExecutor: LifecycleExecutor,
    defaultLifecycles: DefaultLifecycles,
    private val log: Log?,
    private val verbose: Boolean
) {
    
    private val introspectionService = MavenPluginIntrospectionService(session, lifecycleExecutor, log!!, verbose)
    private val phaseAnalyzer = LifecyclePhaseAnalyzer(defaultLifecycles, log, verbose)
    private val analysisCache = ConcurrentHashMap<String, GoalBehavior>()
    
    /**
     * Analyze a goal to determine its behavior and requirements using dynamic Maven API analysis.
     */
    fun analyzeGoal(goal: String, project: MavenProject): GoalBehavior {
        val cacheKey = "${project.id}:$goal"
        
        return analysisCache.computeIfAbsent(cacheKey) {
            if (verbose && log != null) {
                log.debug("Analyzing goal behavior using dynamic Maven APIs: $goal")
            }
            
            var behavior = GoalBehavior()
            
            // 1. Maven API-based plugin introspection (primary analysis)
            val introspectionResult = introspectionService.analyzeGoal(goal, project)
            behavior = behavior.merge(introspectionResult.toGoalBehavior())
            
            // 2. Dynamic lifecycle phase analysis using Maven APIs (replaces hardcoded switch)
            val phase = executionPlanAnalysis.findPhaseForGoal(project, goal)
            if (phase != null) {
                val phaseBehavior = analyzeByPhaseUsingMavenAPIs(phase)
                behavior = behavior.merge(phaseBehavior)
            }
            
            // 3. Conservative fallback only if no analysis succeeded
            if (!behavior.hasAnyBehavior()) {
                behavior = analyzeMinimalFallback(goal)
            }
            
            if (verbose && log != null) {
                log.debug("Enhanced goal analysis for $goal: sources=${behavior.processesSources()}, " +
                         "test=${behavior.isTestRelated()}, resources=${behavior.needsResources()}")
            }
            
            behavior
        }
    }
    
    /**
     * Analyze goal behavior based on Maven lifecycle phase using Maven APIs instead of hardcoded switch.
     * This method demonstrates the replacement of hardcoded phase categorization with dynamic analysis.
     */
    private fun analyzeByPhaseUsingMavenAPIs(phase: String?): GoalBehavior {
        if (phase == null) {
            return GoalBehavior()
        }
        
        if (verbose && log != null) {
            log.debug("Performing dynamic phase analysis using Maven APIs for phase: $phase")
        }
        
        // Use LifecyclePhaseAnalyzer instead of hardcoded switch statement
        return phaseAnalyzer.toGoalBehavior(phase)
    }
    
    /**
     * Legacy method using hardcoded switch for comparison/fallback.
     * This shows the old approach that is being replaced by dynamic Maven API analysis.
     */
    @Deprecated("Use analyzeByPhaseUsingMavenAPIs instead")
    private fun analyzeByPhaseHardcoded(phase: String?): GoalBehavior {
        val behavior = GoalBehavior()
        
        if (phase == null) return behavior
        
        // This is the OLD hardcoded approach being replaced
        when (phase) {
            // Source processing phases
            "generate-sources", "process-sources", "compile", "process-classes" -> {
                behavior.setProcessesSources(true)
            }
                
            // Test phases
            "generate-test-sources", "process-test-sources", "test-compile", 
            "process-test-classes", "test", "integration-test" -> {
                behavior.setTestRelated(true)
                behavior.setProcessesSources(true)
            }
                
            // Resource phases
            "generate-resources", "process-resources" -> {
                behavior.setNeedsResources(true)
            }
                
            "process-test-resources" -> {
                behavior.setNeedsResources(true)
                behavior.setTestRelated(true)
            }
                
            // Packaging phases typically don't need direct source access
            "package", "verify", "install", "deploy" -> {
                // No specific behavior
            }
        }
        
        return behavior
    }
    
    /**
     * Minimal fallback analysis using goal name patterns.
     * This is much more conservative than the old hardcoded approach.
     */
    private fun analyzeMinimalFallback(goal: String): GoalBehavior {
        val behavior = GoalBehavior()
        
        // Only set flags for very obvious cases
        when (goal) {
            "compile" -> {
                behavior.setProcessesSources(true)
            }
            "testCompile" -> {
                behavior.setProcessesSources(true)
                behavior.setTestRelated(true)
            }
            "test" -> {
                behavior.setTestRelated(true)
                behavior.setProcessesSources(true)
            }
            "resources" -> {
                behavior.setNeedsResources(true)
            }
            "testResources" -> {
                behavior.setNeedsResources(true)
                behavior.setTestRelated(true)
            }
        }
        
        return behavior
    }
    
    /**
     * Get detailed phase analysis for debugging/inspection
     */
    fun getPhaseAnalysis(phase: String): LifecyclePhaseAnalyzer.PhaseAnalysis {
        return phaseAnalyzer.analyzePhase(phase)
    }
    
    /**
     * Get all lifecycle phases using Maven APIs
     */
    fun getAllLifecyclePhases(): Set<String> {
        return phaseAnalyzer.getAllLifecyclePhases()
    }
    
    /**
     * Get phases for a specific lifecycle using Maven APIs
     */
    fun getPhasesForLifecycle(lifecycleId: String): List<String> {
        return phaseAnalyzer.getPhasesForLifecycle(lifecycleId)
    }
    
    /**
     * Clear all caches (useful for testing)
     */
    fun clearCaches() {
        analysisCache.clear()
        phaseAnalyzer.clearCache()
    }
    
    /**
     * Get cache statistics for monitoring
     */
    fun getCacheStats(): Map<String, Any> {
        return mapOf(
            "goalAnalysisCache" to analysisCache.size,
            "phaseAnalysisCache" to phaseAnalyzer.getCacheStats()
        )
    }
    
    /**
     * Demonstration method showing Maven API usage for lifecycle introspection.
     * This shows various ways to use DefaultLifecycles and Lifecycle APIs.
     */
    fun demonstrateMavenLifecycleAPIs() {
        log?.let {
            it.info("=== Maven Lifecycle API Demonstration ===")
            
            // Show all lifecycle phases using Maven APIs
            val allPhases = getAllLifecyclePhases()
            it.info("Total phases found using Maven APIs: ${allPhases.size}")
            
            // Show phases by lifecycle
            listOf("default", "clean", "site").forEach { lifecycleId ->
                val phases = getPhasesForLifecycle(lifecycleId)
                it.info("$lifecycleId lifecycle phases (${phases.size}): $phases")
            }
            
            // Show phase analysis examples
            val samplePhases = arrayOf("compile", "test", "package", "process-resources", "clean")
            samplePhases.forEach { phase ->
                val analysis = getPhaseAnalysis(phase)
                it.info("Phase '$phase' analysis: ${analysis.summary}")
            }
        }
    }
}