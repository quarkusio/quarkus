import model.TargetConfiguration;
import model.TargetMetadata;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
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
     * Phases are entry points that depend on individual goal targets
     */
    public Map<String, TargetConfiguration> generatePhaseTargets(MavenProject project, File workspaceRoot,
                                                                 Map<String, TargetConfiguration> allTargets,
                                                                 Map<String, List<String>> phaseDependencies) {
        Map<String, TargetConfiguration> phaseTargets = new LinkedHashMap<>();

        // Get all phases from all 3 Maven lifecycles (default, clean, site)
        Set<String> applicablePhases = executionPlanAnalysisService.getAllLifecyclePhases();

        for (String phase : applicablePhases) {
            // Phase targets depend on individual goal targets (not batch execution)
            List<String> goalsToComplete = executionPlanAnalysisService.getGoalsCompletedByPhase(project, phase);
            
            TargetConfiguration target;
            if (!goalsToComplete.isEmpty()) {
                // Phase targets are just entry points - they depend on individual goal targets
                target = new TargetConfiguration("nx:noop");
                target.setOptions(new LinkedHashMap<>());
                
                // Convert goal names to target names and set as dependencies
                List<String> goalTargetDependencies = new ArrayList<>();
                for (String goalName : goalsToComplete) {
                    // goalName is in format "pluginArtifactId:goalName" 
                    String targetName = getTargetNameFromGoal(goalName);
                    if (allTargets.containsKey(targetName)) {
                        goalTargetDependencies.add(targetName);
                    } else if (verbose) {
                        log.debug("Warning: Goal target '" + targetName + "' not found for phase '" + phase + "'");
                    }
                }
                target.setDependsOn(goalTargetDependencies);
                
            } else {
                // No goals for this phase - make it a no-op
                target = new TargetConfiguration("nx:noop");
                target.setOptions(new LinkedHashMap<>());
                target.setDependsOn(new ArrayList<>());
            }

            // Configure inputs/outputs (minimal since phase is just orchestration)
            target.setInputs(new ArrayList<>());
            target.setOutputs(new ArrayList<>());

            // Add metadata
            String description = "Maven lifecycle phase: " + phase + " (depends on " + target.getDependsOn().size() + " goals)";
            TargetMetadata metadata = new TargetMetadata("phase", description);
            metadata.setPhase(phase);
            metadata.setPlugin("org.apache.maven:maven-core");
            metadata.setTechnologies(Arrays.asList("maven"));
            target.setMetadata(metadata);

            phaseTargets.put(phase, target);
            
            if (verbose) {
                log.debug("Generated phase target '" + phase + "' depending on goals: " + target.getDependsOn());
            }
        }

        return phaseTargets;
    }
    
    /**
     * Convert a full goal name (plugin:goal) to target name (plugin:goal format)
     */
    private String getTargetNameFromGoal(String goalName) {
        // goalName is already in format "artifactId:goal" or "groupId:artifactId:goal"
        // Extract just the artifactId:goal part for target name
        String[] parts = goalName.split(":");
        if (parts.length >= 2) {
            String artifactId = parts[parts.length - 2]; // Second to last part is artifactId
            String goal = parts[parts.length - 1];       // Last part is goal
            return ExecutionPlanAnalysisService.getTargetName(artifactId, goal);
        }
        return goalName; // Fallback to original name
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


        TargetConfiguration target = new TargetConfiguration("@nx-quarkus/maven-plugin:maven-batch");
        
        if (verbose) {
            log.info("DEBUG: Creating goal target with TypeScript executor: " + pluginKey + ":" + goal);
        }

        Map<String, Object> options = new LinkedHashMap<>();
        // Use TypeScript batch executor for single goal execution with session context
        List<String> goals = new ArrayList<>();
        goals.add(pluginKey + ":" + goal);
        options.put("goals", goals);
        options.put("projectRoot", actualProjectPath);
        options.put("verbose", verbose);
        options.put("mavenPluginPath", "maven-plugin");
        options.put("failOnError", true);
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

        TargetConfiguration target = new TargetConfiguration("@nx-quarkus/maven-plugin:maven-batch");
        
        if (verbose) {
            log.info("DEBUG: Creating simple goal target with TypeScript executor: " + pluginKey + ":" + goal);
        }

        Map<String, Object> options = new LinkedHashMap<>();
        // Use TypeScript batch executor for single goal execution with session context
        List<String> goals = new ArrayList<>();
        goals.add(pluginKey + ":" + goal);
        options.put("goals", goals);
        options.put("projectRoot", actualProjectPath);
        options.put("verbose", verbose);
        options.put("mavenPluginPath", "maven-plugin");
        options.put("failOnError", true);
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

        return switch (goal) {
            case "compile" -> "Compile main sources";
            case "testCompile" -> "Compile test sources";
            case "test" -> "Run tests";
            case "integration-test" -> "Run integration tests";
            case "dev" -> "Start development mode";
            case "run" -> "Run application";
            case "build" -> "Build application";
            case "jar" -> "Create JAR";
            case "war" -> "Create WAR";
            case "site" -> "Generate site documentation";
            case "javadoc" -> "Generate Javadoc";
            case "enforce" -> "Enforce build rules";
            case "create" -> "Create build metadata";
            default -> pluginName + " " + goal;
        };
    }
}
