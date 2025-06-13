import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import model.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Maven plugin that uses MavenSession API to analyze Maven projects for Nx integration
 */
@Mojo(name = "analyze", aggregator = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class NxAnalyzerMojo extends AbstractMojo {
    
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;
    
    @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
    private List<MavenProject> reactorProjects;
    
    @Component
    private LifecycleExecutor lifecycleExecutor;
    
    @Parameter(property = "nx.outputFile")
    private String outputFile;
    
    @Parameter(property = "nx.verbose", defaultValue = "false")
    private String verboseStr;
    
    // Performance optimization caches
    private final Map<String, MavenExecutionPlan> executionPlanCache = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> phaseGoalMappingCache = new ConcurrentHashMap<>();
    
    private boolean isVerbose() {
        // Check both the parameter and system property
        String systemProp = System.getProperty("nx.verbose");
        boolean fromParam = "true".equalsIgnoreCase(verboseStr);
        boolean fromSystem = "true".equalsIgnoreCase(systemProp);
        return fromParam || fromSystem;
    }
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        long startTime = System.currentTimeMillis();
        
        try {
            getLog().info("Starting Nx analysis using MavenSession with dependency resolution...");
            String systemProp = System.getProperty("nx.verbose");
            getLog().info("Debug: Verbose param='" + verboseStr + "' system='" + systemProp + "' -> " + isVerbose());
            
            // Use reactor projects from Maven session - these are the properly resolved projects
            List<MavenProject> projects = reactorProjects;
            getLog().info("Found " + projects.size() + " projects in reactor");
            
            if (isVerbose()) {
                getLog().info("Verbose mode enabled - detailed progress will be shown");
            }
            
            // Always show some basic details - the condition was checking verbose but should always show
            getLog().info("Root directory: " + session.getExecutionRootDirectory());
            if (projects.size() > 0) {
                getLog().info("First project: " + projects.get(0).getGroupId() + ":" + projects.get(0).getArtifactId());
                getLog().info("Last project: " + projects.get(projects.size()-1).getGroupId() + ":" + projects.get(projects.size()-1).getArtifactId());
            }
            
            // Convert Maven projects to Nx format
            Map<String, Object> result = convertToNxFormat(projects);
            
            // Write output
            String outputPath = determineOutputPath();
            if (isVerbose()) {
                getLog().info("Writing results to: " + outputPath);
            }
            writeResult(result, outputPath);
            
            long totalTime = System.currentTimeMillis() - startTime;
            double projectsPerSecond = projects.size() / (totalTime / 1000.0);
            
            getLog().info("Analysis complete. Results written to: " + outputPath);
            if (isVerbose()) {
                getLog().info("Performance: Processed " + projects.size() + " projects in " + 
                             (totalTime / 1000.0) + "s (" + String.format("%.1f", projectsPerSecond) + " projects/sec)");
            }
            getLog().info("SUCCESS: Maven analysis completed successfully");
            
        } catch (Exception e) {
            throw new MojoExecutionException("Analysis failed: " + e.getMessage(), e);
        }
    }
    
    private Map<String, Object> convertToNxFormat(List<MavenProject> projects) {
        long startTime = System.currentTimeMillis();
        
        if (isVerbose()) {
            getLog().info("Target generation phase: Processing " + projects.size() + " projects...");
        }
        
        // Pre-compute execution plan for common project types to enable caching
        if (isVerbose()) {
            getLog().info("Pre-computing execution plans for caching...");
        }
        precomputeExecutionPlans();
        
        // Generate targets for each project using parallel streams for better performance
        Map<MavenProject, Map<String, TargetConfiguration>> projectTargets = new ConcurrentHashMap<>();
        Map<MavenProject, Map<String, TargetGroup>> projectTargetGroups = new ConcurrentHashMap<>();
        
        // Progress tracking for parallel processing
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger totalTargets = new AtomicInteger(0);
        
        if (isVerbose()) {
            getLog().info("Starting parallel target generation for " + projects.size() + " projects...");
        }
        
        // Use parallel streams for target generation
        projects.parallelStream().forEach(project -> {
            try {
                Map<String, TargetConfiguration> targets = generateTargets(project);
                projectTargets.put(project, targets);
                totalTargets.addAndGet(targets.size());
                
                // Generate target groups using cached execution plans
                Map<String, TargetGroup> targetGroups = generateTargetGroupsOptimized(project, targets);
                projectTargetGroups.put(project, targetGroups);
                
                int processed = processedCount.incrementAndGet();
                
                // Show progress for large workspaces
                if (isVerbose() && projects.size() > 50 && processed % 50 == 0) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    double rate = processed / (elapsed / 1000.0);
                    double percentage = processed * 100.0 / projects.size();
                    
                    getLog().info("Target generation progress: " + processed + "/" + projects.size() + 
                                 " (" + String.format("%.1f", percentage) + "%) - " +
                                 String.format("%.1f", rate) + " projects/sec");
                } else if (isVerbose() && projects.size() <= 50) {
                    getLog().info("Processed project " + processed + "/" + projects.size() + 
                                 ": " + project.getGroupId() + ":" + project.getArtifactId() + 
                                 " (" + targets.size() + " targets, " + targetGroups.size() + " groups)");
                }
                
            } catch (Exception e) {
                getLog().error("Error processing project " + project.getArtifactId() + ": " + e.getMessage(), e);
                // Continue with empty targets and groups
                projectTargets.put(project, new LinkedHashMap<>());
                projectTargetGroups.put(project, new LinkedHashMap<>());
                processedCount.incrementAndGet();
            }
        });
        
        long targetGenTime = System.currentTimeMillis() - startTime;
        if (isVerbose()) {
            getLog().info("Target generation complete: " + totalTargets.get() + " targets created in " + 
                         (targetGenTime / 1000.0) + "s using parallel processing");
        }
        
        // Memory optimization: suggest GC after intensive target generation for large projects
        if (projects.size() > 100) {
            if (isVerbose()) {
                getLog().info("Suggesting garbage collection after processing " + projects.size() + " projects");
            }
            System.gc();
            Thread.yield(); // Allow GC to run
        }
        
        // Generate Nx-compatible outputs
        File workspaceRoot = new File(session.getExecutionRootDirectory());
        
        if (isVerbose()) {
            getLog().info("Generating CreateNodes results for " + projects.size() + " projects...");
        }
        long createNodesStart = System.currentTimeMillis();
        List<CreateNodesV2Entry> createNodesEntries = CreateNodesResultGenerator.generateCreateNodesV2Results(projects, workspaceRoot, projectTargets, projectTargetGroups);
        List<Object[]> createNodesResults = new ArrayList<>();
        for (CreateNodesV2Entry entry : createNodesEntries) {
            createNodesResults.add(entry.toArray());
        }
        long createNodesTime = System.currentTimeMillis() - createNodesStart;
        
        if (isVerbose()) {
            getLog().info("Generating dependency graph for " + projects.size() + " projects...");
        }
        long dependenciesStart = System.currentTimeMillis();
        List<RawProjectGraphDependency> createDependencies = CreateDependenciesGenerator.generateCreateDependencies(projects, workspaceRoot, getLog(), isVerbose());
        long dependenciesTime = System.currentTimeMillis() - dependenciesStart;
        
        if (isVerbose()) {
            getLog().info("CreateNodes generation: " + (createNodesTime / 1000.0) + "s");
            getLog().info("Dependencies generation: " + (dependenciesTime / 1000.0) + "s");
            getLog().info("Found " + createDependencies.size() + " workspace dependencies");
        }
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("createNodesResults", createNodesResults);
        result.put("createDependencies", createDependencies);
        
        return result;
    }
    
    
    
    
    private Map<String, TargetConfiguration> generateTargets(MavenProject project) {
        Map<String, TargetConfiguration> targets = new LinkedHashMap<>();
        
        File workspaceRoot = new File(session.getExecutionRootDirectory());
        
        // Generate Maven lifecycle phase targets
        targets.putAll(generatePhaseTargets(project, workspaceRoot));
        
        // Generate plugin goal targets using effective POM
        targets.putAll(generatePluginGoalTargets(project, workspaceRoot));
        
        return targets;
    }
    
    private Map<String, TargetConfiguration> generatePhaseTargets(MavenProject project) {
        return generatePhaseTargets(project, null);
    }
    
    private Map<String, TargetConfiguration> generatePhaseTargets(MavenProject project, File workspaceRoot) {
        Map<String, TargetConfiguration> phaseTargets = new LinkedHashMap<>();
        
        String[] phases = {
            "clean", "validate", "compile", "test", "package", 
            "verify", "install", "deploy", "site"
        };
        
        String projectRootToken = "{projectRoot}";
        if (workspaceRoot != null) {
            String relativePath = NxPathUtils.getRelativePath(workspaceRoot, project.getBasedir());
            projectRootToken = relativePath.isEmpty() ? "." : relativePath;
        }
        
        for (String phase : phases) {
            TargetConfiguration target = new TargetConfiguration("@nx/run-commands:run-commands");
            
            Map<String, Object> options = new LinkedHashMap<>();
            options.put("command", "mvn " + phase);
            options.put("cwd", projectRootToken);
            target.setOptions(options);
            
            // Add inputs based on phase
            List<String> inputs = new ArrayList<>();
            inputs.add(projectRootToken + "/pom.xml");
            if (needsSourceInputs(phase)) {
                inputs.add(projectRootToken + "/src/**/*");
            }
            target.setInputs(inputs);
            
            // Add outputs based on phase
            List<String> outputs = getPhaseOutputs(phase, projectRootToken);
            target.setOutputs(outputs);
            
            // Add phase dependencies
            List<String> dependsOn = getPhaseDependencies(phase);
            target.setDependsOn(dependsOn);
            
            // Add metadata
            TargetMetadata metadata = new TargetMetadata("phase", "Maven lifecycle phase: " + phase);
            metadata.setPhase(phase);
            metadata.setTechnologies(Arrays.asList("maven"));
            target.setMetadata(metadata);
            
            phaseTargets.put(phase, target);
        }
        
        return phaseTargets;
    }
    
    private Map<String, TargetConfiguration> generatePluginGoalTargets(MavenProject project) {
        return generatePluginGoalTargets(project, null);
    }
    
    private Map<String, TargetConfiguration> generatePluginGoalTargets(MavenProject project, File workspaceRoot) {
        Map<String, TargetConfiguration> goalTargets = new LinkedHashMap<>();
        
        final String projectRootToken;
        if (workspaceRoot != null) {
            String relativePath = NxPathUtils.getRelativePath(workspaceRoot, project.getBasedir());
            projectRootToken = relativePath.isEmpty() ? "." : relativePath;
        } else {
            projectRootToken = "{projectRoot}";
        }
        
        if (project.getBuildPlugins() != null) {
            project.getBuildPlugins().forEach(plugin -> {
                String pluginKey = plugin.getGroupId() + ":" + plugin.getArtifactId();
                
                // Process actual executions from effective POM
                if (plugin.getExecutions() != null) {
                    plugin.getExecutions().forEach(execution -> {
                        if (execution.getGoals() != null) {
                            execution.getGoals().forEach(goal -> {
                                String targetName = getTargetName(plugin.getArtifactId(), goal);
                                
                                if (!goalTargets.containsKey(targetName)) {
                                    TargetConfiguration target = createGoalTarget(plugin, goal, execution, projectRootToken);
                                    goalTargets.put(targetName, target);
                                }
                            });
                        }
                    });
                }
                
                // Add common goals for well-known plugins
                addCommonGoalsForPlugin(plugin, goalTargets, projectRootToken);
            });
        }
        
        return goalTargets;
    }
    
    private TargetConfiguration createGoalTarget(org.apache.maven.model.Plugin plugin, String goal, 
                                                 org.apache.maven.model.PluginExecution execution, String projectRootToken) {
        String pluginKey = plugin.getGroupId() + ":" + plugin.getArtifactId();
        
        TargetConfiguration target = new TargetConfiguration("@nx/run-commands:run-commands");
        
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("command", "mvn " + pluginKey + ":" + goal);
        options.put("cwd", projectRootToken);
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
        
        // Metadata with execution info
        TargetMetadata metadata = new TargetMetadata("goal", generateGoalDescription(plugin.getArtifactId(), goal));
        metadata.setPlugin(pluginKey);
        metadata.setGoal(goal);
        metadata.setExecutionId(execution.getId());
        metadata.setPhase(execution.getPhase());
        metadata.setTechnologies(Arrays.asList("maven"));
        target.setMetadata(metadata);
        
        return target;
    }
    
    private void addCommonGoalsForPlugin(org.apache.maven.model.Plugin plugin, Map<String, TargetConfiguration> goalTargets, String projectRootToken) {
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
            String targetName = getTargetName(artifactId, goal);
            if (!goalTargets.containsKey(targetName)) {
                TargetConfiguration target = createSimpleGoalTarget(plugin, goal, projectRootToken);
                goalTargets.put(targetName, target);
            }
        }
    }
    
    private TargetConfiguration createSimpleGoalTarget(org.apache.maven.model.Plugin plugin, String goal, String projectRootToken) {
        String pluginKey = plugin.getGroupId() + ":" + plugin.getArtifactId();
        
        TargetConfiguration target = new TargetConfiguration("@nx/run-commands:run-commands");
        
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("command", "mvn " + pluginKey + ":" + goal);
        options.put("cwd", projectRootToken);
        target.setOptions(options);
        
        List<String> inputs = new ArrayList<>();
        inputs.add(projectRootToken + "/pom.xml");
        if (isSourceProcessingGoal(goal)) {
            inputs.add(projectRootToken + "/src/**/*");
        }
        target.setInputs(inputs);
        
        List<String> outputs = getGoalOutputs(goal, projectRootToken);
        target.setOutputs(outputs);
        
        TargetMetadata metadata = new TargetMetadata("goal", generateGoalDescription(plugin.getArtifactId(), goal));
        metadata.setPlugin(pluginKey);
        metadata.setGoal(goal);
        metadata.setTechnologies(Arrays.asList("maven"));
        target.setMetadata(metadata);
        
        return target;
    }
    
    // Helper methods
    private boolean needsSourceInputs(String phase) {
        return Arrays.asList("compile", "test", "package", "verify", "install", "deploy").contains(phase);
    }
    
    private List<String> getPhaseOutputs(String phase, String projectRootToken) {
        List<String> outputs = new ArrayList<>();
        switch (phase) {
            case "compile":
                outputs.add(projectRootToken + "/target/classes/**/*");
                break;
            case "test":
                outputs.add(projectRootToken + "/target/surefire-reports/**/*");
                outputs.add(projectRootToken + "/target/test-classes/**/*");
                break;
            case "package":
            case "verify":
            case "install":
            case "deploy":
                outputs.add(projectRootToken + "/target/*.jar");
                outputs.add(projectRootToken + "/target/*.war");
                break;
            case "site":
                outputs.add(projectRootToken + "/target/site/**/*");
                break;
        }
        return outputs;
    }
    
    private List<String> getPhaseDependencies(String phase) {
        List<String> deps = new ArrayList<>();
        switch (phase) {
            case "test":
                deps.add("compile");
                break;
            case "package":
                deps.add("test");
                break;
            case "verify":
                deps.add("package");
                break;
            case "install":
                deps.add("verify");
                break;
            case "deploy":
                deps.add("install");
                break;
        }
        return deps;
    }
    
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
    
    private String getTargetName(String artifactId, String goal) {
        String pluginName = artifactId.replace("-maven-plugin", "").replace("-plugin", "");
        return pluginName + ":" + goal;
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
    
    /**
     * Pre-compute execution plans for common project configurations to enable caching
     */
    private void precomputeExecutionPlans() {
        try {
            long startTime = System.currentTimeMillis();
            
            // Pre-compute execution plans for common Maven phases
            String[] commonPhases = {"compile", "test", "package", "verify"};
            for (String phase : commonPhases) {
                String cacheKey = "common-" + phase;
                if (!executionPlanCache.containsKey(cacheKey)) {
                    try {
                        MavenExecutionPlan plan = lifecycleExecutor.calculateExecutionPlan(session, phase);
                        executionPlanCache.put(cacheKey, plan);
                        
                        // Build phase-to-goal mapping for fast lookup
                        Map<String, String> goalToPhaseMap = new LinkedHashMap<>();
                        for (MojoExecution execution : plan.getMojoExecutions()) {
                            String goal = execution.getGoal();
                            String executionPhase = execution.getLifecyclePhase();
                            if (executionPhase != null && !executionPhase.isEmpty()) {
                                goalToPhaseMap.put(goal, executionPhase);
                                
                                // Also map plugin:goal format
                                String pluginKey = execution.getPlugin().getGroupId() + ":" + execution.getPlugin().getArtifactId();
                                String targetName = getTargetName(execution.getPlugin().getArtifactId(), goal);
                                goalToPhaseMap.put(targetName, executionPhase);
                            }
                        }
                        phaseGoalMappingCache.put(cacheKey, goalToPhaseMap);
                        
                    } catch (Exception e) {
                        if (isVerbose()) {
                            getLog().warn("Could not pre-compute execution plan for phase " + phase + ": " + e.getMessage());
                        }
                    }
                }
            }
            
            long elapsed = System.currentTimeMillis() - startTime;
            if (isVerbose()) {
                getLog().info("Pre-computed " + executionPlanCache.size() + " execution plans in " + 
                             elapsed + "ms for performance optimization");
            }
            
        } catch (Exception e) {
            if (isVerbose()) {
                getLog().warn("Error during execution plan pre-computation: " + e.getMessage());
            }
        }
    }
    
    /**
     * Optimized target group generation using cached execution plans
     */
    private Map<String, TargetGroup> generateTargetGroupsOptimized(MavenProject project, Map<String, TargetConfiguration> projectTargets) {
        Map<String, TargetGroup> targetGroups = new LinkedHashMap<>();
        
        // Define Maven lifecycle phases with their order
        String[] phases = {
            "clean", "validate", "compile", "test", "package", 
            "verify", "install", "deploy", "site"
        };
        
        Map<String, String> phaseDescriptions = new LinkedHashMap<>();
        phaseDescriptions.put("clean", "Clean up artifacts created by build");
        phaseDescriptions.put("validate", "Validate project structure and configuration");
        phaseDescriptions.put("compile", "Compile source code");
        phaseDescriptions.put("test", "Run unit tests");
        phaseDescriptions.put("package", "Package compiled code");
        phaseDescriptions.put("verify", "Verify package integrity");
        phaseDescriptions.put("install", "Install package to local repository");
        phaseDescriptions.put("deploy", "Deploy package to remote repository");
        phaseDescriptions.put("site", "Generate project documentation");
        
        // Create target groups for each phase
        for (int i = 0; i < phases.length; i++) {
            String phase = phases[i];
            TargetGroup group = new TargetGroup(phase, phaseDescriptions.get(phase), i);
            targetGroups.put(phase, group);
        }
        
        // Use cached phase-to-goal mappings for fast assignment
        Map<String, String> cachedMapping = getCachedPhaseGoalMapping();
        
        for (String targetName : projectTargets.keySet()) {
            TargetConfiguration target = projectTargets.get(targetName);
            String assignedPhase = null;
            
            // First try cached mapping for O(1) lookup
            if (cachedMapping.containsKey(targetName)) {
                assignedPhase = cachedMapping.get(targetName);
            } else if (target.getMetadata() != null && target.getMetadata().getPhase() != null) {
                // Use metadata phase info
                assignedPhase = target.getMetadata().getPhase();
            } else {
                // Fallback to pattern matching
                assignedPhase = assignPhaseByPattern(targetName, phases);
            }
            
            // Add target to the appropriate group
            TargetGroup group = targetGroups.get(assignedPhase);
            if (group != null) {
                group.addTarget(targetName);
            }
        }
        
        return targetGroups;
    }
    
    /**
     * Get cached phase-to-goal mapping, combining all cached execution plans
     */
    private Map<String, String> getCachedPhaseGoalMapping() {
        Map<String, String> combinedMapping = new LinkedHashMap<>();
        
        // Combine all cached mappings
        for (Map<String, String> mapping : phaseGoalMappingCache.values()) {
            combinedMapping.putAll(mapping);
        }
        
        return combinedMapping;
    }
    
    /**
     * Assign phase based on target name patterns (fallback method)
     */
    private String assignPhaseByPattern(String targetName, String[] phases) {
        // Check if target name matches a phase directly
        for (String phase : phases) {
            if (targetName.equals(phase)) {
                return phase;
            }
        }
        
        // Pattern-based assignment
        if (targetName.contains("test")) {
            return "test";
        } else if (targetName.contains("package") || targetName.contains("jar") || targetName.contains("war")) {
            return "package";
        } else if (targetName.contains("clean")) {
            return "clean";
        } else if (targetName.contains("site") || targetName.contains("javadoc")) {
            return "site";
        } else {
            return "compile"; // Default fallback
        }
    }
    
    private Map<String, TargetGroup> generateTargetGroups(MavenProject project, Map<String, TargetConfiguration> projectTargets) {
        Map<String, TargetGroup> targetGroups = new LinkedHashMap<>();
        
        // Define Maven lifecycle phases with their order
        String[] phases = {
            "clean", "validate", "compile", "test", "package", 
            "verify", "install", "deploy", "site"
        };
        
        Map<String, String> phaseDescriptions = new LinkedHashMap<>();
        phaseDescriptions.put("clean", "Clean up artifacts created by build");
        phaseDescriptions.put("validate", "Validate project structure and configuration");
        phaseDescriptions.put("compile", "Compile source code");
        phaseDescriptions.put("test", "Run unit tests");
        phaseDescriptions.put("package", "Package compiled code");
        phaseDescriptions.put("verify", "Verify package integrity");
        phaseDescriptions.put("install", "Install package to local repository");
        phaseDescriptions.put("deploy", "Deploy package to remote repository");
        phaseDescriptions.put("site", "Generate project documentation");
        
        // Create target groups for each phase
        for (int i = 0; i < phases.length; i++) {
            String phase = phases[i];
            TargetGroup group = new TargetGroup(phase, phaseDescriptions.get(phase), i);
            targetGroups.put(phase, group);
        }
        
        // Try to get execution plan to assign plugin goals to phases
        try {
            MavenExecutionPlan executionPlan = lifecycleExecutor.calculateExecutionPlan(session, "package");
            
            if (isVerbose()) {
                getLog().info("Using execution plan to assign targets to phase groups");
            }
            
            // Map plugin executions to phases
            Map<String, String> goalToPhaseMap = new LinkedHashMap<>();
            for (MojoExecution execution : executionPlan.getMojoExecutions()) {
                String goal = execution.getGoal();
                String phase = execution.getLifecyclePhase();
                if (phase != null && !phase.isEmpty()) {
                    goalToPhaseMap.put(goal, phase);
                    
                    // Also map plugin:goal format
                    String pluginKey = execution.getPlugin().getGroupId() + ":" + execution.getPlugin().getArtifactId();
                    String targetName = getTargetName(execution.getPlugin().getArtifactId(), goal);
                    goalToPhaseMap.put(targetName, phase);
                }
            }
            
            // Assign targets to groups based on execution plan
            for (String targetName : projectTargets.keySet()) {
                TargetConfiguration target = projectTargets.get(targetName);
                String assignedPhase = null;
                
                // Check if target metadata has phase info
                if (target.getMetadata() != null && target.getMetadata().getPhase() != null) {
                    assignedPhase = target.getMetadata().getPhase();
                } else {
                    // Try to match by goal name
                    for (Map.Entry<String, String> entry : goalToPhaseMap.entrySet()) {
                        if (targetName.contains(entry.getKey()) || targetName.equals(entry.getKey())) {
                            assignedPhase = entry.getValue();
                            break;
                        }
                    }
                }
                
                // Default phase assignment for targets without execution plan info
                if (assignedPhase == null) {
                    if (Arrays.asList(phases).contains(targetName)) {
                        assignedPhase = targetName; // Phase targets go to their own group
                    } else {
                        assignedPhase = "compile"; // Default fallback
                    }
                }
                
                // Add target to the appropriate group
                TargetGroup group = targetGroups.get(assignedPhase);
                if (group != null) {
                    group.addTarget(targetName);
                }
            }
            
        } catch (Exception e) {
            if (isVerbose()) {
                getLog().warn("Could not get execution plan, using fallback target grouping: " + e.getMessage());
            }
            
            // Fallback: assign targets based on naming patterns
            for (String targetName : projectTargets.keySet()) {
                String assignedPhase = "compile"; // Default
                
                // Assign based on target name patterns
                if (Arrays.asList(phases).contains(targetName)) {
                    assignedPhase = targetName;
                } else if (targetName.contains("test")) {
                    assignedPhase = "test";
                } else if (targetName.contains("package") || targetName.contains("jar") || targetName.contains("war")) {
                    assignedPhase = "package";
                } else if (targetName.contains("clean")) {
                    assignedPhase = "clean";
                } else if (targetName.contains("site") || targetName.contains("javadoc")) {
                    assignedPhase = "site";
                }
                
                TargetGroup group = targetGroups.get(assignedPhase);
                if (group != null) {
                    group.addTarget(targetName);
                }
            }
        }
        
        return targetGroups;
    }
    
    private String determineOutputPath() {
        if (outputFile != null && !outputFile.isEmpty()) {
            return outputFile;
        }
        
        // Default to workspace root
        return new File(session.getExecutionRootDirectory(), "maven-analysis.json").getAbsolutePath();
    }
    

    
    
    private void writeResult(Map<String, Object> result, String outputPath) throws IOException {
        if (isVerbose()) {
            getLog().info("Writing analysis results to: " + outputPath);
        }
        
        // Memory optimization: use buffered writer for large JSON files
        Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls() // Handle null values consistently
            .create();
            
        try (java.io.BufferedWriter bufferedWriter = new java.io.BufferedWriter(
                new FileWriter(outputPath), 65536)) { // 64KB buffer for better I/O performance
            
            gson.toJson(result, bufferedWriter);
            bufferedWriter.flush();
            
            if (isVerbose()) {
                long fileSize = new File(outputPath).length();
                getLog().info("Successfully wrote " + (fileSize / 1024) + "KB analysis results");
            }
        }
        
        // Memory optimization: suggest GC after large JSON write operations
        long fileSize = new File(outputPath).length();
        if (fileSize > 10 * 1024 * 1024) { // > 10MB
            if (isVerbose()) {
                getLog().info("Large output file (" + (fileSize / 1024 / 1024) + "MB) - suggesting garbage collection");
            }
            System.gc();
        }
    }
}