import model.RawProjectGraphDependency
import org.apache.maven.plugin.logging.Log
import org.apache.maven.project.MavenProject
import java.io.File

/**
 * Generates CreateDependencies compatible results for Nx integration
 */
object CreateDependenciesGenerator {
    
    /**
     * Generate CreateDependencies results from Maven project list
     */
    fun generateCreateDependencies(projects: List<MavenProject>, workspaceRoot: File): List<RawProjectGraphDependency> {
        return generateCreateDependencies(projects, workspaceRoot, null, false)
    }
    
    /**
     * Generate CreateDependencies results with logging support
     */
    fun generateCreateDependencies(
        projects: List<MavenProject>,
        workspaceRoot: File,
        log: Log?,
        verbose: Boolean
    ): List<RawProjectGraphDependency> {
        val dependencies = mutableListOf<RawProjectGraphDependency>()
        
        // Extract workspace projects
        val artifactToProject = buildArtifactMapping(projects)
        
        if (verbose && log != null) {
            log.info("Built artifact mapping for ${artifactToProject.size} workspace projects")
        }
        
        var staticDeps = 0
        
        for (i in projects.indices) {
            val project = projects[i]
            val source = MavenUtils.formatProjectKey(project)
            val sourceFile = NxPathUtils.getRelativePomPath(project, workspaceRoot)
            
            // Show progress every 100 projects to reduce log spam
            if (i % 100 == 0 || i == projects.size - 1) {
                log?.info("Dependency analysis progress: ${i + 1}/${projects.size} projects")
            }
            
            // Generate static dependencies from Maven dependencies
            project.dependencies?.let { deps ->
                for (dep in deps) {
                    if (dep.groupId != null && dep.artifactId != null) {
                        val depKey = "${dep.groupId}:${dep.artifactId}"
                        val targetProject = artifactToProject[depKey]
                        
                        if (targetProject != null && targetProject != source) {
                            dependencies.add(
                                RawProjectGraphDependency().apply {
                                    this.source = source
                                    target = targetProject
                                    type = RawProjectGraphDependency.DependencyType.STATIC
                                    this.sourceFile = sourceFile
                                }
                            )
                            staticDeps++
                        }
                    }
                }
            }
        }
        
        if (verbose && log != null) {
            log.info("Generated $staticDeps static dependencies from Maven dependencies")
        }
        
        return dependencies
    }
    
    private fun buildArtifactMapping(projects: List<MavenProject>): Map<String, String> {
        val mapping = hashMapOf<String, String>()
        
        for (project in projects) {
            if (project.groupId != null && project.artifactId != null) {
                val key = MavenUtils.formatProjectKey(project)
                mapping[key] = key
            }
        }
        
        return mapping
    }
}