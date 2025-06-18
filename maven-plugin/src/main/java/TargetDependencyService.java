import model.TargetConfiguration;
import model.TargetDependency;
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
    public List<Object> calculateGoalDependencies(MavenProject project, String executionPhase,
                                                  String targetName, List<MavenProject> actualDependencies) {
        List<Object> dependencies = new ArrayList<>();

        String effectivePhase = executionPhase;
        if (effectivePhase == null || effectivePhase.isEmpty() || effectivePhase.startsWith("${")) {
            effectivePhase = executionPlanAnalysisService.findPhaseForGoal(project, ExecutionPlanAnalysisService.extractGoalFromTargetName(targetName));
        }

        if (effectivePhase != null && !effectivePhase.isEmpty()) {
            // Add goal-to-goal dependencies based on Maven lifecycle ordering (same project)
            List<String> precedingGoals = getPrecedingGoalsInLifecycle(project, effectivePhase);
            for (String goal : precedingGoals) {
                dependencies.add(goal); // Simple string dependency for same project
            }

            // Add cross-module dependencies using object form for better precision
            if (!actualDependencies.isEmpty()) {
                List<String> dependentProjects = new ArrayList<>();
                for (MavenProject depProject : actualDependencies) {
                    dependentProjects.add(MavenUtils.formatProjectKey(depProject));
                }
                
                // Create object dependency for cross-module goals in the same phase
                TargetDependency crossModuleDep = new TargetDependency(effectivePhase, dependentProjects);
                dependencies.add(crossModuleDep);
            }
        }

        return dependencies;
    }

    /**
     * Calculate dependencies for a phase target
     */
    public List<Object> calculatePhaseDependencies(String phase, Map<String, TargetConfiguration> allTargets,
                                                   MavenProject project, List<MavenProject> reactorProjects) {
        List<Object> dependsOn = new ArrayList<>();

        // Add dependencies on all goals that belong to this phase
        List<String> goalsForPhase = getGoalsForPhase(phase, allTargets);
        for (String goal : goalsForPhase) {
            dependsOn.add(goal); // Simple string dependency for goals in same project
        }

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
