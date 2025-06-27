import model.TargetConfiguration
import model.TargetDependency
import org.apache.maven.plugin.logging.Log
import org.apache.maven.project.MavenProject
import org.apache.maven.model.Plugin
import org.apache.maven.model.PluginExecution

/**
 * Service responsible for calculating target dependencies in Maven projects.
 * Handles phase dependencies, cross-module dependencies, and goal dependencies.
 */
class TargetDependencyService(
    private val log: Log?,
    private val verbose: Boolean,
    private val executionPlanAnalysisService: ExecutionPlanAnalysisService
) {

    /**
     * Calculate dependencies for a goal target.
     * Goals now depend on other goals, not on phases.
     */
    fun calculateGoalDependencies(
        project: MavenProject,
        executionPhase: String?,
        targetName: String,
        actualDependencies: List<MavenProject>
    ): List<Any> {
        val dependencies = mutableSetOf<Any>() // Use Set to prevent duplicates

        val effectivePhase = when {
            executionPhase.isNullOrEmpty() || executionPhase.startsWith("\${") -> {
                executionPlanAnalysisService.findPhaseForGoal(
                    project,
                    ExecutionPlanAnalysisService.extractGoalFromTargetName(targetName)
                )
            }
            else -> executionPhase
        }

        if (effectivePhase?.isNotEmpty() == true) {
            // Add goal-to-goal dependencies based on Maven lifecycle ordering (same project)
            val precedingGoals = getPrecedingGoalsInLifecycle(project, effectivePhase)
            dependencies.addAll(precedingGoals) // Simple string dependencies for same project

            // Add cross-module goal dependencies - goals depend on specific goals, not phases
            if (actualDependencies.isNotEmpty()) {
                val crossModuleGoals = getCrossModuleGoalsForPhase(project, effectivePhase, actualDependencies)
                dependencies.addAll(crossModuleGoals) // Direct goal-to-goal dependencies
            }
        }

        return dependencies.toList() // Convert back to list for return
    }

    /**
     * Calculate dependencies for a phase target
     */
    fun calculatePhaseDependencies(
        phase: String,
        allTargets: Map<String, TargetConfiguration>,
        project: MavenProject,
        reactorProjects: List<MavenProject>
    ): List<Any> {
        val dependsOn = mutableSetOf<Any>() // Use Set to prevent duplicates

        // Add dependencies on all goals that belong to this phase
        val goalsForPhase = getGoalsForPhase(phase, allTargets)
        dependsOn.addAll(goalsForPhase) // Simple string dependencies for goals in same project

        return dependsOn.toList() // Convert back to list for return
    }

    /**
     * Get phase dependencies (preceding phases)
     */
    fun getPhaseDependencies(phase: String, project: MavenProject): List<String> {
        val deps = mutableListOf<String>()
        val precedingPhase = getPrecedingPhase(phase, project)
        precedingPhase?.let { deps.add(it) }
        return deps
    }

    /**
     * Get all goals that belong to a specific phase
     */
    fun getGoalsForPhase(phase: String, allTargets: Map<String, TargetConfiguration>): List<String> {
        return allTargets.entries
            .filter { (_, target) ->
                target.metadata?.type == "goal" && target.metadata?.phase == phase
            }
            .map { (targetName, _) -> targetName }
    }

    /**
     * Get all goals from preceding phases in the Maven lifecycle.
     * This ensures goals depend on other goals, not phases.
     * Optimized to use pre-computed execution plan analysis.
     */
    fun getPrecedingGoalsInLifecycle(project: MavenProject, currentPhase: String?): List<String> {
        val precedingGoals = mutableListOf<String>()

        if (currentPhase.isNullOrEmpty()) {
            return precedingGoals
        }

        // Use pre-computed lifecycle information for better performance
        val lifecycle = executionPlanAnalysisService.getLifecycleForPhase(currentPhase)
        if (lifecycle?.phases == null) {
            return precedingGoals
        }

        val lifecyclePhases = lifecycle.phases
        val currentPhaseIndex = lifecyclePhases.indexOf(currentPhase)

        if (currentPhaseIndex <= 0) {
            return precedingGoals // No preceding phases
        }

        // Get all goals from all preceding phases using project's plugin configuration
        for (i in 0 until currentPhaseIndex) {
            val precedingPhase = lifecyclePhases[i]
            val phaseGoals = getCommonGoalsForPhase(precedingPhase, project)
            precedingGoals.addAll(phaseGoals)
        }

        if (verbose && precedingGoals.isNotEmpty()) {
            log?.debug("Found ${precedingGoals.size} preceding goals for phase '$currentPhase': $precedingGoals")
        }

        return precedingGoals
    }
    
    /**
     * Get goals for a Maven lifecycle phase by looking up from the project's plugin configuration.
     * This finds all goals from the project that are bound to the specified phase.
     */
    private fun getCommonGoalsForPhase(phase: String, project: MavenProject?): List<String> {
        val goals = mutableListOf<String>()
        
        if (project?.buildPlugins == null) {
            return goals
        }
        
        // Go through all plugins and their executions to find goals bound to this phase
        project.buildPlugins.forEach { plugin ->
            plugin.executions?.forEach { execution ->
                val executionPhase = execution.phase
                
                // Check if this execution is bound to the phase we're looking for
                if (phase == executionPhase && execution.goals != null) {
                    execution.goals.forEach { goal ->
                        val pluginGoal = ExecutionPlanAnalysisService.getTargetName(plugin.artifactId, goal, execution.id)
                        goals.add(pluginGoal)
                    }
                }
            }
        }
        
        return goals
    }

    /**
     * Get goals from the same phase across actual project dependencies.
     * Returns dependencies in format "nxProjectName:goalName" where nxProjectName is "groupId:artifactId".
     */
    fun getCrossModuleGoalsForPhase(
        currentProject: MavenProject,
        phase: String?,
        actualDependencies: List<MavenProject>?
    ): List<String> {
        val crossModuleGoals = mutableListOf<String>()

        if (phase.isNullOrEmpty() || actualDependencies == null) {
            return crossModuleGoals
        }

        // For each actual dependency project
        actualDependencies
            .filter { it != currentProject }
            .forEach { otherProject ->
                // Get all goals for this phase in the other project
                val phaseGoals = executionPlanAnalysisService.getGoalsForPhase(otherProject, phase)

                phaseGoals
                    .filter { it.contains(":") }
                    .forEach { goal ->
                        // Add with Nx project name to create project:goal dependency
                        val nxProjectName = MavenUtils.formatProjectKey(otherProject)
                        crossModuleGoals.add("$nxProjectName:$goal")
                    }
            }

        if (verbose && crossModuleGoals.isNotEmpty()) {
            log?.debug("Found ${crossModuleGoals.size} cross-module project:goal dependencies from actual dependencies for phase '$phase': $crossModuleGoals")
        }

        return crossModuleGoals
    }

    /**
     * Get the preceding phase in the Maven lifecycle (supports all lifecycles: default, clean, site)
     */
    fun getPrecedingPhase(phase: String?, project: MavenProject): String? {
        if (phase.isNullOrEmpty()) {
            return null
        }

        // Find which lifecycle contains this phase and get its phases
        val lifecycle = executionPlanAnalysisService.getLifecycleForPhase(phase)
        val precedingPhase = lifecycle?.phases?.let { phases ->
            findPrecedingPhaseInLifecycle(phase, phases)
        }

        if (precedingPhase != null && verbose) {
            log?.info("Found preceding phase: $precedingPhase")
        }

        return precedingPhase
    }

    /**
     * Helper method to find preceding phase within a specific lifecycle
     */
    private fun findPrecedingPhaseInLifecycle(phase: String, lifecyclePhases: List<String>): String? {
        val currentPhaseIndex = lifecyclePhases.indexOf(phase)
        return if (currentPhaseIndex > 0) {
            lifecyclePhases[currentPhaseIndex - 1]
        } else {
            null
        }
    }
}