import model.TargetConfiguration;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
     * Calculate dependencies for a goal target
     */
    public List<String> calculateGoalDependencies(MavenProject project, String executionPhase,
                                                  String targetName, List<MavenProject> reactorProjects) {
        List<String> dependsOn = new ArrayList<>();


        String effectivePhase = executionPhase;
        if (effectivePhase == null || effectivePhase.isEmpty() || effectivePhase.startsWith("${")) {
            effectivePhase = executionPlanAnalysisService.findPhaseForGoal(project, ExecutionPlanAnalysisService.extractGoalFromTargetName(targetName));
        }


        if (effectivePhase != null && !effectivePhase.isEmpty()) {
            // Add dependency on preceding phase (but don't fail if this doesn't work)
            try {
                String precedingPhase = getPrecedingPhase(effectivePhase, project);
                if (precedingPhase != null && !precedingPhase.isEmpty()) {
                    dependsOn.add(precedingPhase);
                }
            } catch (Exception e) {
                // Continue even if preceding phase detection fails
            }

            // ALWAYS add cross-module dependencies using Nx ^ syntax
            // Goals depend on their phase across all dependent projects
            dependsOn.add("^" + effectivePhase);
        }


        return dependsOn;
    }

    /**
     * Calculate dependencies for a phase target
     */
    public List<String> calculatePhaseDependencies(String phase, Map<String, TargetConfiguration> allTargets,
                                                   MavenProject project, List<MavenProject> reactorProjects) {
        List<String> dependsOn = new ArrayList<>();

        // Add dependency on preceding phase
        List<String> phaseDependencies = getPhaseDependencies(phase, project);
        dependsOn.addAll(phaseDependencies);

        // Add dependencies on all goals that belong to this phase
        List<String> goalsForPhase = getGoalsForPhase(phase, allTargets);
        dependsOn.addAll(goalsForPhase);

        // Add cross-module dependencies using Nx ^ syntax
        dependsOn.add("^" + phase);

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
     * Get lifecycle phases using ExecutionPlanAnalysisService
     */
    private List<String> getLifecyclePhases(String upToPhase, MavenProject project) {
        List<String> allPhases = executionPlanAnalysisService.getLifecyclePhases();
        
        // Return phases up to the target phase
        int targetIndex = allPhases.indexOf(upToPhase);
        if (targetIndex >= 0) {
            return allPhases.subList(0, targetIndex + 1);
        }
        
        // If upToPhase is not found, return all phases
        return allPhases;
    }

    /**
     * Get the preceding phase in the Maven lifecycle
     */
    public String getPrecedingPhase(String phase, MavenProject project) {
        if (phase == null || phase.isEmpty()) {
            return null;
        }

        // Get lifecycle phases using Maven's lifecycle definitions
        List<String> lifecyclePhases = getLifecyclePhases(phase, project);

        if (verbose) {
            log.info("Found lifecycle phases up to " + phase + ": " + lifecyclePhases);
        }

        // Find the preceding phase
        int currentPhaseIndex = lifecyclePhases.indexOf(phase);
        if (currentPhaseIndex > 0) {
            String precedingPhase = lifecyclePhases.get(currentPhaseIndex - 1);
            if (verbose) {
                log.info("Found preceding phase: " + precedingPhase);
            }
            return precedingPhase;
        }

        if (verbose) {
            log.info("No preceding phase found for: " + phase + " (index: " + currentPhaseIndex + ")");
        }
        return null;
    }






}
