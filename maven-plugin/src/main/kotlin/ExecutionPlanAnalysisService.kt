import org.apache.maven.execution.MavenSession
import org.apache.maven.lifecycle.LifecycleExecutor
import org.apache.maven.lifecycle.MavenExecutionPlan
import org.apache.maven.lifecycle.DefaultLifecycles
import org.apache.maven.plugin.MojoExecution
import org.apache.maven.plugin.logging.Log
import org.apache.maven.project.MavenProject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max

/**
 * Service that analyzes Maven execution plans upfront and stores all results for efficient querying.
 * Pre-computes all Maven analysis to avoid performance bottlenecks during target dependency analysis.
 */
class ExecutionPlanAnalysisService(
    private val log: Log,
    private val verbose: Boolean,
    private val lifecycleExecutor: LifecycleExecutor,
    private val session: MavenSession,
    private val defaultLifecycles: DefaultLifecycles
) {
    
    // Pre-computed analysis results per project
    private val analysisCache = ConcurrentHashMap<String, ProjectExecutionAnalysis>()
    
    // Global execution plan cache to avoid recalculating same plans
    private val executionPlanCache = ConcurrentHashMap<String, MavenExecutionPlan>()
    
    // Thread pool for parallel execution plan analysis
    private val executorService: ExecutorService = Executors.newFixedThreadPool(
        max(2, Runtime.getRuntime().availableProcessors() - 1)
    )
    
    // Pre-computed lifecycle information (shared across all projects)
    private val allLifecyclePhases: Set<String>
    private val defaultLifecyclePhases: List<String>
    private val cleanLifecyclePhases: List<String>
    private val siteLifecyclePhases: List<String>
    private val phaseToLifecycleMap: Map<String, org.apache.maven.lifecycle.Lifecycle>
    
    init {
        // Pre-compute all lifecycle information once
        allLifecyclePhases = computeAllLifecyclePhases()
        defaultLifecyclePhases = computeLifecyclePhases("default")
        cleanLifecyclePhases = computeLifecyclePhases("clean")
        siteLifecyclePhases = computeLifecyclePhases("site")
        phaseToLifecycleMap = computePhaseToLifecycleMap()
        
        if (verbose) {
            log.info("Pre-computed lifecycle information: ${allLifecyclePhases.size} total phases")
        }
    }
    
    /**
     * Pre-analyze all projects in the reactor upfront to avoid performance bottlenecks
     * OPTIMIZED: Lazy loading - only analyze projects when actually needed
     */
    fun preAnalyzeAllProjects(reactorProjects: List<MavenProject>?) {
        if (reactorProjects.isNullOrEmpty()) {
            return
        }
        
        val startTime = System.currentTimeMillis()
        
        // OPTIMIZATION: Don't pre-analyze all projects upfront - use lazy loading instead
        // This dramatically reduces startup time while maintaining full coverage
        if (verbose) {
            log.info("Initialized lazy analysis for ${reactorProjects.size} projects (analysis will be performed on-demand)")
        }
        
        val duration = System.currentTimeMillis() - startTime
        if (verbose) {
            log.info("Completed analysis initialization in ${duration}ms")
        }
    }
    
    /**
     * Get the LifecycleExecutor for use by other services
     */
    fun getLifecycleExecutor(): LifecycleExecutor = lifecycleExecutor
    
    /**
     * Get the DefaultLifecycles for use by other services
     */
    fun getDefaultLifecycles(): DefaultLifecycles = defaultLifecycles
    
    /**
     * Get or compute execution analysis for a project
     */
    fun getAnalysis(project: MavenProject): ProjectExecutionAnalysis {
        val projectKey = MavenUtils.formatProjectKey(project)
        return analysisCache.computeIfAbsent(projectKey) { analyzeProject(project) }
    }
    
    /**
     * Find the phase for a specific goal
     */
    fun findPhaseForGoal(project: MavenProject?, goal: String?): String? {
        if (project == null || goal.isNullOrEmpty()) {
            return null
        }
        val analysis = getAnalysis(project)
        return analysis.getPhaseForGoal(goal)
    }
    
    /**
     * Get all goals for a specific phase
     */
    fun getGoalsForPhase(project: MavenProject?, phase: String?): List<String> {
        if (project == null || phase == null) {
            return emptyList()
        }
        val analysis = getAnalysis(project)
        return analysis.getGoalsForPhase(phase)
    }
    
    /**
     * Get all lifecycle phases from all lifecycles (default, clean, site)
     */
    fun getAllLifecyclePhases(): Set<String> = LinkedHashSet(allLifecyclePhases)
    
    /**
     * Get default lifecycle phases (validate, compile, test, package, verify, install, deploy)
     */
    fun getDefaultLifecyclePhases(): List<String> = ArrayList(defaultLifecyclePhases)
    
    /**
     * Get clean lifecycle phases (pre-clean, clean, post-clean)
     */
    fun getCleanLifecyclePhases(): List<String> = ArrayList(cleanLifecyclePhases)
    
    /**
     * Get site lifecycle phases (pre-site, site, post-site, site-deploy)
     */
    fun getSiteLifecyclePhases(): List<String> = ArrayList(siteLifecyclePhases)
    
    /**
     * Get the lifecycle that contains the specified phase
     */
    fun getLifecycleForPhase(phase: String?): org.apache.maven.lifecycle.Lifecycle? {
        return if (phase == null) null else phaseToLifecycleMap[phase]
    }
    
    /**
     * Get all goals that should be completed by the time the specified phase finishes.
     * This includes goals from all phases up to and including the target phase.
     */
    fun getGoalsCompletedByPhase(project: MavenProject?, targetPhase: String?): List<String> {
        if (project == null || targetPhase == null) {
            return emptyList()
        }
        
        val completedGoals = mutableListOf<String>()
        val lifecycle = getLifecycleForPhase(targetPhase) ?: return completedGoals
        
        val lifecyclePhases = lifecycle.phases
        val targetPhaseIndex = lifecyclePhases.indexOf(targetPhase)
        
        if (targetPhaseIndex == -1) {
            return completedGoals
        }
        
        // Use Set to avoid duplicates
        val uniqueGoals = LinkedHashSet<String>()
        
        // Get goals from all phases up to and including the target phase
        for (i in 0..targetPhaseIndex) {
            val phase = lifecyclePhases[i]
            val phaseGoals = getGoalsForPhase(project, phase)
            
            // Convert to proper plugin:goal format
            for (goal in phaseGoals) {
                if (goal.contains(":")) {
                    // Goal is already in plugin:goal format
                    val parts = goal.split(":")
                    when {
                        parts.size >= 3 -> {
                            // Format: groupId:artifactId:goal
                            val groupId = parts[0]
                            val artifactId = parts[1]
                            val goalName = parts[2]
                            uniqueGoals.add("$groupId:$artifactId:$goalName")
                        }
                        parts.size == 2 -> {
                            // Format: plugin:goal - convert to full format
                            val plugin = parts[0]
                            val goalName = parts[1]
                            uniqueGoals.add("org.apache.maven.plugins:maven-$plugin-plugin:$goalName")
                        }
                    }
                }
            }
        }
        
        completedGoals.addAll(uniqueGoals)
        return completedGoals
    }
    
    /**
     * Pre-compute all lifecycle phases from all lifecycles
     */
    private fun computeAllLifecyclePhases(): Set<String> {
        val allPhases = LinkedHashSet<String>()
        allPhases.addAll(computeLifecyclePhases("default"))
        allPhases.addAll(computeLifecyclePhases("clean"))
        allPhases.addAll(computeLifecyclePhases("site"))
        return allPhases
    }
    
    /**
     * Pre-compute lifecycle phases for a specific lifecycle ID
     */
    private fun computeLifecyclePhases(lifecycleId: String): List<String> {
        if (lifecycleId.isEmpty()) {
            return emptyList()
        }

        return try {
            defaultLifecycles.lifeCycles
                .find { it.id == lifecycleId && it.phases != null }
                ?.phases
                ?.toList()
                ?: emptyList()
        } catch (e: Exception) {
            if (verbose) {
                log.warn("Could not access Maven lifecycle '$lifecycleId': ${e.message}")
            }
            emptyList()
        }
    }
    
    /**
     * Pre-compute phase to lifecycle mapping for efficient lookups
     */
    private fun computePhaseToLifecycleMap(): Map<String, org.apache.maven.lifecycle.Lifecycle> {
        return try {
            defaultLifecycles.phaseToLifecycleMap
        } catch (e: Exception) {
            if (verbose) {
                log.warn("Could not build phase to lifecycle map: ${e.message}")
            }
            emptyMap()
        }
    }
    
    /**
     * Analyze a project's execution plans and cache the results
     * OPTIMIZED: Cache sharing between similar projects + essential phases only
     */
    private fun analyzeProject(project: MavenProject): ProjectExecutionAnalysis {
        if (verbose) {
            log.debug("Analyzing execution plans for project: ${project.artifactId}")
        }
        
        // OPTIMIZATION: Check if we can reuse analysis from a similar project
        findSimilarProjectAnalysis(project)?.let { cachedAnalysis ->
            if (verbose) {
                log.debug("Reusing analysis from similar project for: ${project.artifactId}")
            }
            return cachedAnalysis
        }
        
        val analysis = ProjectExecutionAnalysis()
        
        // OPTIMIZATION: Analyze phases in parallel for better performance
        val essentialPhases = getEssentialPhases()
        
        // Create futures for parallel execution plan calculation
        val futures = essentialPhases.map { phase ->
            CompletableFuture.runAsync({
                try {
                    // OPTIMIZATION: Cache execution plans globally to avoid recalculation
                    val planKey = createExecutionPlanKey(project, phase)
                    val executionPlan = executionPlanCache[planKey] ?: run {
                        try {
                            val plan = lifecycleExecutor.calculateExecutionPlan(session, phase)
                            if (plan != null) {
                                executionPlanCache[planKey] = plan
                            }
                            plan
                        } catch (e: Exception) {
                            if (verbose) {
                                log.warn("Could not calculate execution plan for $phase: ${e.message}")
                            }
                            null
                        }
                    }
                    
                    executionPlan?.let {
                        analysis.addExecutionPlan(phase, it)
                        
                        if (verbose) {
                            log.debug("Analyzed ${it.mojoExecutions.size} executions for phase: $phase")
                        }
                    }
                } catch (e: Exception) {
                    if (verbose) {
                        log.warn("Could not calculate execution plan for $phase: ${e.message}")
                    }
                }
            }, executorService)
        }
        
        // Wait for all phases to complete
        CompletableFuture.allOf(*futures.toTypedArray()).join()
        
        if (verbose) {
            log.debug("Completed analysis for ${project.artifactId}: " +
                     "${analysis.getAllPhases().size} phases, " + 
                     "${analysis.getAllGoals().size} goals")
        }
        
        return analysis
    }
    
    /**
     * Clear cache (useful for testing or memory management)
     */
    fun clearCache() {
        analysisCache.clear()
        executionPlanCache.clear()
    }
    
    /**
     * Shutdown the executor service
     */
    fun shutdown() {
        executorService.shutdown()
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheStats(): Map<String, Any> {
        return mapOf(
            "cachedProjects" to analysisCache.size,
            "cachedExecutionPlans" to executionPlanCache.size,
            "projectKeys" to ArrayList(analysisCache.keys)
        )
    }
    
    // ========================================
    // Target Name Utilities (moved from MavenUtils)
    // ========================================
    
    companion object {
        /**
         * Generate target name from artifact ID and goal
         */
        @JvmStatic
        fun getTargetName(artifactId: String?, goal: String): String {
            val pluginName = normalizePluginName(artifactId)
            return "$pluginName:$goal"
        }
        
        /**
         * Extract goal name from target name (e.g., "compiler:compile" -> "compile")
         */
        @JvmStatic
        fun extractGoalFromTargetName(targetName: String?): String? {
            if (targetName == null || !targetName.contains(":")) {
                return targetName
            }
            return targetName.substring(targetName.lastIndexOf(":") + 1)
        }
        
        /**
         * Normalize plugin artifact ID to plugin name by removing common suffixes
         */
        @JvmStatic
        fun normalizePluginName(artifactId: String?): String? {
            return artifactId?.replace("-maven-plugin", "")?.replace("-plugin", "")
        }
        
        /**
         * Get common goals for well-known plugins
         */
        @JvmStatic
        fun getCommonGoalsForPlugin(artifactId: String?): List<String> {
            return when {
                artifactId == null -> emptyList()
                artifactId.contains("compiler") -> listOf("compile", "testCompile")
                artifactId.contains("surefire") -> listOf("test")
                artifactId.contains("quarkus") -> listOf("dev", "build")
                artifactId.contains("spring-boot") -> listOf("run", "repackage")
                else -> emptyList()
            }
        }
    }
    
    fun getGoalOutputs(goal: String, projectRootToken: String, project: MavenProject): List<String> {
        // Simplified implementation without hardcoded patterns
        return emptyList()
    }
    
    /**
     * Get essential phases that are commonly used - optimization to avoid analyzing all phases
     */
    private fun getEssentialPhases(): Set<String> {
        return setOf(
            // Core default lifecycle phases
            "validate",
            "compile",
            "test",
            "package",
            "verify",
            "install",
            "deploy",
            // Clean lifecycle
            "clean",
            // Site lifecycle
            "site"
        )
    }
    
    /**
     * Find a similar project that we can reuse analysis from
     * OPTIMIZATION: Projects with same packaging + plugin signature can share analysis
     */
    private fun findSimilarProjectAnalysis(project: MavenProject): ProjectExecutionAnalysis? {
        val projectSignature = createProjectSignature(project)
        
        // Look for a cached analysis with the same signature
        return analysisCache.entries
            .find { it.key.contains(projectSignature) }
            ?.value
    }
    
    /**
     * Create a signature for a project based on packaging and plugins
     */
    private fun createProjectSignature(project: MavenProject): String {
        val signature = StringBuilder()
        
        // Add packaging type
        project.packaging?.let {
            signature.append("pkg:").append(it).append(";")
        }
        
        // Add plugin signature (simplified)
        project.buildPlugins?.let { plugins ->
            if (plugins.isNotEmpty()) {
                signature.append("plugins:").append(plugins.size).append(";")
                
                // Add key plugins that affect execution plan
                val hasCompiler = plugins.any { it.artifactId.contains("compiler") }
                val hasSurefire = plugins.any { it.artifactId.contains("surefire") }
                
                if (hasCompiler) signature.append("compiler;")
                if (hasSurefire) signature.append("surefire;")
            }
        }
        
        return signature.toString()
    }
    
    /**
     * Create a key for caching execution plans
     * Uses project signature + phase to create unique cache keys
     */
    private fun createExecutionPlanKey(project: MavenProject, phase: String): String {
        val projectSignature = createProjectSignature(project)
        return "$projectSignature|phase:$phase"
    }
    
    /**
     * Container for analyzed execution plan data for a single project
     */
    class ProjectExecutionAnalysis {
        private val goalToPhaseMap = HashMap<String, String>()
        private val phaseToGoalsMap = HashMap<String, MutableList<String>>()
        private val allPhases = LinkedHashSet<String>()
        private val allGoals = LinkedHashSet<String>()
        private val goalToExecutionInfo = HashMap<String, ExecutionInfo>()
        
        /**
         * Add execution plan data from a lifecycle endpoint
         */
        fun addExecutionPlan(endpoint: String, executionPlan: MavenExecutionPlan) {
            for (mojoExecution in executionPlan.mojoExecutions) {
                val goal = mojoExecution.goal
                val phase = mojoExecution.lifecyclePhase
                val pluginArtifactId = mojoExecution.plugin.artifactId
                
                if (goal != null && phase != null) {
                    // Store goal-to-phase mapping
                    goalToPhaseMap[goal] = phase
                    
                    // Store detailed execution info
                    val execInfo = ExecutionInfo(
                        goal, phase, pluginArtifactId,
                        mojoExecution.executionId,
                        "${mojoExecution.plugin.groupId}:$pluginArtifactId"
                    )
                    goalToExecutionInfo[goal] = execInfo
                    
                    // Handle plugin:goal format too
                    val targetName = getTargetName(pluginArtifactId, goal)
                    goalToPhaseMap[targetName] = phase
                    goalToExecutionInfo[targetName] = execInfo
                    
                    // Track phases and goals
                    allPhases.add(phase)
                    allGoals.add(goal)
                    allGoals.add(targetName)
                    
                    // Build phase-to-goals mapping
                    phaseToGoalsMap.computeIfAbsent(phase) { mutableListOf() }.add(targetName)
                }
            }
        }
        
        fun getPhaseForGoal(goal: String): String? = goalToPhaseMap[goal]
        
        fun getGoalsForPhase(phase: String): List<String> = phaseToGoalsMap[phase] ?: emptyList()
        
        fun getAllPhases(): Set<String> = LinkedHashSet(allPhases)
        
        fun getAllGoals(): Set<String> = LinkedHashSet(allGoals)
        
        fun getExecutionInfo(goal: String): ExecutionInfo? = goalToExecutionInfo[goal]
        
        fun getPhaseToGoalsMap(): Map<String, List<String>> = HashMap(phaseToGoalsMap)
    }
    
    /**
     * Detailed information about a mojo execution
     */
    data class ExecutionInfo(
        val goal: String,
        val phase: String,
        val pluginArtifactId: String,
        val executionId: String,
        val pluginKey: String
    )
}