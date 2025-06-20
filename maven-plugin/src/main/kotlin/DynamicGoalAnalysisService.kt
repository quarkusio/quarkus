import org.apache.maven.execution.MavenSession
import org.apache.maven.lifecycle.DefaultLifecycles
import org.apache.maven.lifecycle.LifecycleExecutor
import org.apache.maven.plugin.logging.Log
import org.apache.maven.project.MavenProject
import java.util.concurrent.ConcurrentHashMap

/**
 * Service that dynamically analyzes Maven goals using Maven APIs instead of hardcoded patterns.
 * Uses MojoExecution, plugin configuration, and lifecycle phase information to determine 
 * goal behavior and requirements.
 */
class DynamicGoalAnalysisService(
    private val session: MavenSession,
    private val executionPlanAnalysis: ExecutionPlanAnalysisService,
    lifecycleExecutor: LifecycleExecutor,
    defaultLifecycles: DefaultLifecycles,
    private val log: Log,
    private val verbose: Boolean
) {
    
    private val introspectionService = MavenPluginIntrospectionService(session, lifecycleExecutor, log, verbose)
    private val phaseAnalyzer = LifecyclePhaseAnalyzer(defaultLifecycles, log, verbose)
    private val analysisCache = ConcurrentHashMap<String, GoalBehavior>()
    
    /**
     * Analyze a goal to determine its behavior and requirements.
     */
    fun analyzeGoal(goal: String, project: MavenProject): GoalBehavior {
        val cacheKey = "${project.id}:$goal"
        
        return analysisCache.computeIfAbsent(cacheKey) {
            if (verbose) {
                log.debug("Analyzing goal behavior for: $goal")
            }
            
            var behavior = GoalBehavior()
            
            // 1. Use Maven API-based introspection (primary analysis)
            val introspectionResult = introspectionService.analyzeGoal(goal, project)
            behavior = behavior.merge(introspectionResult.toGoalBehavior())
            
            // 2. Enhance with Maven API-based lifecycle phase analysis
            val phase = executionPlanAnalysis.findPhaseForGoal(project, goal)
            if (phase != null) {
                val phaseBehavior = phaseAnalyzer.toGoalBehavior(phase)
                behavior = behavior.merge(phaseBehavior)
            }
            
            // No fallback needed - Maven APIs provide complete analysis
            // If behavior is empty, it means the goal genuinely doesn't need
            // source files, test files, or resources (e.g., clean, validate, install)
            
            if (verbose) {
                log.debug("Goal $goal analysis: sources=${behavior.processesSources()}, " +
                         "test=${behavior.isTestRelated()}, resources=${behavior.needsResources()}")
            }
            
            behavior
        }
    }
    
    // Removed findPluginForGoal() - now handled by MavenPluginIntrospectionService
    // which uses Maven's execution plan APIs to find MojoExecution and analyze
    // plugin behavior dynamically using MojoDescriptor and parameter analysis
    
    // Removed hardcoded analyzePluginConfiguration() method!
    // This method contained hardcoded plugin-specific logic that has been replaced
    // with dynamic Maven API-based analysis in MavenPluginIntrospectionService.
    // 
    // The new approach:
    // 1. Uses MojoDescriptor.getParameters() to analyze actual plugin parameters
    // 2. Examines parameter types (java.io.File, etc.) to identify file/directory usage
    // 3. Analyzes parameter names and descriptions for semantic understanding
    // 4. Parses plugin configuration XML to understand actual configured paths
    // 5. Uses flexible pattern matching only as enhancement, not primary logic
    
    // Removed hardcoded analyzeByPhase() method!
    // This method contained a hardcoded switch statement with 20+ lifecycle phases
    // that has been completely replaced with Maven API-based dynamic analysis.
    // 
    // The new approach in LifecyclePhaseAnalyzer:
    // 1. Uses DefaultLifecycles.getPhaseToLifecycleMap() to get lifecycle context
    // 2. Uses Lifecycle.getPhases() to understand phase ordering and position
    // 3. Applies semantic analysis to phase names for behavior categorization
    // 4. Uses position-based analysis within lifecycle for additional context
    // 5. Provides 16 different phase categories for precise behavior detection
    // 
    // This replacement eliminates hardcoded phase knowledge and uses Maven's
    // own lifecycle metadata to understand phase behavior dynamically.
    
    // Removed placeholder analyzeMojoExecution() method!
    // MojoExecution analysis is now fully implemented in MavenPluginIntrospectionService
    // which provides comprehensive analysis of:
    // - MojoExecution metadata (plugin, phase, execution ID)
    // - MojoDescriptor parameter analysis
    // - Plugin configuration XML parsing
    // - Dynamic parameter type and semantic analysis
    
    // Removed analyzeMinimalFallback() method!
    // No fallback logic needed because Maven APIs provide complete analysis:
    // 1. MavenPluginIntrospectionService uses MojoDescriptor parameter analysis
    // 2. LifecyclePhaseAnalyzer uses DefaultLifecycles API for phase behavior
    // 3. If both return no behavior, the goal genuinely doesn't need files
    //    (e.g., clean, validate, install goals work with artifacts, not sources)
    // 
    // This eliminates the last remnant of hardcoded assumptions.
}