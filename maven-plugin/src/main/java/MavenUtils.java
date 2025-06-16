import org.apache.maven.project.MavenProject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility methods for common Maven project operations.
 */
public class MavenUtils {
    
    /**
     * Extract goal name from target name (e.g., "compiler:compile" -> "compile")
     */
    public static String extractGoalFromTargetName(String targetName) {
        if (targetName == null || !targetName.contains(":")) {
            return targetName;
        }
        return targetName.substring(targetName.lastIndexOf(":") + 1);
    }
    
    /**
     * Generate target name from artifact ID and goal
     */
    public static String getTargetName(String artifactId, String goal) {
        String pluginName = artifactId.replace("-maven-plugin", "").replace("-plugin", "");
        return pluginName + ":" + goal;
    }
    
    /**
     * Format Maven project as "groupId:artifactId" key
     */
    public static String formatProjectKey(MavenProject project) {
        return project.getGroupId() + ":" + project.getArtifactId();
    }
    
    /**
     * Infer the Maven phase from a goal name using project build configuration.
     * First tries to analyze actual Maven plugin configurations, then falls back to hardcoded mappings.
     */
    public static String inferPhaseFromGoal(String goal, MavenProject project) {
        if (goal == null || goal.isEmpty()) {
            return null;
        }

        // Try to infer from project build plugins first (better approach)
        if (project != null && project.getBuild() != null) {
            for (org.apache.maven.model.Plugin plugin : project.getBuild().getPlugins()) {
                for (org.apache.maven.model.PluginExecution execution : plugin.getExecutions()) {
                    if (execution.getGoals().contains(goal) ||
                        execution.getGoals().stream().anyMatch(g -> goal.endsWith(":" + g))) {
                        if (execution.getPhase() != null) {
                            return execution.getPhase();
                        }
                    }
                }
            }
        }

        // No fallback - rely only on project plugin configuration
        return null;
    }
    
}