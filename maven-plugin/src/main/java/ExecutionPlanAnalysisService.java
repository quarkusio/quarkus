import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service that analyzes Maven execution plans upfront and stores all results for efficient querying.
 * Pre-computes all Maven analysis to avoid performance bottlenecks during target dependency analysis.
 */
public class ExecutionPlanAnalysisService {
    
    private final Log log;
    private final boolean verbose;
    private final LifecycleExecutor lifecycleExecutor;
    private final MavenSession session;
    private final DefaultLifecycles defaultLifecycles;
    
    // Pre-computed analysis results per project
    private final Map<String, ProjectExecutionAnalysis> analysisCache = new ConcurrentHashMap<>();
    
    // Pre-computed lifecycle information (shared across all projects)
    private final Set<String> allLifecyclePhases;
    private final List<String> defaultLifecyclePhases;
    private final List<String> cleanLifecyclePhases;
    private final List<String> siteLifecyclePhases;
    private final Map<String, org.apache.maven.lifecycle.Lifecycle> phaseToLifecycleMap;
    
    public ExecutionPlanAnalysisService(Log log, boolean verbose, LifecycleExecutor lifecycleExecutor, MavenSession session, DefaultLifecycles defaultLifecycles) {
        this.log = log;
        this.verbose = verbose;
        this.lifecycleExecutor = lifecycleExecutor;
        this.session = session;
        this.defaultLifecycles = defaultLifecycles;
        
        // Pre-compute all lifecycle information once
        this.allLifecyclePhases = computeAllLifecyclePhases();
        this.defaultLifecyclePhases = computeLifecyclePhases("default");
        this.cleanLifecyclePhases = computeLifecyclePhases("clean");
        this.siteLifecyclePhases = computeLifecyclePhases("site");
        this.phaseToLifecycleMap = computePhaseToLifecycleMap();
        
        if (verbose) {
            log.info("Pre-computed lifecycle information: " + allLifecyclePhases.size() + " total phases");
        }
    }
    
