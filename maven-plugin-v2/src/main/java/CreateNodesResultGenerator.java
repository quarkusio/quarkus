import model.*;
import org.apache.maven.project.MavenProject;
import org.apache.maven.artifact.Artifact;
import java.io.File;
import java.util.*;
import java.util.LinkedHashMap;

/**
 * Generates CreateNodesV2 compatible results for Nx integration
 */
public class CreateNodesResultGenerator {
    
    /**
     * Generate CreateNodesV2 results from Maven project list
     * Returns: List<CreateNodesV2Entry>
     */
    public static List<CreateNodesV2Entry> generateCreateNodesV2Results(List<MavenProject> projects, File workspaceRoot, Map<MavenProject, Map<String, TargetConfiguration>> projectTargets, Map<MavenProject, Map<String, TargetGroup>> projectTargetGroups) {
        List<CreateNodesV2Entry> results = new ArrayList<>();
        
        for (MavenProject project : projects) {
            File pomFile = new File(project.getBasedir(), "pom.xml");
            
            // Create tuple: [pomFilePath, CreateNodesResult]
            String pomPath = NxPathUtils.getRelativePath(workspaceRoot, pomFile);
            if (pomPath.isEmpty()) {
                pomPath = "pom.xml";
            }
            
            CreateNodesResult createNodesResult = generateCreateNodesResult(project, workspaceRoot, projectTargets.get(project), projectTargetGroups.get(project));
            CreateNodesV2Entry entry = new CreateNodesV2Entry(pomPath, createNodesResult);
            
            results.add(entry);
        }
        
        return results;
    }
    
    /**
     * Generate a single CreateNodesResult for a project
     */
    private static CreateNodesResult generateCreateNodesResult(MavenProject project, File workspaceRoot, Map<String, TargetConfiguration> targets, Map<String, TargetGroup> targetGroups) {
        String projectName = NxPathUtils.getProjectName(project);
        String projectRoot = NxPathUtils.getRelativePath(workspaceRoot, project.getBasedir());
        
        // Handle root project case
        if (projectRoot.isEmpty()) {
            projectRoot = ".";
        }
        
        // Create ProjectConfiguration
        ProjectConfiguration projectConfig = new ProjectConfiguration();
        projectConfig.setName(project.getGroupId() + ":" + project.getArtifactId());
        projectConfig.setRoot(projectRoot);
        
        
        // Add targets (update paths to use workspace-relative paths)
        if (targets != null && !targets.isEmpty()) {
            Map<String, TargetConfiguration> updatedTargets = updateTargetPaths(targets, workspaceRoot, project.getBasedir());
            projectConfig.setTargets(updatedTargets);
        }
        
        // Add metadata
        ProjectMetadata metadata = new ProjectMetadata(
            project.getGroupId(),
            project.getArtifactId(), 
            project.getVersion(),
            project.getPackaging()
        );
        
        // Add target groups to metadata (convert from Map<String, TargetGroup> to Map<String, List<String>>)
        if (targetGroups != null && !targetGroups.isEmpty()) {
            Map<String, List<String>> convertedTargetGroups = new LinkedHashMap<>();
            for (Map.Entry<String, TargetGroup> entry : targetGroups.entrySet()) {
                String phaseName = entry.getKey();
                TargetGroup targetGroup = entry.getValue();
                convertedTargetGroups.put(phaseName, new ArrayList<>(targetGroup.getTargets()));
            }
            metadata.setTargetGroups(convertedTargetGroups);
        }
        
        projectConfig.setMetadata(metadata);
        
        // Set project type
        projectConfig.setProjectType(determineProjectType(project));
        
        // Create CreateNodesResult
        CreateNodesResult result = new CreateNodesResult();
        result.addProject(projectRoot, projectConfig);
        
        return result;
    }
    
    /**
     * Update target paths to use workspace-relative paths instead of {projectRoot} tokens
     */
    private static Map<String, TargetConfiguration> updateTargetPaths(Map<String, TargetConfiguration> targets, File workspaceRoot, File projectDir) {
        Map<String, TargetConfiguration> updatedTargets = new LinkedHashMap<>();
        String projectRootToken = NxPathUtils.getRelativePath(workspaceRoot, projectDir);
        if (projectRootToken.isEmpty()) {
            projectRootToken = ".";
        }
        
        for (Map.Entry<String, TargetConfiguration> targetEntry : targets.entrySet()) {
            String targetName = targetEntry.getKey();
            TargetConfiguration target = targetEntry.getValue();
            TargetConfiguration updatedTarget = new TargetConfiguration();
            
            // Copy basic properties
            updatedTarget.setExecutor(target.getExecutor());
            updatedTarget.setMetadata(target.getMetadata());
            updatedTarget.setDependsOn(new ArrayList<>(target.getDependsOn()));
            
            // Update cwd in options
            Map<String, Object> options = new LinkedHashMap<>(target.getOptions());
            if (options.containsKey("cwd") && "{projectRoot}".equals(options.get("cwd"))) {
                options.put("cwd", projectRootToken);
            }
            updatedTarget.setOptions(options);
            
            // Update inputs
            List<String> updatedInputs = new ArrayList<>();
            for (String input : target.getInputs()) {
                if (input.startsWith("{projectRoot}/")) {
                    updatedInputs.add(input.replace("{projectRoot}", projectRootToken));
                } else {
                    updatedInputs.add(input);
                }
            }
            updatedTarget.setInputs(updatedInputs);
            
            // Update outputs
            List<String> updatedOutputs = new ArrayList<>();
            for (String output : target.getOutputs()) {
                if (output.startsWith("{projectRoot}/")) {
                    updatedOutputs.add(output.replace("{projectRoot}", projectRootToken));
                } else {
                    updatedOutputs.add(output);
                }
            }
            updatedTarget.setOutputs(updatedOutputs);
            
            updatedTargets.put(targetName, updatedTarget);
        }
        
        return updatedTargets;
    }
    
    /**
     * Determine project type based on Maven project configuration
     * @param project The Maven project to analyze
     * @return "application" or "library" (never null)
     */
    private static String determineProjectType(MavenProject project) {
        String packaging = project.getPackaging();
        
        // Handle null packaging by defaulting to library
        if (packaging == null) {
            return "library";
        }
        
        // POM packaging usually indicates an aggregator/parent project
        if ("pom".equals(packaging)) {
            return "library";
        }
        
        // WAR and EAR packaging indicates web applications
        if ("war".equals(packaging) || "ear".equals(packaging)) {
            return "application";
        }
        
        // Default to library for all other packaging types (jar, etc.)
        return "library";
    }
}