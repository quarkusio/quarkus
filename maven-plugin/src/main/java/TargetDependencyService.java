import model.TargetConfiguration;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service responsible for calculating target dependencies in Maven projects.
 * Handles phase dependencies, cross-module dependencies, and goal dependencies.
 */
public class TargetDependencyService {

    private final Log log;
    private final boolean verbose;
    private final ExecutionPlanAnalysisService executionPlanAnalysisService;

    public TargetDependencyService(Log log, boolean verbose, ExecutionPlanAnalysisService executionPlanAnalysisService) {
        this.log = log;
        this.verbose = verbose;
        this.executionPlanAnalysisService = executionPlanAnalysisService;
    }

    /**
     * Calculate dependencies for a goal target.
     * Goals now depend on other goals, not on phases.
     */
    public List<String> calculateGoalDependencies(MavenProject project, String executionPhase,
                                                  String targetName, List<MavenProject> actualDependencies) {
        Set<String> dependsOnSet = new LinkedHashSet<>();

        String effectivePhase = executionPhase;
        if (effectivePhase == null || effectivePhase.isEmpty() || effectivePhase.startsWith("${")) {
            effectivePhase = executionPlanAnalysisService.findPhaseForGoal(project, ExecutionPlanAnalysisService.extractGoalFromTargetName(targetName));
        }

        if (effectivePhase != null && !effectivePhase.isEmpty()) {
            // Add goal-to-goal dependencies based on Maven lifecycle ordering
            List<String> precedingGoals = getPrecedingGoalsInLifecycle(project, effectivePhase);
            dependsOnSet.addAll(precedingGoals);

            // Add cross-module dependencies for goals in the same phase across all dependent projects
            List<String> crossModuleGoals = getCrossModuleGoalsForPhase(project, effectivePhase, actualDependencies);
            dependsOnSet.addAll(crossModuleGoals);
        }

        return new ArrayList<>(dependsOnSet);
    }

    /**
     * Calculate dependencies for a phase target
     */
    public List<String> calculatePhaseDependencies(String phase, Map<String, TargetConfiguration> allTargets,
                                                   MavenProject project, List<MavenProject> reactorProjects) {
        List<String> dependsOn = new ArrayList<>();

        // Add dependencies on all goals that belong to this phase
        List<String> goalsForPhase = getGoalsForPhase(phase, allTargets);
        dependsOn.addAll(goalsForPhase);

        return dependsOn;
    }

    /**
     * Get phase dependencies (preceding phases)
     */
    public List<String> getPhaseDependencies(String phase, MavenProject project) {
        List<String> deps = new ArrayList<>();
        String precedingPhase = getPrecedingPhase(phase, project);
        if (precedingPhase != null) {
            deps.add(precedingPhase);
        }
        return deps;
    }

    /**
     * Get all goals that belong to a specific phase
     */
    public List<String> getGoalsForPhase(String phase, Map<String, TargetConfiguration> allTargets) {
        List<String> goalsForPhase = new ArrayList<>();

        for (Map.Entry<String, TargetConfiguration> entry : allTargets.entrySet()) {
            String targetName = entry.getKey();
            TargetConfiguration target = entry.getValue();

            if (target.getMetadata() != null &&
                "goal".equals(target.getMetadata().getType()) &&
                phase.equals(target.getMetadata().getPhase())) {
                goalsForPhase.add(targetName);
            }
        }

        return goalsForPhase;
    }




    /**
     * Get all goals from preceding phases in the Maven lifecycle.
     * This ensures goals depend on other goals, not phases.
     */
    public List<String> getPrecedingGoalsInLifecycle(MavenProject project, String currentPhase) {
        List<String> precedingGoals = new ArrayList<>();

        if (currentPhase == null || currentPhase.isEmpty()) {
            return precedingGoals;
        }

        // Get the lifecycle containing this phase
        org.apache.maven.lifecycle.Lifecycle lifecycle = executionPlanAnalysisService.getLifecycleForPhase(currentPhase);
        if (lifecycle == null || lifecycle.getPhases() == null) {
            return precedingGoals;
        }

        List<String> lifecyclePhases = lifecycle.getPhases();
        int currentPhaseIndex = lifecyclePhases.indexOf(currentPhase);

        if (currentPhaseIndex <= 0) {
            return precedingGoals; // No preceding phases
        }

        // Get all goals from all preceding phases using project's plugin configuration
        for (int i = 0; i < currentPhaseIndex; i++) {
            String precedingPhase = lifecyclePhases.get(i);
            List<String> phaseGoals = getCommonGoalsForPhase(precedingPhase, project);
            precedingGoals.addAll(phaseGoals);
        }

        if (verbose && !precedingGoals.isEmpty()) {
            log.debug("Found " + precedingGoals.size() + " preceding goals for phase '" + currentPhase + "': " + precedingGoals);
        }

        return precedingGoals;
    }
    
