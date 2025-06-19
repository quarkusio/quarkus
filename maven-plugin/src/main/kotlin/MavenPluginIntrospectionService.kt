import org.apache.maven.execution.MavenSession
import org.apache.maven.lifecycle.LifecycleExecutor
import org.apache.maven.lifecycle.MavenExecutionPlan
import org.apache.maven.plugin.MojoExecution
import org.apache.maven.plugin.descriptor.MojoDescriptor
import org.apache.maven.plugin.descriptor.Parameter
import org.apache.maven.plugin.logging.Log
import org.apache.maven.project.MavenProject
import org.codehaus.plexus.util.xml.Xpp3Dom
import java.util.concurrent.ConcurrentHashMap

/**
 * Service that uses Maven's introspection APIs to dynamically determine plugin and goal behavior
 * instead of hardcoded plugin-specific logic. This leverages PluginDescriptor, MojoDescriptor,
 * and parameter analysis to understand what files/directories a Mojo actually uses.
 */
class MavenPluginIntrospectionService(
    private val session: MavenSession,
    private val lifecycleExecutor: LifecycleExecutor,
    private val log: Log,
    private val verbose: Boolean
) {
    
    // Cache to avoid repeated introspection
    private val introspectionCache = ConcurrentHashMap<String, GoalIntrospectionResult>()
    
    /**
     * Analyze a goal using Maven's introspection APIs to determine its behavior dynamically.
     */
    fun analyzeGoal(goal: String, project: MavenProject): GoalIntrospectionResult {
        val cacheKey = "${project.id}:$goal"
        
        return introspectionCache.computeIfAbsent(cacheKey) {
            if (verbose) {
                log.debug("Performing dynamic introspection for goal: $goal")
            }
            
            val result = GoalIntrospectionResult(goal)
            
            try {
                // 1. Find MojoExecution for this goal
                val mojoExecution = findMojoExecution(goal, project)
                if (mojoExecution != null) {
                    analyzeMojoExecution(mojoExecution, result)
                    
                    // 2. Analyze using MojoDescriptor if available
                    mojoExecution.mojoDescriptor?.let { analyzeMojoDescriptor(it, result) }
                    
                    // 3. Analyze plugin configuration
                    analyzePluginConfiguration(mojoExecution, result)
                } else {
                    // No MojoExecution found - use fallback
                    if (verbose) {
                        log.debug("No MojoExecution found for goal $goal, using fallback analysis")
                    }
                    return@computeIfAbsent createFallbackResult(goal)
                }
                
            } catch (e: Exception) {
                if (verbose) {
                    log.warn("Introspection failed for goal $goal: ${e.message}")
                }
                // Fall back to minimal analysis
                return@computeIfAbsent createFallbackResult(goal)
            }
            
            if (verbose) {
                log.debug("Introspection result for $goal: $result")
            }
            
            result
        }
    }
    
    /**
     * Find MojoExecution for a specific goal by examining execution plans
     */
    private fun findMojoExecution(goal: String, project: MavenProject): MojoExecution? {
        return try {
            // Try to find the goal in various lifecycle phases
            val phasesToCheck = listOf(
                "validate", "compile", "test", "package", "verify", "install", "deploy",
                "clean", "site"
            )
            
            for (phase in phasesToCheck) {
                val executionPlan = lifecycleExecutor.calculateExecutionPlan(session, phase)
                
                for (mojoExecution in executionPlan.mojoExecutions) {
                    if (goal == mojoExecution.goal || 
                        goal == "${mojoExecution.plugin.artifactId}:${mojoExecution.goal}") {
                        return mojoExecution
                    }
                }
            }
            null
        } catch (e: Exception) {
            if (verbose) {
                log.debug("Could not find MojoExecution for $goal: ${e.message}")
            }
            null
        }
    }
    
    /**
     * Analyze MojoExecution to extract basic information
     */
    private fun analyzeMojoExecution(mojoExecution: MojoExecution, result: GoalIntrospectionResult) {
        // Get basic execution info
        result.pluginArtifactId = mojoExecution.plugin.artifactId
        result.pluginGroupId = mojoExecution.plugin.groupId
        result.lifecyclePhase = mojoExecution.lifecyclePhase
        result.executionId = mojoExecution.executionId
        
        // Analyze plugin type patterns
        val artifactId = mojoExecution.plugin.artifactId
        analyzePluginTypePatterns(artifactId, mojoExecution.goal, result)
    }
    
    /**
     * Analyze MojoDescriptor to understand mojo parameters and requirements
     */
    private fun analyzeMojoDescriptor(mojoDescriptor: MojoDescriptor, result: GoalIntrospectionResult) {
        // Get mojo description and requirements
        result.description = mojoDescriptor.description
        result.requiresDependencyResolution = mojoDescriptor.dependencyResolutionRequired
        result.requiresProject = mojoDescriptor.isProjectRequired
        
        // Analyze parameters to understand file/directory requirements
        mojoDescriptor.parameters?.forEach { parameter ->
            analyzeParameter(parameter, result)
        }
        
        if (verbose) {
            log.debug("MojoDescriptor analysis: ${mojoDescriptor.parameters?.size ?: 0} parameters, " +
                     "requires project: ${mojoDescriptor.isProjectRequired}, " +
                     "dependency resolution: ${mojoDescriptor.dependencyResolutionRequired}")
        }
    }
    
    /**
     * Analyze individual parameter to understand file/directory requirements
     */
    private fun analyzeParameter(parameter: Parameter, result: GoalIntrospectionResult) {
        val name = parameter.name
        val type = parameter.type
        val description = parameter.description
        
        // Check for file/directory parameters
        if (isFileParameter(name, type, description)) {
            result.addFileParameter(name, type, description)
            
            // Determine if it's input or output
            if (isOutputParameter(name, description)) {
                result.addOutputPattern(name)
            } else {
                result.addInputPattern(name)
            }
        }
        
        // Check for source-related parameters
        if (isSourceParameter(name, type, description)) {
            result.processesSources = true
        }
        
        // Check for test-related parameters
        if (isTestParameter(name, type, description)) {
            result.testRelated = true
        }
        
        // Check for resource parameters
        if (isResourceParameter(name, type, description)) {
            result.needsResources = true
        }
    }
    
    /**
     * Check if parameter represents a file or directory
     */
    private fun isFileParameter(name: String?, type: String?, description: String?): Boolean {
        if (type == null) return false
        
        // Check type
        if (type == "java.io.File" || type == "java.nio.file.Path" || 
            (type == "java.lang.String" && name != null && 
             (name.contains("Dir") || name.contains("File") || name.contains("Path")))) {
            return true
        }
        
        // Check name patterns
        name?.let {
            val lowerName = it.lowercase()
            if (lowerName.contains("directory") || lowerName.contains("file") || lowerName.contains("path") ||
                lowerName.contains("output") || lowerName.contains("input") || lowerName.contains("source") ||
                lowerName.contains("target") || lowerName.contains("destination")) {
                return true
            }
        }
        
        // Check description
        description?.let {
            val lowerDesc = it.lowercase()
            return lowerDesc.contains("directory") || lowerDesc.contains("file") || lowerDesc.contains("path")
        }
        
        return false
    }
    
    /**
     * Check if parameter is an output parameter
     */
    private fun isOutputParameter(name: String?, description: String?): Boolean {
        name?.let {
            val lowerName = it.lowercase()
            if (lowerName.contains("output") || lowerName.contains("target") || lowerName.contains("destination") ||
                lowerName.contains("generated") || lowerName.contains("build")) {
                return true
            }
        }
        
        description?.let {
            val lowerDesc = it.lowercase()
            return lowerDesc.contains("output") || lowerDesc.contains("generate") || lowerDesc.contains("create") ||
                   lowerDesc.contains("write") || lowerDesc.contains("produce")
        }
        
        return false
    }
    
    /**
     * Check if parameter is source-related
     */
    private fun isSourceParameter(name: String?, type: String?, description: String?): Boolean {
        return (name != null && name.lowercase().contains("source")) ||
               (description != null && description.lowercase().contains("source"))
    }
    
    /**
     * Check if parameter is test-related
     */
    private fun isTestParameter(name: String?, type: String?, description: String?): Boolean {
        return (name != null && name.lowercase().contains("test")) ||
               (description != null && description.lowercase().contains("test"))
    }
    
    /**
     * Check if parameter is resource-related
     */
    private fun isResourceParameter(name: String?, type: String?, description: String?): Boolean {
        return (name != null && name.lowercase().contains("resource")) ||
               (description != null && description.lowercase().contains("resource"))
    }
    
    /**
     * Analyze plugin configuration XML to understand file/directory usage
     */
    private fun analyzePluginConfiguration(mojoExecution: MojoExecution, result: GoalIntrospectionResult) {
        mojoExecution.configuration?.let { configuration ->
            analyzeConfigurationElement(configuration, result)
        }
    }
    
    /**
     * Recursively analyze configuration XML elements
     */
    private fun analyzeConfigurationElement(element: Xpp3Dom?, result: GoalIntrospectionResult) {
        if (element == null) return
        
        val name = element.name
        val value = element.value
        
        // Look for file/directory configurations
        if (value != null && (name.lowercase().contains("dir") || name.lowercase().contains("file") ||
                             name.lowercase().contains("path") || name.lowercase().contains("output"))) {
            result.addConfigurationPath(name, value)
        }
        
        // Recursively check child elements
        element.children.forEach { child ->
            analyzeConfigurationElement(child, result)
        }
    }
    
    /**
     * Analyze plugin type patterns (enhanced version of the old hardcoded logic)
     */
    private fun analyzePluginTypePatterns(artifactId: String?, goal: String?, result: GoalIntrospectionResult) {
        if (artifactId == null) return
        
        // Use pattern matching but make it more flexible
        when {
            artifactId.contains("compiler") -> {
                result.processesSources = true
                if (goal != null && goal.contains("test")) {
                    result.testRelated = true
                }
            }
            artifactId.contains("surefire") || artifactId.contains("failsafe") -> {
                result.testRelated = true
                result.processesSources = true
            }
            artifactId.contains("resources") -> {
                result.needsResources = true
                if (goal != null && goal.contains("test")) {
                    result.testRelated = true
                }
            }
            artifactId.contains("source") || artifactId.contains("javadoc") -> {
                result.processesSources = true
            }
        }
        
        // Framework-specific patterns
        if (artifactId.contains("quarkus") || artifactId.contains("spring-boot")) {
            result.processesSources = true
            result.needsResources = true
            if (goal != null && (goal == "dev" || goal == "test")) {
                result.testRelated = true
            }
        }
    }
    
    /**
     * Create fallback result when introspection fails
     */
    private fun createFallbackResult(goal: String): GoalIntrospectionResult {
        val result = GoalIntrospectionResult(goal)
        
        // Very conservative fallback
        when (goal) {
            "compile", "testCompile" -> {
                result.processesSources = true
                if (goal == "testCompile") {
                    result.testRelated = true
                }
            }
            "test" -> {
                result.testRelated = true
                result.processesSources = true
            }
        }
        
        return result
    }
    
    /**
     * Result of goal introspection containing all discovered information
     */
    class GoalIntrospectionResult(val goal: String) {
        var pluginGroupId: String? = null
        var pluginArtifactId: String? = null
        var lifecyclePhase: String? = null
        var executionId: String? = null
        var description: String? = null
        var requiresDependencyResolution: String? = null
        var requiresProject: Boolean = false
        
        // Behavior flags
        var processesSources: Boolean = false
        var testRelated: Boolean = false
        var needsResources: Boolean = false
        
        // Parameter and configuration analysis
        private val fileParameters = mutableListOf<ParameterInfo>()
        private val inputPatterns = mutableSetOf<String>()
        private val outputPatterns = mutableSetOf<String>()
        private val configurationPaths = mutableMapOf<String, String>()
        
        fun addFileParameter(name: String, type: String, description: String) {
            fileParameters.add(ParameterInfo(name, type, description))
        }
        
        fun addInputPattern(pattern: String) { inputPatterns.add(pattern) }
        fun addOutputPattern(pattern: String) { outputPatterns.add(pattern) }
        fun addConfigurationPath(name: String, path: String) { configurationPaths[name] = path }
        
        fun getFileParameters(): List<ParameterInfo> = fileParameters.toList()
        fun getInputPatterns(): Set<String> = inputPatterns.toSet()
        fun getOutputPatterns(): Set<String> = outputPatterns.toSet()
        fun getConfigurationPaths(): Map<String, String> = configurationPaths.toMap()
        
        // Java-style getter methods for compatibility
        fun processesSources(): Boolean = this.processesSources
        fun isRequiresProject(): Boolean = this.requiresProject
        
        /**
         * Convert to GoalBehavior for compatibility with existing code
         */
        fun toGoalBehavior(): GoalBehavior {
            val behavior = GoalBehavior()
            behavior.setProcessesSources(processesSources)
            behavior.setTestRelated(testRelated)
            behavior.setNeedsResources(needsResources)
            return behavior
        }
        
        override fun toString(): String {
            return "GoalIntrospectionResult(" +
                   "goal='$goal', " +
                   "plugin=$pluginGroupId:$pluginArtifactId, " +
                   "phase=$lifecyclePhase, " +
                   "sources=$processesSources, " +
                   "test=$testRelated, " +
                   "resources=$needsResources, " +
                   "fileParams=${fileParameters.size})"
        }
    }
    
    /**
     * Information about a parameter
     */
    data class ParameterInfo(
        val name: String,
        val type: String,
        val description: String
    ) {
        override fun toString(): String = "$name($type)"
    }
}