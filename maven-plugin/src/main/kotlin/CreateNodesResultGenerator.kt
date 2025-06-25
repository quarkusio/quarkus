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
            val pomPath = NxPathUtils.getRelativePath(workspaceRoot, pomFile).let {
                if (it.isEmpty()) "pom.xml" else it
            }
            
            val createNodesResult = generateCreateNodesResult(
                project, 
                workspaceRoot, 
                projectTargets[project], 
                projectTargetGroups[project]
            )
            val entry = CreateNodesV2Entry(pomPath, createNodesResult)
            
            results.add(entry)
        }
        
        return results
    }
    
    /**
     * Generate a single CreateNodesResult for a project
     */
    private fun generateCreateNodesResult(
        project: MavenProject,
        workspaceRoot: File,
        targets: Map<String, TargetConfiguration>?,
        targetGroups: Map<String, TargetGroup>?
    ): CreateNodesResult {
        val projectRoot = NxPathUtils.getRelativePath(workspaceRoot, project.basedir).let {
            if (it.isEmpty()) "." else it
        }
        
        // Create ProjectConfiguration
        val projectConfig = ProjectConfiguration().apply {
            name = "${project.groupId}:${project.artifactId}"
            root = projectRoot
        }
        
        // Add targets (update paths to use workspace-relative paths)
        if (targets?.isNotEmpty() == true) {
            val updatedTargets = updateTargetPaths(targets, workspaceRoot, project.basedir)
            projectConfig.targets = updatedTargets.toMutableMap()
        }
        
        // Add metadata
        val metadata = ProjectMetadata(
            project.groupId,
            project.artifactId,
            project.version,
            project.packaging
        )
        
        // Add target groups to metadata (convert from Map<String, TargetGroup> to Map<String, List<String>>)
        if (targetGroups?.isNotEmpty() == true) {
            val convertedTargetGroups = targetGroups.mapValues { (_, targetGroup) ->
                ArrayList(targetGroup.targets)
            }
            metadata.targetGroups = convertedTargetGroups.toMutableMap()
        }
        
        projectConfig.metadata = metadata
        
        // Set project type
        projectConfig.projectType = determineProjectType(project)
        
        // Create CreateNodesResult
        val result = CreateNodesResult()
        result.addProject(projectRoot, projectConfig)
        
        return result
    }
    
    /**
     * Update target paths to use workspace-relative paths instead of {projectRoot} tokens
     */
    private fun updateTargetPaths(
        targets: Map<String, TargetConfiguration>,
        workspaceRoot: File,
        projectDir: File
    ): Map<String, TargetConfiguration> {
        val updatedTargets = mutableMapOf<String, TargetConfiguration>()
        val projectRootToken = NxPathUtils.getRelativePath(workspaceRoot, projectDir).let {
            if (it.isEmpty()) "." else it
        }
        
        for ((targetName, target) in targets) {
            val updatedTarget = TargetConfiguration().apply {
                // Copy basic properties
                executor = target.executor
                metadata = target.metadata
                dependsOn = ArrayList(target.dependsOn)
                cache = target.cache  // Copy cache property
                
                // Debug: Log cache copying
                println("DEBUG: Copying cache for target $targetName: ${target.cache} -> ${this.cache}")
                
                // Update cwd in options
                val options = target.options.toMutableMap()
                if (options["cwd"] == "{projectRoot}") {
                    options["cwd"] = projectRootToken
                }
                this.options = options
                
                // Keep inputs as-is (preserve {projectRoot} placeholders)
                inputs = ArrayList(target.inputs)
                
                // Keep outputs as-is (preserve {projectRoot} placeholders)
                outputs = ArrayList(target.outputs)
            }
            
            updatedTargets[targetName] = updatedTarget
        }
        
        return updatedTargets
    }
    
    /**
     * Determine project type based on Maven project configuration
     * @param project The Maven project to analyze
     * @return "application" or "library" (never null)
     */
    private fun determineProjectType(project: MavenProject): String {
        val packaging = project.packaging
        
        // Handle null packaging by defaulting to library
        if (packaging == null) {
            return "library"
        }
        
        return when (packaging) {
            // POM packaging usually indicates an aggregator/parent project
            "pom" -> "library"
            // WAR and EAR packaging indicates web applications
            "war", "ear" -> "application"
            // Default to library for all other packaging types (jar, etc.)
            else -> "library"
        }
    }
}