import model.TargetConfiguration;
import model.TargetMetadata;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.util.LinkedHashSet;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service responsible for generating Nx targets from Maven project configuration.
 * Handles both phase targets and plugin goal targets.
 */
public class TargetGenerationService {
    
    private final Log log;
    private final boolean verbose;
    private final MavenSession session;
    private final ExecutionPlanAnalysisService executionPlanAnalysisService;

    public TargetGenerationService(Log log, boolean verbose, MavenSession session, ExecutionPlanAnalysisService executionPlanAnalysisService) {
        this.log = log;
        this.verbose = verbose;
        this.session = session;
        this.executionPlanAnalysisService = executionPlanAnalysisService;
    }

    /**
     * Generate all targets for a Maven project
     * @param project The Maven project to generate targets for
     * @param workspaceRoot The workspace root directory
     * @param goalDependencies Pre-calculated goal dependencies
     * @param phaseDependencies Pre-calculated phase dependencies
     * @return Map of target name to target configuration
     * @throws IllegalArgumentException if project is null
     */
    public Map<String, TargetConfiguration> generateTargets(MavenProject project, File workspaceRoot, 
                                                           Map<String, List<String>> goalDependencies,
                                                           Map<String, List<String>> phaseDependencies) {
        if (project == null) {
            throw new IllegalArgumentException("Project cannot be null");
        }
        
        Map<String, TargetConfiguration> targets = new LinkedHashMap<>();
        
        try {
            // Generate plugin goal targets first
            targets.putAll(generatePluginGoalTargets(project, workspaceRoot, goalDependencies));
            
            // Generate Maven lifecycle phase targets
            targets.putAll(generatePhaseTargets(project, workspaceRoot, targets, phaseDependencies));
            
        } catch (Exception e) {
            if (log != null) {
                log.warn("Error generating targets for project " + project.getArtifactId() + ": " + e.getMessage(), e);
            }
            // Return empty targets rather than failing completely
        }
        
        return targets;
    }

    /**
     * Generate targets for Maven lifecycle phases
     */
    public Map<String, TargetConfiguration> generatePhaseTargets(MavenProject project, File workspaceRoot, 
                                                                 Map<String, TargetConfiguration> allTargets, 
                                                                 Map<String, List<String>> phaseDependencies) {
        Map<String, TargetConfiguration> phaseTargets = new LinkedHashMap<>();
        
        // Dynamically discover applicable phases for this project
        Set<String> applicablePhases = getApplicablePhases(project);
        
        for (String phase : applicablePhases) {
            TargetConfiguration target = new TargetConfiguration("nx:noop");
            
            // Configure as no-op
            target.setOptions(new LinkedHashMap<>());
            target.setInputs(new ArrayList<>());
            target.setOutputs(new ArrayList<>());
            
            // Phase dependencies - use pre-calculated dependencies
            List<String> dependsOn = phaseDependencies.getOrDefault(phase, new ArrayList<>());
            
            target.setDependsOn(dependsOn);
            
            // Add metadata
            TargetMetadata metadata = new TargetMetadata("phase", "Maven lifecycle phase: " + phase);
            metadata.setPhase(phase);
            metadata.setPlugin("org.apache.maven:maven-core");
            metadata.setTechnologies(Arrays.asList("maven"));
            target.setMetadata(metadata);
            
            phaseTargets.put(phase, target);
        }
        
        return phaseTargets;
    }

