import model.TargetConfiguration
import model.TargetGroup
import org.apache.maven.execution.MavenSession
import org.apache.maven.project.MavenProject

/**
 * Service responsible for organizing targets into logical groups based on Maven phases.
 */
class TargetGroupService(
    private val executionPlanAnalysisService: ExecutionPlanAnalysisService
) {

    /**
     * Generate target groups for a project based on its targets
     * @param project The Maven project to generate groups for
     * @param projectTargets The targets to organize into groups
     * @param session The Maven session to extract execution plan phases
     * @return Map of phase name to target group
     * @throws IllegalArgumentException if project or projectTargets is null
     */
    fun generateTargetGroups(
        project: MavenProject?,
        projectTargets: Map<String, TargetConfiguration>?,
        session: MavenSession?
    ): Map<String, TargetGroup> {
        requireNotNull(project) { "Project cannot be null" }
        requireNotNull(projectTargets) { "Project targets cannot be null" }
        requireNotNull(session) { "Maven session cannot be null" }
        
        val targetGroups = linkedMapOf<String, TargetGroup>()

        // Get all phases from all 3 Maven lifecycles (default, clean, site)
        val phases = getAllLifecyclePhases()
        val phaseDescriptions = getPhaseDescriptions()

        // Create target groups for each phase
        phases.forEachIndexed { index, phase ->
            val group = TargetGroup(phase, phaseDescriptions[phase], index)
            targetGroups[phase] = group
        }

        // Assign targets to groups
        projectTargets.forEach { (targetName, target) ->
            val assignedPhase = assignTargetToPhase(targetName, target, phases, project)

            // Skip targets that don't have phase metadata (noop)
            assignedPhase?.let { phase ->
                targetGroups[phase]?.addTarget(targetName)
            }
        }

        return targetGroups
    }

    /**
     * Get all phases from all 3 Maven lifecycles (default, clean, site)
     * @return List of all lifecycle phase names
     */
    private fun getAllLifecyclePhases(): List<String> {
        return executionPlanAnalysisService.getAllLifecyclePhases().toList()
    }

    private fun getPhaseDescriptions(): Map<String, String> {
        return linkedMapOf(
            "clean" to "Clean up artifacts created by build",
            "validate" to "Validate project structure and configuration",
            "compile" to "Compile source code",
            "test" to "Run unit tests",
            "package" to "Package compiled code",
            "verify" to "Verify package integrity",
            "install" to "Install package to local repository",
            "deploy" to "Deploy package to remote repository",
            "site" to "Generate project documentation"
        )
    }

    private fun assignTargetToPhase(
        targetName: String,
        target: TargetConfiguration,
        phases: List<String>,
        project: MavenProject
    ): String? {
        // All targets should have metadata.phase set by TargetGenerationService
        target.metadata?.phase?.let { return it }

        // Use ExecutionPlanAnalysisService to determine the phase for this target
        return try {
            // If target name is a phase name, assign it to that phase
            if (phases.contains(targetName)) {
                targetName
            } else {
                // For goal targets, extract the goal and find its phase
                val goal = ExecutionPlanAnalysisService.extractGoalFromTargetName(targetName)
                goal?.let {
                    val foundPhase = executionPlanAnalysisService.findPhaseForGoal(project, it)
                    if (foundPhase != null && phases.contains(foundPhase)) {
                        foundPhase
                    } else {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            // Log but don't fail
            null
        }
    }
}