    /**
     * Pre-analyze all projects in the reactor upfront to avoid performance bottlenecks
     */
    public void preAnalyzeAllProjects(List<MavenProject> reactorProjects) {
        if (reactorProjects == null || reactorProjects.isEmpty()) {
            return;
        }
        
        long startTime = System.currentTimeMillis();
        if (verbose) {
            log.info("Pre-analyzing execution plans for " + reactorProjects.size() + " projects...");
        }
        
        for (MavenProject project : reactorProjects) {
            String projectKey = MavenUtils.formatProjectKey(project);
            if (!analysisCache.containsKey(projectKey)) {
                analysisCache.put(projectKey, analyzeProject(project));
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        if (verbose) {
            log.info("Completed pre-analysis of " + reactorProjects.size() + " projects in " + duration + "ms");
        }
    }
    
    /**
     * Get the LifecycleExecutor for use by other services
     */
    public LifecycleExecutor getLifecycleExecutor() {
        return lifecycleExecutor;
    }
    
    /**
     * Get the DefaultLifecycles for use by other services
     */
    public DefaultLifecycles getDefaultLifecycles() {
        return defaultLifecycles;
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
     * Get all lifecycle phases from all lifecycles (default, clean, site)
     */
    public Set<String> getAllLifecyclePhases() {
        return new LinkedHashSet<>(allLifecyclePhases);
    }
    
    /**
     * Get default lifecycle phases (validate, compile, test, package, verify, install, deploy)
     */
    public List<String> getDefaultLifecyclePhases() {
        return new ArrayList<>(defaultLifecyclePhases);
    }
    
    /**
     * Get clean lifecycle phases (pre-clean, clean, post-clean)
     */
    public List<String> getCleanLifecyclePhases() {
        return new ArrayList<>(cleanLifecyclePhases);
    }
    
    /**
     * Get site lifecycle phases (pre-site, site, post-site, site-deploy)
     */
    public List<String> getSiteLifecyclePhases() {
        return new ArrayList<>(siteLifecyclePhases);
    }
    
    /**
     * Get the lifecycle that contains the specified phase
     */
    public org.apache.maven.lifecycle.Lifecycle getLifecycleForPhase(String phase) {
        if (phase == null) {
            return null;
        }
        return phaseToLifecycleMap.get(phase);
    }
    
    /**
     * Get all goals that should be completed by the time the specified phase finishes.
     * This includes goals from all phases up to and including the target phase.
     */
    public List<String> getGoalsCompletedByPhase(MavenProject project, String targetPhase) {
        if (project == null || targetPhase == null) {
            return new ArrayList<>();
        }
        
        List<String> completedGoals = new ArrayList<>();
        org.apache.maven.lifecycle.Lifecycle lifecycle = getLifecycleForPhase(targetPhase);
        
        if (lifecycle == null) {
            return completedGoals;
        }
        
        List<String> lifecyclePhases = lifecycle.getPhases();
        int targetPhaseIndex = lifecyclePhases.indexOf(targetPhase);
        
        if (targetPhaseIndex == -1) {
            return completedGoals;
        }
        
        // Use Set to avoid duplicates
        Set<String> uniqueGoals = new LinkedHashSet<>();
        
        // Get goals from all phases up to and including the target phase
        for (int i = 0; i <= targetPhaseIndex; i++) {
            String phase = lifecyclePhases.get(i);
            List<String> phaseGoals = getGoalsForPhase(project, phase);
            
            // Convert to proper plugin:goal format
            for (String goal : phaseGoals) {
                if (goal.contains(":")) {
                    // Goal is already in plugin:goal format
                    String[] parts = goal.split(":");
                    if (parts.length >= 3) {
                        // Format: groupId:artifactId:goal
                        String groupId = parts[0];
                        String artifactId = parts[1];
                        String goalName = parts[2];
                        uniqueGoals.add(groupId + ":" + artifactId + ":" + goalName);
                    } else if (parts.length == 2) {
                        // Format: plugin:goal - convert to full format
                        String plugin = parts[0];
                        String goalName = parts[1];
                        uniqueGoals.add("org.apache.maven.plugins:maven-" + plugin + "-plugin:" + goalName);
                    }
                }
            }
        }
        
        completedGoals.addAll(uniqueGoals);
        
        return completedGoals;
    }
    
    
    /**
     * Pre-compute all lifecycle phases from all lifecycles
     */
    private Set<String> computeAllLifecyclePhases() {
        Set<String> allPhases = new LinkedHashSet<>();
        allPhases.addAll(computeLifecyclePhases("default"));
        allPhases.addAll(computeLifecyclePhases("clean"));
        allPhases.addAll(computeLifecyclePhases("site"));
        return allPhases;
    }
    
    /**
     * Pre-compute lifecycle phases for a specific lifecycle ID
     */
    private List<String> computeLifecyclePhases(String lifecycleId) {
        if (defaultLifecycles == null || lifecycleId == null) {
            return new ArrayList<>();
        }

        try {
            for (org.apache.maven.lifecycle.Lifecycle lifecycle : defaultLifecycles.getLifeCycles()) {
                if (lifecycleId.equals(lifecycle.getId()) && lifecycle.getPhases() != null) {
                    return new ArrayList<>(lifecycle.getPhases());
                }
            }
            return new ArrayList<>();

        } catch (Exception e) {
            if (verbose) {
                log.warn("Could not access Maven lifecycle '" + lifecycleId + "': " + e.getMessage());
            }
            return new ArrayList<>();
        }
    }
    
    /**
     * Pre-compute phase to lifecycle mapping for efficient lookups
     */
    private Map<String, org.apache.maven.lifecycle.Lifecycle> computePhaseToLifecycleMap() {
        Map<String, org.apache.maven.lifecycle.Lifecycle> map = new HashMap<>();
        
        if (defaultLifecycles == null) {
            return map;
        }
        
        try {
            return defaultLifecycles.getPhaseToLifecycleMap();
        } catch (Exception e) {
            if (verbose) {
                log.warn("Could not build phase to lifecycle map: " + e.getMessage());
            }
            return map;
        }
    }
    
    /**
     * Analyze a project's execution plans and cache the results
     */
    private ProjectExecutionAnalysis analyzeProject(MavenProject project) {
        if (verbose) {
            log.debug("Analyzing execution plans for project: " + project.getArtifactId());
        }
        
        ProjectExecutionAnalysis analysis = new ProjectExecutionAnalysis();
        
        // Use pre-computed lifecycle phases for better performance
        for (String phase : allLifecyclePhases) {
            try {
                if (lifecycleExecutor != null && session != null) {
                    MavenExecutionPlan executionPlan = lifecycleExecutor.calculateExecutionPlan(session, phase);
                    analysis.addExecutionPlan(phase, executionPlan);
                    
                    if (verbose) {
                        log.debug("Analyzed " + executionPlan.getMojoExecutions().size() + 
                                 " executions for phase: " + phase);
                    }
                } else {
                    if (verbose) {
                        log.warn("LifecycleExecutor or session is null, skipping phase: " + phase);
                    }
                }
                
            } catch (Exception e) {
                if (verbose) {
                    log.warn("Could not calculate execution plan for " + phase + ": " + e.getMessage());
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
    
    public List<String> getGoalOutputs(String goal, String projectRootToken, MavenProject project) {
        // Simplified implementation without hardcoded patterns
        return new ArrayList<>();
    }
}