    /**
     * Generate targets for plugin goals
     */
    public Map<String, TargetConfiguration> generatePluginGoalTargets(MavenProject project, File workspaceRoot, 
                                                                      Map<String, List<String>> goalDependencies) {
        Map<String, TargetConfiguration> goalTargets = new LinkedHashMap<>();
        
        if (verbose) {
            log.debug("Generating plugin goal targets for project: " + project.getArtifactId());
        }
        
        final String projectRootToken = "{projectRoot}";
        final String actualProjectPath;
        
        if (workspaceRoot != null) {
            String relativePath = NxPathUtils.getRelativePath(workspaceRoot, project.getBasedir());
            actualProjectPath = relativePath.isEmpty() ? "." : relativePath;
        } else {
            actualProjectPath = "{projectRoot}";
        }
        
        if (project.getBuildPlugins() != null) {
            project.getBuildPlugins().forEach(plugin -> {
                // Process actual executions from effective POM
                if (plugin.getExecutions() != null) {
                    plugin.getExecutions().forEach(execution -> {
                        if (execution.getGoals() != null) {
                            execution.getGoals().forEach(goal -> {
                                String targetName = ExecutionPlanAnalysisService.getTargetName(plugin.getArtifactId(), goal);
                                
                                if (!goalTargets.containsKey(targetName)) {
                                    TargetConfiguration target = createGoalTarget(plugin, goal, execution, projectRootToken, actualProjectPath, goalDependencies.getOrDefault(targetName, new ArrayList<>()), project);
                                    goalTargets.put(targetName, target);
                                }
                            });
                        }
                    });
                }
                
                // Add common goals for well-known plugins
                addCommonGoalsForPlugin(plugin, goalTargets, projectRootToken, actualProjectPath, goalDependencies, project);
            });
        }
        
        return goalTargets;
    }

    /**
     * Create a target configuration for a specific Maven goal
     */
    public TargetConfiguration createGoalTarget(Plugin plugin, String goal, PluginExecution execution, 
                                               String projectRootToken, String actualProjectPath, 
                                               List<String> dependencies, MavenProject project) {
        String pluginKey = plugin.getGroupId() + ":" + plugin.getArtifactId();
        
        TargetConfiguration target = new TargetConfiguration("nx:run-commands");
        
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("command", "mvn " + pluginKey + ":" + goal);
        options.put("cwd", actualProjectPath);
        target.setOptions(options);
        
        // Smart inputs/outputs based on goal
        List<String> inputs = new ArrayList<>();
        inputs.add(projectRootToken + "/pom.xml");
        if (isSourceProcessingGoal(goal)) {
            inputs.add(projectRootToken + "/src/**/*");
        }
        target.setInputs(inputs);
        
        List<String> outputs = executionPlanAnalysisService.getGoalOutputs(goal, projectRootToken, project);
        target.setOutputs(outputs);
        
        // Use pre-calculated dependencies
        target.setDependsOn(dependencies);
        
        // Metadata
        TargetMetadata metadata = new TargetMetadata("goal", generateGoalDescription(plugin.getArtifactId(), goal));
        metadata.setPlugin(pluginKey);
        metadata.setGoal(goal);
        metadata.setExecutionId(execution.getId());
        String executionPhase = execution.getPhase();
        if (executionPhase != null && !executionPhase.isEmpty() && !executionPhase.startsWith("${")) {
            metadata.setPhase(executionPhase);
        } else {
            metadata.setPhase(executionPlanAnalysisService.findPhaseForGoal(project, goal));
        }
        metadata.setTechnologies(Arrays.asList("maven"));
        target.setMetadata(metadata);
        
        return target;
    }

    private void addCommonGoalsForPlugin(Plugin plugin, Map<String, TargetConfiguration> goalTargets, 
                                        String projectRootToken, String actualProjectPath, 
                                        Map<String, List<String>> goalDependencies, MavenProject project) {
        String artifactId = plugin.getArtifactId();
        List<String> commonGoals = ExecutionPlanAnalysisService.getCommonGoalsForPlugin(artifactId);
        
        for (String goal : commonGoals) {
            String targetName = ExecutionPlanAnalysisService.getTargetName(artifactId, goal);
            if (!goalTargets.containsKey(targetName)) {
                TargetConfiguration target = createSimpleGoalTarget(plugin, goal, projectRootToken, actualProjectPath, goalDependencies.getOrDefault(targetName, new ArrayList<>()), project);
                goalTargets.put(targetName, target);
            }
        }
    }

