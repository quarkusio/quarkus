import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import model.CreateNodesV2Entry;
import model.RawProjectGraphDependency;
import model.TargetConfiguration;
import model.TargetGroup;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Refactored Maven plugin that analyzes Maven projects for Nx integration.
 * This version delegates complex logic to specialized service classes.
 */
@Mojo(name = "analyze", aggregator = true, requiresDependencyResolution = ResolutionScope.NONE)
public class NxAnalyzerMojo extends AbstractMojo {
    
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;
    
    @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
    private List<MavenProject> reactorProjects;
    
    @Parameter(property = "nx.outputFile")
    private String outputFile;
    
    @Parameter(property = "nx.verbose", defaultValue = "false")
    private String verboseStr;
    
    @Component
    private LifecycleExecutor lifecycleExecutor;
    
    @Component
    private DefaultLifecycles defaultLifecycles;
    
    // Services for delegating complex operations
    private ExecutionPlanAnalysisService executionPlanAnalysisService;
    private TargetGenerationService targetGenerationService;
    private TargetGroupService targetGroupService;
    private TargetDependencyService targetDependencyService;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        long overallStartTime = System.currentTimeMillis();
        
        getLog().info("🔥 Starting Maven analysis for " + reactorProjects.size() + " projects...");
        
        try {
            // Phase 1: Initialize services
            long initStart = System.currentTimeMillis();
            initializeServices();
            long initDuration = System.currentTimeMillis() - initStart;
            getLog().info("⏱️  Service initialization completed in " + initDuration + "ms");
            
            logBasicInfo();
            
            // Phase 2: Perform Maven analysis
            long analysisStart = System.currentTimeMillis();
            getLog().info("📊 Starting Maven workspace analysis...");
            Map<String, Object> result = performAnalysis();
            long analysisDuration = System.currentTimeMillis() - analysisStart;
            getLog().info("⏱️  Maven workspace analysis completed in " + analysisDuration + "ms (" + 
                         String.format("%.2f", analysisDuration / 1000.0) + "s)");
            
            // Phase 3: Write output
            long writeStart = System.currentTimeMillis();
            String outputPath = determineOutputPath();
            writeResult(result, outputPath);
            long writeDuration = System.currentTimeMillis() - writeStart;
            getLog().info("⏱️  Output file writing completed in " + writeDuration + "ms");
            
            logCompletion(overallStartTime, outputPath, analysisDuration);
            
        } catch (Exception e) {
            long failureDuration = System.currentTimeMillis() - overallStartTime;
            getLog().error("❌ Analysis failed after " + failureDuration + "ms: " + e.getMessage());
            throw new MojoExecutionException("Analysis failed: " + e.getMessage(), e);
        }
    }
    
    private void initializeServices() {
        this.executionPlanAnalysisService = new ExecutionPlanAnalysisService(getLog(), isVerbose(), lifecycleExecutor, session, defaultLifecycles);
        this.targetGenerationService = new TargetGenerationService(getLog(), isVerbose(), session, executionPlanAnalysisService);
        this.targetGroupService = new TargetGroupService(executionPlanAnalysisService);
        this.targetDependencyService = new TargetDependencyService(getLog(), isVerbose(), executionPlanAnalysisService);
    }
    
    private void logBasicInfo() {
        if (isVerbose()) {
            getLog().info("Verbose mode enabled");
        }
        
        getLog().info("Root directory: " + session.getExecutionRootDirectory());
        if (!reactorProjects.isEmpty()) {
            MavenProject first = reactorProjects.get(0);
            MavenProject last = reactorProjects.get(reactorProjects.size() - 1);
            getLog().info("First project: " + first.getGroupId() + ":" + first.getArtifactId());
            getLog().info("Last project: " + last.getGroupId() + ":" + last.getArtifactId());
        }
    }
    
    private Map<String, Object> performAnalysis() {
        File workspaceRoot = new File(session.getExecutionRootDirectory());
        
        // Phase 1: Calculate project dependencies
        long depCalcStart = System.currentTimeMillis();
        Map<MavenProject, List<MavenProject>> projectDependencies = calculateProjectDependencies();
        long depCalcDuration = System.currentTimeMillis() - depCalcStart;
        getLog().info("⏱️  Project dependency calculation completed in " + depCalcDuration + "ms");
        
        // Phase 2: Generate targets and groups for each project
        long targetGenStart = System.currentTimeMillis();
        Map<MavenProject, Map<String, TargetConfiguration>> projectTargets = new LinkedHashMap<>();
        Map<MavenProject, Map<String, TargetGroup>> projectTargetGroups = new LinkedHashMap<>();
        
        int processedProjects = 0;
        for (MavenProject project : reactorProjects) {
            long projectStart = System.currentTimeMillis();
            try {
                // Get actual project dependencies for this project (not all reactor projects)
                List<MavenProject> actualDependencies = projectDependencies.getOrDefault(project, new ArrayList<>());
                
                // Calculate goal dependencies using only actual dependencies
                Map<String, List<Object>> goalDependencies = calculateGoalDependencies(project, actualDependencies);
                
                // Generate targets using pre-calculated goal dependencies (phase dependencies calculated later)
                Map<String, TargetConfiguration> targets = targetGenerationService.generateTargets(
                    project, workspaceRoot, goalDependencies, new LinkedHashMap<>());
                
                // Now calculate phase dependencies using the generated targets
                Map<String, List<Object>> phaseDependencies = calculatePhaseDependencies(project, targets);
                
                // Update phase targets with calculated dependencies
                updatePhaseTargetsWithDependencies(targets, phaseDependencies);
                projectTargets.put(project, targets);
                
                // Generate target groups
                Map<String, TargetGroup> targetGroups = targetGroupService.generateTargetGroups(project, targets, session);
                projectTargetGroups.put(project, targetGroups);
                
                processedProjects++;
                long projectDuration = System.currentTimeMillis() - projectStart;
                
                if (isVerbose()) {
                    getLog().info("✅ Processed " + project.getArtifactId() + " in " + projectDuration + "ms" +
                                 ": " + targets.size() + " targets, " + targetGroups.size() + " groups, " +
                                 actualDependencies.size() + " project dependencies");
                } else if (processedProjects % 100 == 0) {
                    // Log progress every 100 projects for large workspaces
                    long avgTimePerProject = (System.currentTimeMillis() - targetGenStart) / processedProjects;
                    getLog().info("📊 Processed " + processedProjects + "/" + reactorProjects.size() + 
                                 " projects (avg " + avgTimePerProject + "ms per project)");
                }
                
            } catch (Exception e) {
                getLog().error("Error processing project " + project.getArtifactId() + ": " + e.getMessage(), e);
                // Continue with empty targets and groups
                projectTargets.put(project, new LinkedHashMap<>());
                projectTargetGroups.put(project, new LinkedHashMap<>());
            }
        }
        
        long targetGenDuration = System.currentTimeMillis() - targetGenStart;
        getLog().info("⏱️  Target generation completed in " + targetGenDuration + "ms for " + 
                     processedProjects + " projects (avg " + (targetGenDuration / processedProjects) + "ms per project)");
        
        // Phase 3: Generate Nx-compatible outputs
        long outputGenStart = System.currentTimeMillis();
        List<CreateNodesV2Entry> createNodesEntries = CreateNodesResultGenerator.generateCreateNodesV2Results(
            reactorProjects, workspaceRoot, projectTargets, projectTargetGroups);
        
        List<Object[]> createNodesResults = new ArrayList<>();
        for (CreateNodesV2Entry entry : createNodesEntries) {
            createNodesResults.add(entry.toArray());
        }
        
        List<RawProjectGraphDependency> createDependencies = CreateDependenciesGenerator.generateCreateDependencies(
            reactorProjects, workspaceRoot, getLog(), isVerbose());
        
        long outputGenDuration = System.currentTimeMillis() - outputGenStart;
        getLog().info("⏱️  Nx output generation completed in " + outputGenDuration + "ms");
        
        if (isVerbose()) {
            getLog().info("Generated " + createDependencies.size() + " workspace dependencies");
        }
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("createNodesResults", createNodesResults);
        result.put("createDependencies", createDependencies);
        
        return result;
    }
    
    /**
     * Calculate actual project dependencies for all projects in the reactor.
     * Returns a map from each project to its list of actual Maven dependency projects.
     */
    private Map<MavenProject, List<MavenProject>> calculateProjectDependencies() {
        Map<MavenProject, List<MavenProject>> projectDependencies = new LinkedHashMap<>();
        
        // Build artifact mapping for workspace projects
        Map<String, MavenProject> artifactToProject = new HashMap<>();
        for (MavenProject project : reactorProjects) {
            if (project.getGroupId() != null && project.getArtifactId() != null) {
                String key = MavenUtils.formatProjectKey(project);
                artifactToProject.put(key, project);
            }
        }
        
        // Calculate dependencies for each project
        for (MavenProject project : reactorProjects) {
            List<MavenProject> dependencies = new ArrayList<>();
            
            if (project.getDependencies() != null) {
                for (org.apache.maven.model.Dependency dep : project.getDependencies()) {
                    if (dep.getGroupId() != null && dep.getArtifactId() != null) {
                        String depKey = dep.getGroupId() + ":" + dep.getArtifactId();
                        
                        // Check if this dependency refers to another project in workspace
                        MavenProject targetProject = artifactToProject.get(depKey);
                        if (targetProject != null && !targetProject.equals(project)) {
                            dependencies.add(targetProject);
                        }
                    }
                }
            }
            
            projectDependencies.put(project, dependencies);
            
            if (isVerbose() && !dependencies.isEmpty()) {
                getLog().debug("Project " + project.getArtifactId() + " depends on " + 
                             dependencies.size() + " workspace projects");
            }
        }
        
        return projectDependencies;
    }

    /**
     * Calculate goal dependencies for a project using only actual project dependencies
     */
    private Map<String, List<Object>> calculateGoalDependencies(MavenProject project, List<MavenProject> actualDependencies) {
        Map<String, List<Object>> goalDependencies = new LinkedHashMap<>();
        
        if (isVerbose()) {
            getLog().debug("Calculating goal dependencies for " + project.getArtifactId());
        }
        
        // First pass: collect all potential goal targets
        Set<String> goalTargets = collectGoalTargets(project);
        
        // Calculate dependencies for each goal target
        for (String targetName : goalTargets) {
            String goal = ExecutionPlanAnalysisService.extractGoalFromTargetName(targetName);
            
            // Try to find execution phase from plugin configuration
            String executionPhase = findExecutionPhase(project, targetName);
            
            List<Object> dependencies = targetDependencyService.calculateGoalDependencies(
                project, executionPhase, targetName, actualDependencies);
            goalDependencies.put(targetName, dependencies);
        }
        
        return goalDependencies;
    }
    
    /**
     * Calculate phase dependencies for a project using generated targets
     */
    private Map<String, List<Object>> calculatePhaseDependencies(MavenProject project, Map<String, TargetConfiguration> allTargets) {
        Map<String, List<Object>> phaseDependencies = new LinkedHashMap<>();
        
        // Get all lifecycle phases from all Maven lifecycles (default, clean, site)
        Set<String> allPhases = executionPlanAnalysisService.getAllLifecyclePhases();
        
        if (isVerbose()) {
            getLog().debug("Calculating dependencies for " + allPhases.size() + " lifecycle phases: " + allPhases);
        }
        
        for (String phase : allPhases) {
            List<Object> dependencies = targetDependencyService.calculatePhaseDependencies(
                phase, allTargets, project, reactorProjects);
            phaseDependencies.put(phase, dependencies);
        }
        
        return phaseDependencies;
    }
    
    /**
     * Update phase targets with calculated dependencies
     */
    private void updatePhaseTargetsWithDependencies(Map<String, TargetConfiguration> targets, 
                                                   Map<String, List<Object>> phaseDependencies) {
        for (Map.Entry<String, List<Object>> entry : phaseDependencies.entrySet()) {
            String phase = entry.getKey();
            List<Object> dependencies = entry.getValue();
            
            TargetConfiguration phaseTarget = targets.get(phase);
            if (phaseTarget != null) {
                phaseTarget.setDependsOn(dependencies);
            }
        }
    }
    
    /**
     * Collect all potential goal targets from project plugins
     */
    private Set<String> collectGoalTargets(MavenProject project) {
        Set<String> goalTargets = new LinkedHashSet<>();
        
        if (isVerbose()) {
            getLog().debug("Collecting goals for " + project.getArtifactId() + " (" + 
                          (project.getBuildPlugins() != null ? project.getBuildPlugins().size() : 0) + " plugins)");
        }
        
        if (project.getBuildPlugins() != null) {
            project.getBuildPlugins().forEach(plugin -> {
                String artifactId = plugin.getArtifactId();
                
                // Add goals from executions
                if (plugin.getExecutions() != null) {
                    plugin.getExecutions().forEach(execution -> {
                        if (execution.getGoals() != null) {
                            execution.getGoals().forEach(goal -> {
                                String targetName = ExecutionPlanAnalysisService.getTargetName(artifactId, goal);
                                goalTargets.add(targetName);
                            });
                        }
                    });
                }
                
                // Add common goals for well-known plugins
                addCommonGoals(artifactId, goalTargets);
            });
        }
        
        if (isVerbose()) {
            getLog().debug("Found " + goalTargets.size() + " goals for " + project.getArtifactId());
        }
        return goalTargets;
    }
    
    /**
     * Find execution phase for a goal target
     */
    private String findExecutionPhase(MavenProject project, String targetName) {
        String goal = ExecutionPlanAnalysisService.extractGoalFromTargetName(targetName);
        
        if (project.getBuildPlugins() != null) {
            for (org.apache.maven.model.Plugin plugin : project.getBuildPlugins()) {
                if (plugin.getExecutions() != null) {
                    for (org.apache.maven.model.PluginExecution execution : plugin.getExecutions()) {
                        if (execution.getGoals() != null && execution.getGoals().contains(goal)) {
                            return execution.getPhase();
                        }
                    }
                }
            }
        }
        
        return null; // Will trigger phase inference in dependency service
    }
    
    /**
     * Add common goals for well-known plugins
     */
    private void addCommonGoals(String artifactId, Set<String> goalTargets) {
        List<String> commonGoals = ExecutionPlanAnalysisService.getCommonGoalsForPlugin(artifactId);
        for (String goal : commonGoals) {
            goalTargets.add(ExecutionPlanAnalysisService.getTargetName(artifactId, goal));
        }
    }
    
    
    private boolean isVerbose() {
        String systemProp = System.getProperty("nx.verbose");
        boolean fromParam = "true".equalsIgnoreCase(verboseStr);
        boolean fromSystem = "true".equalsIgnoreCase(systemProp);
        return fromParam || fromSystem;
    }
    
    private String determineOutputPath() {
        if (outputFile != null && !outputFile.isEmpty()) {
            return outputFile;
        }
        return new File(session.getExecutionRootDirectory(), "maven-analysis.json").getAbsolutePath();
    }
    
    private void writeResult(Map<String, Object> result, String outputPath) throws IOException {
        Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();
            
        try (FileWriter writer = new FileWriter(outputPath)) {
            gson.toJson(result, writer);
        }
        
        if (isVerbose()) {
            long fileSize = new File(outputPath).length();
            getLog().info("Wrote " + (fileSize / 1024) + "KB analysis results");
        }
    }
    
    private void logCompletion(long startTime, String outputPath, long analysisDuration) {
        long totalTime = System.currentTimeMillis() - startTime;
        
        // Calculate analysis percentage of total time
        double analysisPercentage = (analysisDuration * 100.0) / totalTime;
        
        // Always show comprehensive timing information
        getLog().info("✅ Maven analysis completed successfully!");
        getLog().info("📊 Total execution time: " + String.format("%.2f", totalTime / 1000.0) + "s");
        getLog().info("⚡ Core analysis time: " + String.format("%.2f", analysisDuration / 1000.0) + "s (" + 
                     String.format("%.1f", analysisPercentage) + "% of total)");
        getLog().info("📁 Output written to: " + outputPath);
        getLog().info("🏗️  Projects analyzed: " + reactorProjects.size());
        
        // Performance insights
        if (reactorProjects.size() > 0) {
            double avgTimePerProject = analysisDuration / (double) reactorProjects.size();
            getLog().info("⏱️  Average time per project: " + String.format("%.1f", avgTimePerProject) + "ms");
            
            if (avgTimePerProject > 1000) {
                getLog().warn("⚠️  High average time per project detected - consider optimizing large projects");
            } else if (avgTimePerProject < 10) {
                getLog().info("🚀 Excellent performance - very fast analysis per project");
            }
        }
    }
}