import model.CreateNodesResult
import model.CreateNodesV2Entry
import model.ProjectConfiguration
import model.ProjectMetadata
import model.TargetConfiguration
import model.TargetGroup
import org.apache.maven.project.MavenProject
import java.io.File

/**
 * Generates CreateNodesV2 compatible results for Nx integration
 */
object CreateNodesResultGenerator {
    
    /**
     * Generate CreateNodesV2 results from Maven project list
     * Returns: List<CreateNodesV2Entry>
     */
    fun generateCreateNodesV2Results(
        projects: List<MavenProject>,
        workspaceRoot: File,
        projectTargets: Map<MavenProject, Map<String, TargetConfiguration>>,
        projectTargetGroups: Map<MavenProject, Map<String, TargetGroup>>
    ): List<CreateNodesV2Entry> {
        val results = mutableListOf<CreateNodesV2Entry>()
        
        for (project in projects) {
            val pomFile = File(project.basedir, "pom.xml")
            
            // Create tuple: [pomFilePath, CreateNodesResult]
            var pomPath = NxPathUtils.getRelativePath(workspaceRoot, pomFile)
            if (pomPath.isEmpty()) {
                pomPath = "pom.xml"
            }
            
            val createNodesResult = generateCreateNodesResult(
                project, 
                workspaceRoot, 
                projectTargets[project], 
                projectTargetGroups[project]
            )
            val entry = CreateNodesV2Entry().apply {
                pomFilePath = pomPath
                result = createNodesResult
            }
            
            results.add(entry)
        }
        
        return results
    }
    
    private fun generateCreateNodesResult(
        project: MavenProject,
        workspaceRoot: File,
        targets: Map<String, TargetConfiguration>?,
        targetGroups: Map<String, TargetGroup>?
    ): CreateNodesResult {
        val projectName = NxPathUtils.getProjectName(project)
        val projectRoot = NxPathUtils.getRelativePath(workspaceRoot, project.basedir)
        
        val projectConfig = ProjectConfiguration(projectRoot).apply {
            name = projectName
            sourceRoot = "src/main/java"
            projectType = "library"
            this.targets.putAll(targets ?: emptyMap())
            metadata = ProjectMetadata(
                project.groupId,
                project.artifactId,
                project.version,
                project.packaging
            )
        }
        
        val result = CreateNodesResult()
        result.addProject(projectName, projectConfig)
        
        return result
    }
}