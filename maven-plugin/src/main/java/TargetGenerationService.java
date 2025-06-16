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

    public TargetGenerationService(Log log, boolean verbose, MavenSession session) {
        this.log = log;
        this.verbose = verbose;
        this.session = session;
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
                                String targetName = MavenUtils.getTargetName(plugin.getArtifactId(), goal);
                                
                                if (!goalTargets.containsKey(targetName)) {
                                    TargetConfiguration target = createGoalTarget(plugin, goal, execution, projectRootToken, actualProjectPath, goalDependencies.getOrDefault(targetName, new ArrayList<>()));
                                    goalTargets.put(targetName, target);
                                }
                            });
                        }
                    });
                }
                
                // Add common goals for well-known plugins
                addCommonGoalsForPlugin(plugin, goalTargets, projectRootToken, actualProjectPath, goalDependencies);
            });
        }
        
        return goalTargets;
    }

    /**
     * Create a target configuration for a specific Maven goal
     */
    public TargetConfiguration createGoalTarget(Plugin plugin, String goal, PluginExecution execution, 
                                               String projectRootToken, String actualProjectPath, 
                                               List<String> dependencies) {
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
        
        List<String> outputs = getGoalOutputs(goal, projectRootToken);
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
            metadata.setPhase(inferPhaseFromGoal(goal));
        }
        metadata.setTechnologies(Arrays.asList("maven"));
        target.setMetadata(metadata);
        
        return target;
    }

    private void addCommonGoalsForPlugin(Plugin plugin, Map<String, TargetConfiguration> goalTargets, 
                                        String projectRootToken, String actualProjectPath, 
                                        Map<String, List<String>> goalDependencies) {
        String artifactId = plugin.getArtifactId();
        List<String> commonGoals = new ArrayList<>();
        
        if (artifactId.contains("compiler")) {
            commonGoals.addAll(Arrays.asList("compile", "testCompile"));
        } else if (artifactId.contains("surefire")) {
            commonGoals.add("test");
        } else if (artifactId.contains("quarkus")) {
            commonGoals.addAll(Arrays.asList("dev", "build"));
        } else if (artifactId.contains("spring-boot")) {
            commonGoals.addAll(Arrays.asList("run", "repackage"));
        }
        
        for (String goal : commonGoals) {
            String targetName = MavenUtils.getTargetName(artifactId, goal);
            if (!goalTargets.containsKey(targetName)) {
                TargetConfiguration target = createSimpleGoalTarget(plugin, goal, projectRootToken, actualProjectPath, goalDependencies.getOrDefault(targetName, new ArrayList<>()));
                goalTargets.put(targetName, target);
            }
        }
    }

    private TargetConfiguration createSimpleGoalTarget(Plugin plugin, String goal, 
                                                      String projectRootToken, String actualProjectPath,
                                                      List<String> dependencies) {
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
        
        List<String> outputs = getGoalOutputs(goal, projectRootToken);
        target.setOutputs(outputs);
        
        // Use pre-calculated dependencies
        target.setDependsOn(dependencies);
        
        TargetMetadata metadata = new TargetMetadata("goal", generateGoalDescription(plugin.getArtifactId(), goal));
        metadata.setPlugin(pluginKey);
        metadata.setGoal(goal);
        metadata.setPhase(inferPhaseFromGoal(goal));
        metadata.setTechnologies(Arrays.asList("maven"));
        target.setMetadata(metadata);
        
        return target;
    }

    // Helper methods

    private boolean isSourceProcessingGoal(String goal) {
        return goal.contains("compile") || goal.contains("test") || goal.contains("build") || 
               goal.equals("dev") || goal.equals("run");
    }

    private List<String> getGoalOutputs(String goal, String projectRootToken) {
        List<String> outputs = new ArrayList<>();
        if (goal.contains("compile") || goal.contains("build") || goal.contains("jar") || goal.contains("war")) {
            outputs.add(projectRootToken + "/target/**/*");
        } else if (goal.contains("test")) {
            outputs.add(projectRootToken + "/target/surefire-reports/**/*");
            outputs.add(projectRootToken + "/target/failsafe-reports/**/*");
        } else if (goal.contains("site") || goal.contains("javadoc")) {
            outputs.add(projectRootToken + "/target/site/**/*");
        }
        return outputs;
    }


    private String generateGoalDescription(String artifactId, String goal) {
        String pluginName = artifactId.replace("-maven-plugin", "").replace("-plugin", "");
        
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


    private String inferPhaseFromGoal(String goal) {
        if (goal == null || goal.isEmpty()) {
            return null;
        }
        
        if (goal.equals("compile") || goal.equals("testCompile")) {
            return "compile";
        } else if (goal.equals("test") || goal.contains("test")) {
            return "test";
        } else if (goal.equals("jar") || goal.equals("war") || goal.equals("build") || goal.equals("repackage")) {
            return "package";
        } else if (goal.equals("dev") || goal.equals("run")) {
            return "compile";
        } else if (goal.contains("site") || goal.contains("javadoc")) {
            return "site";
        } else {
            return "compile";
        }
    }

    /**
     * Dynamically discover all lifecycle phases that have bound goals for this project
     */
    private Set<String> getApplicablePhases(MavenProject project) {
        Set<String> applicablePhases = new LinkedHashSet<>();
        
        try {
            LifecycleExecutor lifecycleExecutor = session.getContainer().lookup(LifecycleExecutor.class);
            
            // Calculate execution plan for "deploy" phase (includes all phases up to deploy)
            MavenExecutionPlan executionPlan = lifecycleExecutor.calculateExecutionPlan(
                session, "deploy"
            );
            
            // Extract unique phases from all mojo executions
            for (MojoExecution mojoExecution : executionPlan.getMojoExecutions()) {
                String phase = mojoExecution.getLifecyclePhase();
                if (phase != null && !phase.isEmpty()) {
                    applicablePhases.add(phase);
                }
            }
            
            if (verbose) {
                log.debug("Discovered phases for " + project.getArtifactId() + ": " + applicablePhases);
            }
            
        } catch (Exception e) {
            if (log != null) {
                log.warn("Could not determine applicable phases for " + project.getArtifactId() + ": " + e.getMessage());
            }
            // Fallback to minimal phases for safety
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
            LifecycleExecutor lifecycleExecutor = session.getContainer().lookup(LifecycleExecutor.class);
            
            // Calculate execution plan for just this phase
            MavenExecutionPlan executionPlan = lifecycleExecutor.calculateExecutionPlan(
                session, phase
            );
            
            // Check if any mojo executions are bound to this phase
            return executionPlan.getMojoExecutions()
                .stream()
                .anyMatch(mojoExecution -> phase.equals(mojoExecution.getLifecyclePhase()));
                
        } catch (Exception e) {
            if (verbose && log != null) {
                log.warn("Could not check phase bindings for " + phase + ": " + e.getMessage());
            }
            return true; // Assume it has goals if we can't determine
        }
    }
}