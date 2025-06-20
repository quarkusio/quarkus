import org.apache.maven.lifecycle.DefaultLifecycles
import org.apache.maven.lifecycle.Lifecycle
import org.apache.maven.plugin.logging.Log
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Service that dynamically analyzes Maven lifecycle phases using Maven APIs
 * instead of hardcoded phase categorization. Replaces hardcoded switch statements
 * with intelligent phase analysis based on Maven's lifecycle metadata.
 */
class LifecyclePhaseAnalyzer(
    private val defaultLifecycles: DefaultLifecycles?,
    private val log: Log?,
    private val verbose: Boolean
) {
    
    private val analysisCache = ConcurrentHashMap<String, PhaseAnalysis>()
    
    /**
     * Analyze a Maven lifecycle phase to determine its behavior and characteristics.
     * Uses Maven's DefaultLifecycles API and semantic analysis instead of hardcoded patterns.
     */
    fun analyzePhase(phase: String?): PhaseAnalysis {
        if (phase.isNullOrEmpty()) {
            return PhaseAnalysis(phase)
        }
        
        return analysisCache.computeIfAbsent(phase) { performPhaseAnalysis(it) }
    }
    
    /**
     * Convert PhaseAnalysis to GoalBehavior for backward compatibility
     */
    fun toGoalBehavior(phase: String): GoalBehavior {
        val analysis = analyzePhase(phase)
        return analysis.toGoalBehavior()
    }
    
    /**
     * Perform comprehensive phase analysis using Maven APIs and semantic analysis
     */
    private fun performPhaseAnalysis(phase: String): PhaseAnalysis {
        if (verbose && log != null) {
            log.debug("Performing dynamic analysis for phase: $phase")
        }
        
        val analysis = PhaseAnalysis(phase)
        
        // 1. Get lifecycle context using Maven API
        val lifecycle = getLifecycleForPhase(phase)
        if (lifecycle != null) {
            analysis.lifecycleId = lifecycle.id
            analysis.phasePosition = getPhasePosition(lifecycle, phase)
            analysis.lifecyclePhases = lifecycle.phases
        }
        
        // 2. Semantic analysis of phase name
        analyzePhaseNameSemantics(analysis)
        
        // 3. Position-based analysis within lifecycle
        analyzePhasePosition(analysis)
        
        // 4. Lifecycle-specific analysis
        analyzeLifecycleContext(analysis)
        
        if (verbose && log != null) {
            log.debug("Phase analysis for '$phase': ${analysis.summary}")
        }
        
        return analysis
    }
    
    /**
     * Get lifecycle containing the specified phase using Maven API
     */
    private fun getLifecycleForPhase(phase: String): Lifecycle? {
        return try {
            defaultLifecycles?.phaseToLifecycleMap?.get(phase)
        } catch (e: Exception) {
            if (verbose && log != null) {
                log.warn("Could not get lifecycle for phase '$phase': ${e.message}")
            }
            null
        }
    }
    
    /**
     * Get position of phase within its lifecycle (0-based index)
     */
    private fun getPhasePosition(lifecycle: Lifecycle, phase: String): Int {
        return try {
            lifecycle.phases.indexOf(phase)
        } catch (e: Exception) {
            -1
        }
    }
    
    /**
     * Analyze phase name using semantic patterns (replacement for hardcoded switch)
     */
    private fun analyzePhaseNameSemantics(analysis: PhaseAnalysis) {
        val phase = analysis.phase?.lowercase() ?: return
        
        // Source-related phases
        if (phase.contains("source") || phase == "compile" || phase.contains("classes")) {
            analysis.addCategory(PhaseCategory.SOURCE_PROCESSING)
            if (phase.contains("compile")) {
                analysis.addCategory(PhaseCategory.COMPILATION)
            }
        }
        
        // Test-related phases  
        if (phase.contains("test")) {
            analysis.addCategory(PhaseCategory.TEST_RELATED)
            if (phase.contains("compile")) {
                analysis.addCategory(PhaseCategory.COMPILATION)
            }
        }
        
        // Resource-related phases
        if (phase.contains("resource")) {
            analysis.addCategory(PhaseCategory.RESOURCE_PROCESSING)
        }
        
        // Generation phases
        if (phase.contains("generate")) {
            analysis.addCategory(PhaseCategory.GENERATION)
        }
        
        // Processing phases
        if (phase.contains("process")) {
            analysis.addCategory(PhaseCategory.PROCESSING)
        }
        
        // Packaging phases
        if (phase == "package" || phase.contains("package")) {
            analysis.addCategory(PhaseCategory.PACKAGING)
        }
        
        // Verification phases
        if (phase == "verify" || phase.contains("verify")) {
            analysis.addCategory(PhaseCategory.VERIFICATION)
        }
        
        // Deployment/Installation phases
        if (phase == "install" || phase == "deploy" || phase.contains("deploy")) {
            analysis.addCategory(PhaseCategory.DEPLOYMENT)
        }
        
        // Validation phases
        if (phase == "validate" || phase.contains("validate")) {
            analysis.addCategory(PhaseCategory.VALIDATION)
        }
        
        // Integration test phases
        if (phase.contains("integration")) {
            analysis.addCategory(PhaseCategory.TEST_RELATED)
            analysis.addCategory(PhaseCategory.INTEGRATION)
        }
    }
    
    /**
     * Analyze phase based on its position within the lifecycle
     */
    private fun analyzePhasePosition(analysis: PhaseAnalysis) {
        val phases = analysis.lifecyclePhases ?: return
        val position = analysis.phasePosition
        if (position < 0) return
        
        val totalPhases = phases.size
        
        // Early phases (first third) - typically setup/preparation
        when {
            position < totalPhases / 3 -> {
                analysis.addCategory(PhaseCategory.EARLY_PHASE)
            }
            // Middle phases - typically compilation/testing  
            position < (2 * totalPhases) / 3 -> {
                analysis.addCategory(PhaseCategory.MIDDLE_PHASE)
            }
            // Late phases - typically packaging/deployment
            else -> {
                analysis.addCategory(PhaseCategory.LATE_PHASE)
            }
        }
    }
    
    /**
     * Analyze phase based on its lifecycle context
     */
    private fun analyzeLifecycleContext(analysis: PhaseAnalysis) {
        when (analysis.lifecycleId) {
            "clean" -> analysis.addCategory(PhaseCategory.CLEANUP)
            "site" -> analysis.addCategory(PhaseCategory.DOCUMENTATION)
            "default" -> analysis.addCategory(PhaseCategory.BUILD)
        }
    }
    
    /**
     * Get all phases from all lifecycles using Maven API
     */
    fun getAllLifecyclePhases(): Set<String> {
        val allPhases = LinkedHashSet<String>()
        
        try {
            defaultLifecycles?.lifeCycles?.forEach { lifecycle ->
                lifecycle.phases?.let { phases ->
                    allPhases.addAll(phases)
                }
            }
        } catch (e: Exception) {
            if (verbose && log != null) {
                log.warn("Could not retrieve all lifecycle phases: ${e.message}")
            }
        }
        
        return allPhases
    }
    
    /**
     * Get phases for a specific lifecycle using Maven API
     */
    fun getPhasesForLifecycle(lifecycleId: String): List<String> {
        return try {
            defaultLifecycles?.lifeCycles
                ?.find { it.id == lifecycleId }
                ?.phases
                ?.toList()
                ?: emptyList()
        } catch (e: Exception) {
            if (verbose && log != null) {
                log.warn("Could not get phases for lifecycle '$lifecycleId': ${e.message}")
            }
            emptyList()
        }
    }
    
    /**
     * Clear analysis cache (useful for testing)
     */
    fun clearCache() {
        analysisCache.clear()
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheStats(): Map<String, Any> {
        return mapOf(
            "cachedAnalyses" to analysisCache.size,
            "analyzedPhases" to analysisCache.keys.toList()
        )
    }
    
    /**
     * Comprehensive phase analysis result
     */
    class PhaseAnalysis(@get:JvmName("getPhase") val phase: String?) {
        var lifecycleId: String? = null
        var phasePosition: Int = -1
        var lifecyclePhases: List<String>? = null
        private val categories = EnumSet.noneOf(PhaseCategory::class.java)
        
        fun getCategories(): Set<PhaseCategory> = 
            if (categories.isEmpty()) EnumSet.noneOf(PhaseCategory::class.java) else EnumSet.copyOf(categories)
        
        fun addCategory(category: PhaseCategory) { 
            categories.add(category) 
        }
        
        fun hasCategory(category: PhaseCategory): Boolean = categories.contains(category)
        
        /**
         * Convert to GoalBehavior for backward compatibility
         */
        fun toGoalBehavior(): GoalBehavior {
            val behavior = GoalBehavior()
            
            // Source processing
            if (hasCategory(PhaseCategory.SOURCE_PROCESSING) || hasCategory(PhaseCategory.COMPILATION)) {
                behavior.setProcessesSources(true)
            }
            
            // Test related
            if (hasCategory(PhaseCategory.TEST_RELATED)) {
                behavior.setTestRelated(true)
                // Test phases typically also process sources
                behavior.setProcessesSources(true)
            }
            
            // Resource processing
            if (hasCategory(PhaseCategory.RESOURCE_PROCESSING)) {
                behavior.setNeedsResources(true)
            }
            
            return behavior
        }
        
        /**
         * Get summary of analysis for logging
         */
        val summary: String
            get() = "lifecycle=$lifecycleId, position=$phasePosition/${lifecyclePhases?.size ?: 0}, " +
                    "categories=${categories.joinToString(",") { it.name }}"
    }
    
    /**
     * Phase categories determined by dynamic analysis
     */
    enum class PhaseCategory {
        SOURCE_PROCESSING,
        TEST_RELATED,
        RESOURCE_PROCESSING,
        GENERATION,
        PROCESSING,
        COMPILATION,
        PACKAGING,
        VERIFICATION,
        DEPLOYMENT,
        VALIDATION,
        INTEGRATION,
        CLEANUP,
        DOCUMENTATION,
        BUILD,
        EARLY_PHASE,
        MIDDLE_PHASE,
        LATE_PHASE
    }
}