    private TargetConfiguration createSimpleGoalTarget(Plugin plugin, String goal, 
                                                      String projectRootToken, String actualProjectPath,
                                                      List<String> dependencies, MavenProject project) {
        String pluginKey = plugin.getGroupId() + ":" + plugin.getArtifactId();
        
        TargetConfiguration target = new TargetConfiguration("nx:run-commands");
        
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("command", "mvn " + pluginKey + ":" + goal);
        options.put("cwd", actualProjectPath);
        target.setOptions(options);
        
        List<String> inputs = new ArrayList<>();
        inputs.add(projectRootToken + "/pom.xml");
        if (isSourceProcessingGoal(goal)) {
            inputs.add(projectRootToken + "/src/**/*");
        }
        target.setInputs(inputs);
        
        List<String> outputs = executionPlanAnalysisService.getGoalOutputs(goal, projectRootToken, project);
        target.setOutputs(outputs);
        
        // Use pre-calculated dependencies
        target.setDependsOn(dependencies);
        
        TargetMetadata metadata = new TargetMetadata("goal", generateGoalDescription(plugin.getArtifactId(), goal));
        metadata.setPlugin(pluginKey);
        metadata.setGoal(goal);
        metadata.setPhase(executionPlanAnalysisService.findPhaseForGoal(project, goal));
        metadata.setTechnologies(Arrays.asList("maven"));
        target.setMetadata(metadata);
        
        return target;
    }

    // Helper methods

    private boolean isSourceProcessingGoal(String goal) {
        return goal.contains("compile") || goal.contains("test") || goal.contains("build") || 
               goal.equals("dev") || goal.equals("run");
    }

    
    


    private String generateGoalDescription(String artifactId, String goal) {
        String pluginName = ExecutionPlanAnalysisService.normalizePluginName(artifactId);
        
        switch (goal) {
            case "compile": return "Compile main sources";
            case "testCompile": return "Compile test sources";
            case "test": return "Run tests";
            case "integration-test": return "Run integration tests";
            case "dev": return "Start development mode";
            case "run": return "Run application";
            case "build": return "Build application";
            case "jar": return "Create JAR";
            case "war": return "Create WAR";
            case "site": return "Generate site documentation";
            case "javadoc": return "Generate Javadoc";
            case "enforce": return "Enforce build rules";
            case "create": return "Create build metadata";
            default: return pluginName + " " + goal;
        }
    }



    /**
     * Dynamically discover all lifecycle phases that have bound goals for this project
     */
    private Set<String> getApplicablePhases(MavenProject project) {
        Set<String> applicablePhases = executionPlanAnalysisService.getApplicablePhases(project);
        
        if (verbose) {
            log.debug("Discovered phases for " + project.getArtifactId() + ": " + applicablePhases);
        }
        // Always log this for debugging
        if (log != null) {
            log.info("DEBUG: Applicable phases for " + project.getArtifactId() + ": " + applicablePhases);
        }
        
        // Fallback to minimal phases if analysis returns empty
        if (applicablePhases.isEmpty()) {
            if (log != null) {
                log.warn("No phases discovered for " + project.getArtifactId() + ", using fallback phases");
            }
            applicablePhases = new LinkedHashSet<>();
            applicablePhases.add("validate");
            applicablePhases.add("install");
        }
        
        return applicablePhases;
    }

    /**
     * Check if a specific phase has any goals bound to it for this project
     */
    private boolean hasGoalsBoundToPhase(String phase, MavenProject project) {
        try {
            // Use the analysis service to check if this phase has any goals
            List<String> goalsForPhase = executionPlanAnalysisService.getGoalsForPhase(project, phase);
            return !goalsForPhase.isEmpty();
                
        } catch (Exception e) {
            if (verbose && log != null) {
                log.warn("Could not check phase bindings for " + phase + ": " + e.getMessage());
            }
            return true; // Assume it has goals if we can't determine
        }
    }
}