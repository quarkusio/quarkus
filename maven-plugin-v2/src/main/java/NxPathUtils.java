import org.apache.maven.project.MavenProject;
import java.io.File;
import java.io.IOException;

/**
 * Utility class for path operations in Nx Maven plugin
 */
public class NxPathUtils {
    
    /**
     * Calculate relative path from workspace root to target file/directory
     */
    public static String getRelativePath(File workspaceRoot, File target) {
        try {
            String workspacePath = workspaceRoot.getCanonicalPath();
            String targetPath = target.getCanonicalPath();
            
            if (targetPath.startsWith(workspacePath)) {
                String relative = targetPath.substring(workspacePath.length());
                if (relative.startsWith("/") || relative.startsWith("\\")) {
                    relative = relative.substring(1);
                }
                return relative;
            }
            
            return targetPath;
        } catch (IOException e) {
            System.err.println("Warning: Failed to calculate relative path, using absolute path: " + e.getMessage());
            return target.getAbsolutePath();
        }
    }
    
    /**
     * Get project name from Maven project (directory name)
     */
    public static String getProjectName(MavenProject project) {
        return project.getBasedir().getName();
    }
    
    /**
     * Get relative path to pom.xml for a project
     */
    public static String getRelativePomPath(MavenProject project, File workspaceRoot) {
        File pomFile = new File(project.getBasedir(), "pom.xml");
        String relativePath = getRelativePath(workspaceRoot, pomFile);
        return relativePath.isEmpty() ? "pom.xml" : relativePath;
    }
    
    /**
     * Check if two directories have a parent-child relationship
     */
    public static boolean isParentChildRelation(File dir1, File dir2) {
        try {
            String path1 = dir1.getCanonicalPath();
            String path2 = dir2.getCanonicalPath();
            return path1.startsWith(path2) || path2.startsWith(path1);
        } catch (IOException e) {
            System.err.println("Warning: Failed to check parent-child relationship: " + e.getMessage());
            return false;
        }
    }
}