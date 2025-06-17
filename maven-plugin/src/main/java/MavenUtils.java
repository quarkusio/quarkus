import org.apache.maven.project.MavenProject;

/**
 * Utility methods for common Maven project operations.
 */
public class MavenUtils {
    
    
    /**
     * Format Maven project as "groupId:artifactId" key
     */
    public static String formatProjectKey(MavenProject project) {
        return project.getGroupId() + ":" + project.getArtifactId();
    }
    
    
}