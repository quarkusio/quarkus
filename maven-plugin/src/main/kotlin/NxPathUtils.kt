import org.apache.maven.project.MavenProject
import java.io.File
import java.io.IOException

/**
 * Utility class for path operations in Nx Maven plugin
 */
object NxPathUtils {
    
    /**
     * Calculate relative path from workspace root to target file/directory
     */
    @JvmStatic
    fun getRelativePath(workspaceRoot: File, target: File): String {
        return try {
            val workspacePath = workspaceRoot.canonicalPath
            val targetPath = target.canonicalPath
            
            if (targetPath.startsWith(workspacePath)) {
                var relative = targetPath.substring(workspacePath.length)
                if (relative.startsWith("/") || relative.startsWith("\\")) {
                    relative = relative.substring(1)
                }
                relative
            } else {
                targetPath
            }
        } catch (e: IOException) {
            System.err.println("Warning: Failed to calculate relative path, using absolute path: ${e.message}")
            target.absolutePath
        }
    }
    
    /**
     * Get project name from Maven project (directory name)
     */
    @JvmStatic
    fun getProjectName(project: MavenProject): String {
        return project.basedir.name
    }
    
    /**
     * Get relative path to pom.xml for a project
     */
    @JvmStatic
    fun getRelativePomPath(project: MavenProject, workspaceRoot: File): String {
        val pomFile = File(project.basedir, "pom.xml")
        val relativePath = getRelativePath(workspaceRoot, pomFile)
        return if (relativePath.isEmpty()) "pom.xml" else relativePath
    }
    
    /**
     * Check if two directories have a parent-child relationship
     */
    @JvmStatic
    fun isParentChildRelation(dir1: File, dir2: File): Boolean {
        return try {
            val path1 = dir1.canonicalPath
            val path2 = dir2.canonicalPath
            path1.startsWith(path2) || path2.startsWith(path1)
        } catch (e: IOException) {
            System.err.println("Warning: Failed to check parent-child relationship: ${e.message}")
            false
        }
    }
}