    /**
     * Get goals for a Maven lifecycle phase by looking up from the project's plugin configuration.
     * This finds all goals from the project that are bound to the specified phase.
     */
    private List<String> getCommonGoalsForPhase(String phase, MavenProject project) {
        List<String> goals = new ArrayList<>();
        
        if (project == null || project.getBuildPlugins() == null) {
            return goals;
        }
        
        // Go through all plugins and their executions to find goals bound to this phase
        for (org.apache.maven.model.Plugin plugin : project.getBuildPlugins()) {
            if (plugin.getExecutions() != null) {
                for (org.apache.maven.model.PluginExecution execution : plugin.getExecutions()) {
                    String executionPhase = execution.getPhase();
                    
                    // Check if this execution is bound to the phase we're looking for
                    if (phase.equals(executionPhase) && execution.getGoals() != null) {
                        for (String goal : execution.getGoals()) {
                            String pluginGoal = ExecutionPlanAnalysisService.getTargetName(plugin.getArtifactId(), goal);
                            goals.add(pluginGoal);
                        }
                    }
                }
            }
        }
        
        return goals;
    }

    /**
     * Get goals from the same phase across actual project dependencies.
     * Returns dependencies in format "nxProjectName:goalName" where nxProjectName is "groupId:artifactId".
     */
    public List<String> getCrossModuleGoalsForPhase(MavenProject currentProject, String phase, List<MavenProject> actualDependencies) {
        List<String> crossModuleGoals = new ArrayList<>();

        if (phase == null || phase.isEmpty() || actualDependencies == null) {
            return crossModuleGoals;
        }

        // For each actual dependency project
        for (MavenProject otherProject : actualDependencies) {
            if (otherProject != null && !otherProject.equals(currentProject)) {
                // Get all goals for this phase in the other project
                List<String> phaseGoals = executionPlanAnalysisService.getGoalsForPhase(otherProject, phase);

                for (String goal : phaseGoals) {
                    if (goal.contains(":")) {
                        // Add with Nx project name to create project:goal dependency
                        String nxProjectName = MavenUtils.formatProjectKey(otherProject);
                        crossModuleGoals.add(nxProjectName + ":" + goal);
                    }
                }
            }
        }

        if (verbose && !crossModuleGoals.isEmpty()) {
            log.debug("Found " + crossModuleGoals.size() + " cross-module project:goal dependencies from actual dependencies for phase '" + phase + "': " + crossModuleGoals);
        }

        return crossModuleGoals;
    }

    /**
     * Get the preceding phase in the Maven lifecycle (supports all lifecycles: default, clean, site)
     */
    public String getPrecedingPhase(String phase, MavenProject project) {
        if (phase == null || phase.isEmpty()) {
            return null;
        }

        // Find which lifecycle contains this phase and get its phases
        org.apache.maven.lifecycle.Lifecycle lifecycle = executionPlanAnalysisService.getLifecycleForPhase(phase);
        String precedingPhase = null;

        if (lifecycle != null && lifecycle.getPhases() != null) {
            precedingPhase = findPrecedingPhaseInLifecycle(phase, lifecycle.getPhases());
        }

        if (precedingPhase != null && verbose) {
            log.info("Found preceding phase: " + precedingPhase);
        }

        return precedingPhase;
    }

    /**
     * Helper method to find preceding phase within a specific lifecycle
     */
    private String findPrecedingPhaseInLifecycle(String phase, List<String> lifecyclePhases) {
        int currentPhaseIndex = lifecyclePhases.indexOf(phase);
        if (currentPhaseIndex > 0) {
            return lifecyclePhases.get(currentPhaseIndex - 1);
        }
        return null;
    }






}
