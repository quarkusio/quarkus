import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service that analyzes Maven execution plans once and caches results for efficient querying.
 * This avoids recalculating execution plans multiple times for the same project.
 */
public class ExecutionPlanAnalysisService {
    
    private final Log log;
    private final boolean verbose;
    private final LifecycleExecutor lifecycleExecutor;
    private final MavenSession session;
    
    // Cache to store analysis results per project
    private final Map<String, ProjectExecutionAnalysis> analysisCache = new ConcurrentHashMap<>();
    
    public ExecutionPlanAnalysisService(Log log, boolean verbose, LifecycleExecutor lifecycleExecutor, MavenSession session) {
        this.log = log;
        this.verbose = verbose;
        this.lifecycleExecutor = lifecycleExecutor;
        this.session = session;
    }
    
    /**
     * Get or compute execution analysis for a project
     */
    public ProjectExecutionAnalysis getAnalysis(MavenProject project) {
        String projectKey = MavenUtils.formatProjectKey(project);
        return analysisCache.computeIfAbsent(projectKey, key -> analyzeProject(project));
    }
    
    /**
     * Find the phase for a specific goal
     */
    public String findPhaseForGoal(MavenProject project, String goal) {
        if (project == null || goal == null || goal.isEmpty()) {
            return null;
        }
        ProjectExecutionAnalysis analysis = getAnalysis(project);
        return analysis.getPhaseForGoal(goal);
    }
    
    /**
     * Get all applicable phases for a project
     */
    public Set<String> getApplicablePhases(MavenProject project) {
        if (project == null) {
            return new LinkedHashSet<>();
        }
        ProjectExecutionAnalysis analysis = getAnalysis(project);
        return analysis.getAllPhases();
    }
    
    /**
     * Get all goals for a specific phase
     */
    public List<String> getGoalsForPhase(MavenProject project, String phase) {
        if (project == null || phase == null) {
            return new ArrayList<>();
        }
        ProjectExecutionAnalysis analysis = getAnalysis(project);
        return analysis.getGoalsForPhase(phase);
    }
    
    /**
     * Analyze a project's execution plans and cache the results
     */
    private ProjectExecutionAnalysis analyzeProject(MavenProject project) {
        if (verbose) {
            log.debug("Analyzing execution plans for project: " + project.getArtifactId());
        }
        
        ProjectExecutionAnalysis analysis = new ProjectExecutionAnalysis();
        
        // Try different lifecycle endpoints to get comprehensive coverage
        String[] lifecycleEndpoints = {"compile", "test", "package", "deploy", "clean", "site"};
        
        for (String endpoint : lifecycleEndpoints) {
            try {
                if (lifecycleExecutor != null && session != null) {
                    MavenExecutionPlan executionPlan = lifecycleExecutor.calculateExecutionPlan(session, endpoint);
                    analysis.addExecutionPlan(endpoint, executionPlan);
                    
                    if (verbose) {
                        log.debug("Analyzed " + executionPlan.getMojoExecutions().size() + 
                                 " executions for endpoint: " + endpoint);
                    }
                } else {
                    if (verbose && log != null) {
                        log.warn("LifecycleExecutor or session is null, skipping endpoint: " + endpoint);
                    }
                }
                
            } catch (Exception e) {
                if (verbose && log != null) {
                    log.warn("Could not calculate execution plan for " + endpoint + ": " + e.getMessage());
                }
            }
        }
        
        if (verbose) {
            log.debug("Completed analysis for " + project.getArtifactId() + 
                     ": " + analysis.getAllPhases().size() + " phases, " + 
                     analysis.getAllGoals().size() + " goals");
        }
        
        return analysis;
    }
    
