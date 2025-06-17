import model.TargetConfiguration;
import model.TargetGroup;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        // Extract phases from Maven execution plan
        List<String> phases = extractPhasesFromExecutionPlan(project, session);

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
            String assignedPhase = assignTargetToPhase(targetName, target, phases);

            TargetGroup group = targetGroups.get(assignedPhase);
            if (group != null) {
                group.addTarget(targetName);
            }
        }

        return targetGroups;
    }

    /**
     * Extract phases from the Maven execution plan for the given project
     * @param project The Maven project
     * @param session The Maven session (used for fallback if needed)
     * @return List of phase names in execution order
     */
    private List<String> extractPhasesFromExecutionPlan(MavenProject project, MavenSession session) {
        List<String> phases = new ArrayList<>();

        try {
            // Use the ExecutionPlanAnalysisService to get applicable phases
            Set<String> applicablePhases = executionPlanAnalysisService.getApplicablePhases(project);

            phases.addAll(applicablePhases);

        } catch (Exception e) {
            // If we can't access the execution plan analysis, fall back to minimal essential phases
            phases.addAll(Arrays.asList("clean", "validate", "compile", "test", "package", "install", "deploy"));
        }

        return phases;
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

    private String assignTargetToPhase(String targetName, TargetConfiguration target, List<String> phases) {
        // All targets should have metadata.phase set by TargetGenerationService
        if (target.getMetadata() != null && target.getMetadata().getPhase() != null) {
            return target.getMetadata().getPhase();
        }

        // This should not happen with properly created targets
        throw new IllegalStateException("Target " + targetName + " has no phase metadata set");
    }

}
