import model.TargetConfiguration;
import model.TargetGroup;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for organizing targets into logical groups based on Maven phases.
 */
public class TargetGroupService {

    private final ExecutionPlanAnalysisService executionPlanAnalysisService;

    public TargetGroupService(ExecutionPlanAnalysisService executionPlanAnalysisService) {
        this.executionPlanAnalysisService = executionPlanAnalysisService;
    }

    /**
     * Generate target groups for a project based on its targets
     * @param project The Maven project to generate groups for
     * @param projectTargets The targets to organize into groups
     * @param session The Maven session to extract execution plan phases
     * @return Map of phase name to target group
     * @throws IllegalArgumentException if project or projectTargets is null
     */
    public Map<String, TargetGroup> generateTargetGroups(MavenProject project, Map<String, TargetConfiguration> projectTargets, MavenSession session) {
        if (project == null) {
            throw new IllegalArgumentException("Project cannot be null");
        }
        if (projectTargets == null) {
            throw new IllegalArgumentException("Project targets cannot be null");
        }
        if (session == null) {
            throw new IllegalArgumentException("Maven session cannot be null");
        }
        Map<String, TargetGroup> targetGroups = new LinkedHashMap<>();

        // Get all phases from all 3 Maven lifecycles (default, clean, site)
        List<String> phases = getAllLifecyclePhases();

        Map<String, String> phaseDescriptions = getPhaseDescriptions();

        // Create target groups for each phase
        for (int i = 0; i < phases.size(); i++) {
            String phase = phases.get(i);
            TargetGroup group = new TargetGroup(phase, phaseDescriptions.get(phase), i);
            targetGroups.put(phase, group);
        }

        // Assign targets to groups
        for (String targetName : projectTargets.keySet()) {
            TargetConfiguration target = projectTargets.get(targetName);
            String assignedPhase = assignTargetToPhase(targetName, target, phases, project);

            // Skip targets that don't have phase metadata (noop)
            if (assignedPhase != null) {
                TargetGroup group = targetGroups.get(assignedPhase);
                if (group != null) {
                    group.addTarget(targetName);
                }
            }
        }

        return targetGroups;
    }

    /**
     * Get all phases from all 3 Maven lifecycles (default, clean, site)
     * @return List of all lifecycle phase names
     */
    private List<String> getAllLifecyclePhases() {
        return new ArrayList<>(executionPlanAnalysisService.getAllLifecyclePhases());
    }

    private Map<String, String> getPhaseDescriptions() {
        Map<String, String> descriptions = new LinkedHashMap<>();
        descriptions.put("clean", "Clean up artifacts created by build");
        descriptions.put("validate", "Validate project structure and configuration");
        descriptions.put("compile", "Compile source code");
        descriptions.put("test", "Run unit tests");
        descriptions.put("package", "Package compiled code");
        descriptions.put("verify", "Verify package integrity");
        descriptions.put("install", "Install package to local repository");
        descriptions.put("deploy", "Deploy package to remote repository");
        descriptions.put("site", "Generate project documentation");
        return descriptions;
    }

    private String assignTargetToPhase(String targetName, TargetConfiguration target, List<String> phases, MavenProject project) {
        // All targets should have metadata.phase set by TargetGenerationService
        if (target.getMetadata() != null && target.getMetadata().getPhase() != null) {
            return target.getMetadata().getPhase();
        }

        // Use ExecutionPlanAnalysisService to determine the phase for this target
        try {
            // If target name is a phase name, assign it to that phase
            if (phases.contains(targetName)) {
                return targetName;
            }
            
            // For goal targets, extract the goal and find its phase
            String goal = ExecutionPlanAnalysisService.extractGoalFromTargetName(targetName);
            if (goal != null) {
                String foundPhase = executionPlanAnalysisService.findPhaseForGoal(project, goal);
                if (foundPhase != null && phases.contains(foundPhase)) {
                    return foundPhase;
                }
            }
        } catch (Exception e) {
            // Log but don't fail
        }

        // Return null if no phase can be determined - caller should handle this gracefully (noop)
        return null;
    }

}