    /**
     * Clear cache (useful for testing or memory management)
     */
    public void clearCache() {
        analysisCache.clear();
    }
    
    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cachedProjects", analysisCache.size());
        stats.put("projectKeys", new ArrayList<>(analysisCache.keySet()));
        return stats;
    }
    
    // ========================================
    // Target Name Utilities (moved from MavenUtils)
    // ========================================
    
    /**
     * Generate target name from artifact ID and goal
     */
    public static String getTargetName(String artifactId, String goal) {
        String pluginName = normalizePluginName(artifactId);
        return pluginName + ":" + goal;
    }
    
    /**
     * Extract goal name from target name (e.g., "compiler:compile" -> "compile")
     */
    public static String extractGoalFromTargetName(String targetName) {
        if (targetName == null || !targetName.contains(":")) {
            return targetName;
        }
        return targetName.substring(targetName.lastIndexOf(":") + 1);
    }
    
    /**
     * Normalize plugin artifact ID to plugin name by removing common suffixes
     */
    public static String normalizePluginName(String artifactId) {
        if (artifactId == null) {
            return null;
        }
        return artifactId.replace("-maven-plugin", "").replace("-plugin", "");
    }
    
    /**
     * Get common goals for well-known plugins
     */
    public static List<String> getCommonGoalsForPlugin(String artifactId) {
        List<String> commonGoals = new ArrayList<>();
        
        if (artifactId == null) {
            return commonGoals;
        }
        
        if (artifactId.contains("compiler")) {
            commonGoals.addAll(Arrays.asList("compile", "testCompile"));
        } else if (artifactId.contains("surefire")) {
            commonGoals.add("test");
        } else if (artifactId.contains("quarkus")) {
            commonGoals.addAll(Arrays.asList("dev", "build"));
        } else if (artifactId.contains("spring-boot")) {
            commonGoals.addAll(Arrays.asList("run", "repackage"));
        }
        
        return commonGoals;
    }
    
    /**
     * Container for analyzed execution plan data for a single project
     */
    public static class ProjectExecutionAnalysis {
        private final Map<String, String> goalToPhaseMap = new HashMap<>();
        private final Map<String, List<String>> phaseToGoalsMap = new HashMap<>();
        private final Set<String> allPhases = new LinkedHashSet<>();
        private final Set<String> allGoals = new LinkedHashSet<>();
        private final Map<String, ExecutionInfo> goalToExecutionInfo = new HashMap<>();
        
        /**
         * Add execution plan data from a lifecycle endpoint
         */
        void addExecutionPlan(String endpoint, MavenExecutionPlan executionPlan) {
            for (MojoExecution mojoExecution : executionPlan.getMojoExecutions()) {
                String goal = mojoExecution.getGoal();
                String phase = mojoExecution.getLifecyclePhase();
                String pluginArtifactId = mojoExecution.getPlugin().getArtifactId();
                
                if (goal != null && phase != null) {
                    // Store goal-to-phase mapping
                    goalToPhaseMap.put(goal, phase);
                    
                    // Store detailed execution info
                    ExecutionInfo execInfo = new ExecutionInfo(
                        goal, phase, pluginArtifactId, 
                        mojoExecution.getExecutionId(),
                        mojoExecution.getPlugin().getGroupId() + ":" + pluginArtifactId
                    );
                    goalToExecutionInfo.put(goal, execInfo);
                    
                    // Handle plugin:goal format too
                    String targetName = getTargetName(pluginArtifactId, goal);
                    goalToPhaseMap.put(targetName, phase);
                    goalToExecutionInfo.put(targetName, execInfo);
                    
                    // Track phases and goals
                    allPhases.add(phase);
                    allGoals.add(goal);
                    allGoals.add(targetName);
                    
                    // Build phase-to-goals mapping
                    phaseToGoalsMap.computeIfAbsent(phase, k -> new ArrayList<>()).add(targetName);
                }
            }
        }
        
        public String getPhaseForGoal(String goal) {
            return goalToPhaseMap.get(goal);
        }
        
        public List<String> getGoalsForPhase(String phase) {
            return phaseToGoalsMap.getOrDefault(phase, new ArrayList<>());
        }
        
        public Set<String> getAllPhases() {
            return new LinkedHashSet<>(allPhases);
        }
        
        public Set<String> getAllGoals() {
            return new LinkedHashSet<>(allGoals);
        }
        
        public ExecutionInfo getExecutionInfo(String goal) {
            return goalToExecutionInfo.get(goal);
        }
        
        public Map<String, List<String>> getPhaseToGoalsMap() {
            return new HashMap<>(phaseToGoalsMap);
        }
    }
    
    /**
     * Detailed information about a mojo execution
     */
    public static class ExecutionInfo {
        private final String goal;
        private final String phase;
        private final String pluginArtifactId;
        private final String executionId;
        private final String pluginKey;
        
        public ExecutionInfo(String goal, String phase, String pluginArtifactId, String executionId, String pluginKey) {
            this.goal = goal;
            this.phase = phase;
            this.pluginArtifactId = pluginArtifactId;
            this.executionId = executionId;
            this.pluginKey = pluginKey;
        }
        
        public String getGoal() { return goal; }
        public String getPhase() { return phase; }
        public String getPluginArtifactId() { return pluginArtifactId; }
        public String getExecutionId() { return executionId; }
        public String getPluginKey() { return pluginKey; }
    }
}