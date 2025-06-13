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
    
}