import model.RawProjectGraphDependency
import org.apache.maven.project.MavenProject
import org.apache.maven.model.Dependency
import org.apache.maven.plugin.logging.Log
import java.io.File

/**
 * Generates CreateDependencies compatible results for Nx integration
 */
object CreateDependenciesGenerator {

    /**
     * Generate CreateDependencies results from Maven project list
     * Returns: List<RawProjectGraphDependency>
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

            // 1. Static dependencies - direct Maven dependencies
            val prevStatic = staticDeps
            staticDeps += addStaticDependencies(dependencies, project, artifactToProject, sourceFile)

            // 2. Skip implicit dependencies for better performance
            // They're rarely needed and cause O(n²) complexity

            if (verbose && log != null && projects.size <= 20) {
                val newStatic = staticDeps - prevStatic
                if (newStatic > 0) {
                    log.info("Project $source: $newStatic static dependencies")
                }
            }
        }

        if (verbose && log != null) {
            log.info("Dependency analysis complete: $staticDeps static dependencies")
        }

        return dependencies
    }

    /**
     * Build mapping from artifactId to project name for workspace projects
     */
    private fun buildArtifactMapping(projects: List<MavenProject>): Map<String, String> {
        val mapping = mutableMapOf<String, String>()
        for (project in projects) {
            if (project.groupId != null && project.artifactId != null) {
                val key = MavenUtils.formatProjectKey(project)
                val projectName = MavenUtils.formatProjectKey(project)
                mapping[key] = projectName
            } else {
                System.err.println("Warning: Skipping project with null groupId or artifactId: $project")
            }
        }
        return mapping
    }

    /**
     * Add static dependencies (explicit Maven dependencies between workspace projects)
     * Returns: number of dependencies added
     */
    private fun addStaticDependencies(
        dependencies: MutableList<RawProjectGraphDependency>,
        project: MavenProject,
        artifactToProject: Map<String, String>,
        sourceFile: String
    ): Int {
        val source = MavenUtils.formatProjectKey(project)
        var count = 0

        // Check both declared dependencies and resolved artifacts
        project.dependencies?.let { deps ->
            for (dep in deps) {
                if (dep.groupId != null && dep.artifactId != null) {
                    val depKey = "${dep.groupId}:${dep.artifactId}"

                    // Check if this dependency refers to another project in workspace
                    val target = artifactToProject[depKey]
                    if (target != null && target != source) {
                        val dependency = RawProjectGraphDependency(
                            source, target, RawProjectGraphDependency.DependencyType.STATIC, sourceFile
                        )
                        dependencies.add(dependency)
                        count++
                    }
                }
            }
        }

        return count
    }
}