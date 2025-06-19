import model.TargetConfiguration
import model.TargetMetadata
import org.apache.maven.execution.MavenSession
import org.apache.maven.model.Plugin
import org.apache.maven.model.PluginExecution
import org.apache.maven.model.Resource
import org.apache.maven.plugin.logging.Log
import org.apache.maven.project.MavenProject
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Service responsible for generating Nx targets from Maven project configuration.
 * Handles both phase targets and plugin goal targets.
 */
class TargetGenerationService(
    private val log: Log?,
    private val verbose: Boolean,
    private val session: MavenSession,
    private val executionPlanAnalysisService: ExecutionPlanAnalysisService
) {

    private val dynamicGoalAnalysis = DynamicGoalAnalysisService(
        session, 
        executionPlanAnalysisService,
        executionPlanAnalysisService.getLifecycleExecutor(),
        executionPlanAnalysisService.getDefaultLifecycles(),
        log!!, 
        verbose
    )

    /**
     * Generate all targets for a Maven project
     * @param project The Maven project to generate targets for
     * @param workspaceRoot The workspace root directory
     * @param goalDependencies Pre-calculated goal dependencies
     * @param phaseDependencies Pre-calculated phase dependencies
     * @return Map of target name to target configuration
     * @throws IllegalArgumentException if project is null
     */
    fun generateTargets(
        project: MavenProject?,
        workspaceRoot: File?,
        goalDependencies: Map<String, List<Any>>,
        phaseDependencies: Map<String, List<Any>>
    ): Map<String, TargetConfiguration> {
        requireNotNull(project) { "Project cannot be null" }

        val targets = linkedMapOf<String, TargetConfiguration>()

        try {
            // Generate plugin goal targets first
            targets.putAll(generatePluginGoalTargets(project, workspaceRoot, goalDependencies))

            // Generate Maven lifecycle phase targets
            targets.putAll(generatePhaseTargets(project, workspaceRoot, targets, phaseDependencies))

        } catch (e: Exception) {
            log?.warn("Error generating targets for project ${project.artifactId}: ${e.message}", e)
            // Return empty targets rather than failing completely
        }

        return targets
    }

    /**
     * Generate targets for Maven lifecycle phases
     * Phases are entry points that depend on individual goal targets
     */
    fun generatePhaseTargets(
        project: MavenProject,
        workspaceRoot: File?,
        allTargets: Map<String, TargetConfiguration>,
        phaseDependencies: Map<String, List<Any>>
    ): Map<String, TargetConfiguration> {
        val phaseTargets = linkedMapOf<String, TargetConfiguration>()

        // Get all phases from all 3 Maven lifecycles (default, clean, site)
        val applicablePhases = executionPlanAnalysisService.getAllLifecyclePhases()

        applicablePhases.forEach { phase ->
            // Phase targets depend on individual goal targets (not batch execution)
            val goalsToComplete = executionPlanAnalysisService.getGoalsCompletedByPhase(project, phase)
            
            val target = if (goalsToComplete.isNotEmpty()) {
                // Phase targets are just entry points - they depend on individual goal targets
                TargetConfiguration("nx:noop").apply {
                    options = linkedMapOf()
                    
                    // Convert goal names to target names and set as dependencies
                    val goalTargetDependencies = mutableListOf<Any>()
                    goalsToComplete.forEach { goalName ->
                        // goalName is in format "pluginArtifactId:goalName" 
                        val targetName = getTargetNameFromGoal(goalName)
                        if (allTargets.containsKey(targetName)) {
                            goalTargetDependencies.add(targetName)
                        } else if (verbose) {
                            log?.debug("Warning: Goal target '$targetName' not found for phase '$phase'")
                        }
                    }
                    dependsOn = goalTargetDependencies
                }
            } else {
                // No goals for this phase - make it a no-op
                TargetConfiguration("nx:noop").apply {
                    options = linkedMapOf()
                    dependsOn = mutableListOf()
                }
            }

            // Configure inputs/outputs (minimal since phase is just orchestration)
            target.inputs = mutableListOf()
            target.outputs = mutableListOf()

            // Add metadata
            val description = "Maven lifecycle phase: $phase (depends on ${target.dependsOn.size} goals)"
            val metadata = TargetMetadata("phase", description).apply {
                this.phase = phase
                plugin = "org.apache.maven:maven-core"
                technologies = mutableListOf("maven")
            }
            target.metadata = metadata

            phaseTargets[phase] = target
            
            if (verbose) {
                log?.debug("Generated phase target '$phase' depending on goals: ${target.dependsOn}")
            }
        }

        return phaseTargets
    }
    
    /**
     * Convert a full goal name (plugin:goal) to target name (plugin:goal format)
     */
    private fun getTargetNameFromGoal(goalName: String): String {
        // goalName is already in format "artifactId:goal" or "groupId:artifactId:goal"
        // Extract just the artifactId:goal part for target name
        val parts = goalName.split(":")
        return if (parts.size >= 2) {
            val artifactId = parts[parts.size - 2] // Second to last part is artifactId
            val goal = parts[parts.size - 1]       // Last part is goal
            ExecutionPlanAnalysisService.getTargetName(artifactId, goal)
        } else {
            goalName // Fallback to original name
        }
    }

    /**
     * Generate targets for plugin goals
     */
    fun generatePluginGoalTargets(
        project: MavenProject,
        workspaceRoot: File?,
        goalDependencies: Map<String, List<Any>>
    ): Map<String, TargetConfiguration> {
        val goalTargets = linkedMapOf<String, TargetConfiguration>()

        if (verbose) {
            log?.debug("Generating plugin goal targets for project: ${project.artifactId}")
        }

        val projectRootToken = "{projectRoot}"
        val actualProjectPath = if (workspaceRoot != null) {
            val relativePath = NxPathUtils.getRelativePath(workspaceRoot, project.basedir)
            relativePath.ifEmpty { "." }
        } else {
            "{projectRoot}"
        }

        project.buildPlugins?.forEach { plugin ->
            // Process actual executions from effective POM
            plugin.executions?.forEach { execution ->
                execution.goals?.forEach { goal ->
                    val targetName = ExecutionPlanAnalysisService.getTargetName(plugin.artifactId, goal)

                    if (!goalTargets.containsKey(targetName)) {
                        val target = createGoalTarget(
                            plugin, goal, execution, projectRootToken, actualProjectPath,
                            goalDependencies[targetName] ?: mutableListOf(), project
                        )
                        goalTargets[targetName] = target
                    }
                }
            }

            // Add common goals for well-known plugins
            addCommonGoalsForPlugin(plugin, goalTargets, projectRootToken, actualProjectPath, goalDependencies, project)
        }

        return goalTargets
    }

    /**
     * Create a target configuration for a specific Maven goal
     */
    fun createGoalTarget(
        plugin: Plugin,
        goal: String,
        execution: PluginExecution,
        projectRootToken: String,
        actualProjectPath: String,
        dependencies: List<Any>,
        project: MavenProject
    ): TargetConfiguration {
        val pluginKey = "${plugin.groupId}:${plugin.artifactId}"

        val target = TargetConfiguration("@nx-quarkus/maven-plugin:maven-batch")
        
        if (verbose) {
            log?.info("DEBUG: Creating goal target with TypeScript executor: $pluginKey:$goal")
        }

        val options = linkedMapOf<String, Any>(
            "goals" to listOf("$pluginKey:$goal"),
            "projectRoot" to actualProjectPath,
            "verbose" to verbose,
            "mavenPluginPath" to "maven-plugin",
            "failOnError" to true
        )
        target.options = options

        // Smart inputs/outputs based on goal using Maven APIs
        val inputs = getSmartInputsForGoal(goal, project, projectRootToken)
        target.inputs = inputs

        val outputs = executionPlanAnalysisService.getGoalOutputs(goal, projectRootToken, project).toMutableList()
        target.outputs = outputs

        // Use pre-calculated dependencies
        target.dependsOn = dependencies.toMutableList()

        // Metadata
        val metadata = TargetMetadata("goal", generateGoalDescription(plugin.artifactId, goal)).apply {
            this.plugin = pluginKey
            this.goal = goal
            executionId = execution.id
            phase = when {
                !execution.phase.isNullOrEmpty() && !execution.phase.startsWith("\${") -> execution.phase
                else -> executionPlanAnalysisService.findPhaseForGoal(project, goal)
            }
            technologies = mutableListOf("maven")
        }
        target.metadata = metadata

        return target
    }

    private fun addCommonGoalsForPlugin(
        plugin: Plugin,
        goalTargets: MutableMap<String, TargetConfiguration>,
        projectRootToken: String,
        actualProjectPath: String,
        goalDependencies: Map<String, List<Any>>,
        project: MavenProject
    ) {
        val artifactId = plugin.artifactId
        val commonGoals = ExecutionPlanAnalysisService.getCommonGoalsForPlugin(artifactId)

        commonGoals.forEach { goal ->
            val targetName = ExecutionPlanAnalysisService.getTargetName(artifactId, goal)
            if (!goalTargets.containsKey(targetName)) {
                val target = createSimpleGoalTarget(
                    plugin, goal, projectRootToken, actualProjectPath,
                    goalDependencies[targetName] ?: mutableListOf(), project
                )
                goalTargets[targetName] = target
            }
        }
    }

    private fun createSimpleGoalTarget(
        plugin: Plugin,
        goal: String,
        projectRootToken: String,
        actualProjectPath: String,
        dependencies: List<Any>,
        project: MavenProject
    ): TargetConfiguration {
        val pluginKey = "${plugin.groupId}:${plugin.artifactId}"

        val target = TargetConfiguration("@nx-quarkus/maven-plugin:maven-batch")
        
        if (verbose) {
            log?.info("DEBUG: Creating simple goal target with TypeScript executor: $pluginKey:$goal")
        }

        val options = linkedMapOf<String, Any>(
            "goals" to listOf("$pluginKey:$goal"),
            "projectRoot" to actualProjectPath,
            "verbose" to verbose,
            "mavenPluginPath" to "maven-plugin",
            "failOnError" to true
        )
        target.options = options

        val inputs = getSmartInputsForGoal(goal, project, projectRootToken)
        target.inputs = inputs

        val outputs = executionPlanAnalysisService.getGoalOutputs(goal, projectRootToken, project).toMutableList()
        target.outputs = outputs

        // Use pre-calculated dependencies
        target.dependsOn = dependencies.toMutableList()

        val metadata = TargetMetadata("goal", generateGoalDescription(plugin.artifactId, goal)).apply {
            this.plugin = pluginKey
            this.goal = goal
            phase = executionPlanAnalysisService.findPhaseForGoal(project, goal)
            technologies = mutableListOf("maven")
        }
        target.metadata = metadata

        return target
    }

    // Helper methods

    /**
     * Get smart inputs for a Maven goal using dynamic Maven API analysis
     * instead of hardcoded patterns. This uses actual Maven project configuration,
     * plugin analysis, and lifecycle phase information.
     */
    private fun getSmartInputsForGoal(goal: String, project: MavenProject, projectRootToken: String): MutableList<String> {
        val inputs = mutableListOf<String>()
        
        // Always include POM
        inputs.add("$projectRootToken/pom.xml")
        
        // Get dynamic analysis of goal behavior
        val behavior = dynamicGoalAnalysis.analyzeGoal(goal, project)
        
        if (behavior.processesSources()) {
            // Use actual source directories from Maven configuration
            val sourcePaths = behavior.getSourcePaths().ifEmpty { 
                project.compileSourceRoots.toMutableList()
            }
            
            sourcePaths.forEach { sourcePath: String ->
                val relativePath = getRelativePathFromProject(sourcePath, project)
                if (!relativePath.isNullOrEmpty()) {
                    inputs.add("$projectRootToken/$relativePath/**/*")
                }
            }
            
            // Add test sources for test-related goals
            if (behavior.isTestRelated()) {
                project.testCompileSourceRoots.forEach { testSourceRoot ->
                    val relativePath = getRelativePathFromProject(testSourceRoot, project)
                    if (!relativePath.isNullOrEmpty()) {
                        inputs.add("$projectRootToken/$relativePath/**/*")
                    }
                }
            }
        }
        
        if (behavior.needsResources()) {
            // Use actual resource directories from Maven configuration
            val resourcePaths = behavior.getResourcePaths().ifEmpty {
                (project.build?.resources?.mapNotNull { it.directory } ?: emptyList()).toMutableList()
            }
            
            resourcePaths.forEach { resourcePath: String ->
                val relativePath = getRelativePathFromProject(resourcePath, project)
                if (!relativePath.isNullOrEmpty()) {
                    inputs.add("$projectRootToken/$relativePath/**/*")
                }
            }
            
            // Test resources for test-related goals
            if (behavior.isTestRelated()) {
                project.build?.testResources?.forEach { resource ->
                    resource.directory?.let { directory ->
                        val relativePath = getRelativePathFromProject(directory, project)
                        if (!relativePath.isNullOrEmpty()) {
                            inputs.add("$projectRootToken/$relativePath/**/*")
                        }
                    }
                }
            }
        }
        
        return inputs
    }

    // Removed hardcoded goal classification methods:
    // - isSourceProcessingGoal() - replaced by dynamicGoalAnalysis.analyzeGoal()
    // - isTestGoal() - replaced by GoalBehavior.isTestRelated() 
    // - needsResources() - replaced by GoalBehavior.needsResources()
    // 
    // These methods used hardcoded string patterns and have been replaced with
    // dynamic Maven API-based analysis that uses actual plugin configuration,
    // lifecycle phases, and MojoExecution information.
    
    /**
     * Convert an absolute or relative path from Maven configuration to a relative path
     * from the project base directory.
     */
    private fun getRelativePathFromProject(pathString: String?, project: MavenProject): String? {
        if (pathString.isNullOrBlank()) {
            return null
        }
        
        return try {
            val projectBase = project.basedir.toPath().toAbsolutePath()
            val targetPath = Paths.get(pathString)
            
            if (targetPath.isAbsolute) {
                // Convert absolute path to relative
                if (targetPath.startsWith(projectBase)) {
                    projectBase.relativize(targetPath).toString().replace('\\', '/')
                } else {
                    // Path is outside project - skip it
                    if (verbose && log != null) {
                        log.debug("Skipping path outside project: $pathString")
                    }
                    null
                }
            } else {
                // Already relative - normalize separators
                targetPath.toString().replace('\\', '/')
            }
        } catch (e: Exception) {
            log?.warn("Error processing path: $pathString, error: ${e.message}")
            null
        }
    }

    private fun generateGoalDescription(artifactId: String, goal: String): String {
        val pluginName = ExecutionPlanAnalysisService.normalizePluginName(artifactId)

        return when (goal) {
            "compile" -> "Compile main sources"
            "testCompile" -> "Compile test sources"
            "test" -> "Run tests"
            "integration-test" -> "Run integration tests"
            "dev" -> "Start development mode"
            "run" -> "Run application"
            "build" -> "Build application"
            "jar" -> "Create JAR"
            "war" -> "Create WAR"
            "site" -> "Generate site documentation"
            "javadoc" -> "Generate Javadoc"
            "enforce" -> "Enforce build rules"
            "create" -> "Create build metadata"
            else -> "$pluginName $goal"
        }
    }
}