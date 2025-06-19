import org.apache.maven.project.MavenProject

/**
 * Utility methods for common Maven project operations.
 */
object MavenUtils {
    
    /**
     * Format Maven project as "groupId:artifactId" key
     */
    @JvmStatic
    fun formatProjectKey(project: MavenProject): String {
        return "${project.groupId}:${project.artifactId}"
    }
}