import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service that uses Maven's introspection APIs to dynamically determine plugin and goal behavior
 * instead of hardcoded plugin-specific logic. This leverages PluginDescriptor, MojoDescriptor,
 * and parameter analysis to understand what files/directories a Mojo actually uses.
 */
public class MavenPluginIntrospectionService {
    
    private final MavenSession session;
    private final LifecycleExecutor lifecycleExecutor;
    private final Log log;
    private final boolean verbose;
    
    // Cache to avoid repeated introspection
    private final Map<String, GoalIntrospectionResult> introspectionCache = new ConcurrentHashMap<>();
    
    public MavenPluginIntrospectionService(MavenSession session, LifecycleExecutor lifecycleExecutor, Log log, boolean verbose) {
        this.session = session;
        this.lifecycleExecutor = lifecycleExecutor;
        this.log = log;
        this.verbose = verbose;
    }
    
    /**
     * Analyze a goal using Maven's introspection APIs to determine its behavior dynamically.
     */
    public GoalIntrospectionResult analyzeGoal(String goal, MavenProject project) {
        String cacheKey = project.getId() + ":" + goal;
        
        return introspectionCache.computeIfAbsent(cacheKey, k -> {
            if (verbose) {
                log.debug("Performing dynamic introspection for goal: " + goal);
            }
            
            GoalIntrospectionResult result = new GoalIntrospectionResult(goal);
            
            try {
                // 1. Find MojoExecution for this goal
                MojoExecution mojoExecution = findMojoExecution(goal, project);
                if (mojoExecution != null) {
                    analyzeMojoExecution(mojoExecution, result);
                }
                
                // 2. Analyze using MojoDescriptor if available
                if (mojoExecution != null && mojoExecution.getMojoDescriptor() != null) {
                    analyzeMojoDescriptor(mojoExecution.getMojoDescriptor(), result);
                }
                
                // 3. Analyze plugin configuration
                if (mojoExecution != null) {
                    analyzePluginConfiguration(mojoExecution, result);
                }
                
            } catch (Exception e) {
                if (verbose) {
                    log.warn("Introspection failed for goal " + goal + ": " + e.getMessage());
                }
                // Fall back to minimal analysis
                result = createFallbackResult(goal);
            }
            
            if (verbose) {
                log.debug("Introspection result for " + goal + ": " + result.toString());
            }
            
            return result;
        });
    }
    
    /**
     * Find MojoExecution for a specific goal by examining execution plans
     */
    private MojoExecution findMojoExecution(String goal, MavenProject project) {
        try {
            // Try to find the goal in various lifecycle phases
            List<String> phasesToCheck = Arrays.asList(
                "validate", "compile", "test", "package", "verify", "install", "deploy",
                "clean", "site"
            );
            
            for (String phase : phasesToCheck) {
                MavenExecutionPlan executionPlan = lifecycleExecutor.calculateExecutionPlan(session, phase);
                
                for (MojoExecution mojoExecution : executionPlan.getMojoExecutions()) {
                    if (goal.equals(mojoExecution.getGoal()) || 
                        goal.equals(mojoExecution.getPlugin().getArtifactId() + ":" + mojoExecution.getGoal())) {
                        return mojoExecution;
                    }
                }
            }
        } catch (Exception e) {
            if (verbose) {
                log.debug("Could not find MojoExecution for " + goal + ": " + e.getMessage());
            }
        }
        
        return null;
    }
    
    /**
     * Analyze MojoExecution to extract basic information
     */
    private void analyzeMojoExecution(MojoExecution mojoExecution, GoalIntrospectionResult result) {
        // Get basic execution info
        result.setPluginArtifactId(mojoExecution.getPlugin().getArtifactId());
        result.setPluginGroupId(mojoExecution.getPlugin().getGroupId());
        result.setLifecyclePhase(mojoExecution.getLifecyclePhase());
        result.setExecutionId(mojoExecution.getExecutionId());
        
        // Analyze plugin type patterns
        String artifactId = mojoExecution.getPlugin().getArtifactId();
        analyzePluginTypePatterns(artifactId, mojoExecution.getGoal(), result);
    }
    
    /**
     * Analyze MojoDescriptor to understand mojo parameters and requirements
     */
    private void analyzeMojoDescriptor(MojoDescriptor mojoDescriptor, GoalIntrospectionResult result) {
        if (mojoDescriptor == null) return;
        
        // Get mojo description and requirements
        result.setDescription(mojoDescriptor.getDescription());
        result.setRequiresDependencyResolution(mojoDescriptor.getDependencyResolutionRequired());
        result.setRequiresProject(mojoDescriptor.isProjectRequired());
        
        // Analyze parameters to understand file/directory requirements
        List<Parameter> parameters = mojoDescriptor.getParameters();
        if (parameters != null) {
            for (Parameter parameter : parameters) {
                analyzeParameter(parameter, result);
            }
        }
        
        if (verbose) {
            log.debug("MojoDescriptor analysis: " + parameters.size() + " parameters, " +
                     "requires project: " + mojoDescriptor.isProjectRequired() +
                     ", dependency resolution: " + mojoDescriptor.getDependencyResolutionRequired());
        }
    }
    
    /**
     * Analyze individual parameter to understand file/directory requirements
     */
    private void analyzeParameter(Parameter parameter, GoalIntrospectionResult result) {
        String name = parameter.getName();
        String type = parameter.getType();
        String description = parameter.getDescription();
        
        // Check for file/directory parameters
        if (isFileParameter(name, type, description)) {
            result.addFileParameter(name, type, description);
            
            // Determine if it's input or output
            if (isOutputParameter(name, description)) {
                result.addOutputPattern(name);
            } else {
                result.addInputPattern(name);
            }
        }
        
        // Check for source-related parameters
        if (isSourceParameter(name, type, description)) {
            result.setProcessesSources(true);
        }
        
        // Check for test-related parameters
        if (isTestParameter(name, type, description)) {
            result.setTestRelated(true);
        }
        
        // Check for resource parameters
        if (isResourceParameter(name, type, description)) {
            result.setNeedsResources(true);
        }
    }
    
    /**
     * Check if parameter represents a file or directory
     */
    private boolean isFileParameter(String name, String type, String description) {
        if (type == null) return false;
        
        // Check type
        if (type.equals("java.io.File") || type.equals("java.nio.file.Path") || 
            type.equals("java.lang.String") && (name.contains("Dir") || name.contains("File") || name.contains("Path"))) {
            return true;
        }
        
        // Check name patterns
        if (name != null) {
            String lowerName = name.toLowerCase();
            if (lowerName.contains("directory") || lowerName.contains("file") || lowerName.contains("path") ||
                lowerName.contains("output") || lowerName.contains("input") || lowerName.contains("source") ||
                lowerName.contains("target") || lowerName.contains("destination")) {
                return true;
            }
        }
        
        // Check description
        if (description != null) {
            String lowerDesc = description.toLowerCase();
            return lowerDesc.contains("directory") || lowerDesc.contains("file") || lowerDesc.contains("path");
        }
        
        return false;
    }
    
    /**
     * Check if parameter is an output parameter
     */
    private boolean isOutputParameter(String name, String description) {
        if (name != null) {
            String lowerName = name.toLowerCase();
            if (lowerName.contains("output") || lowerName.contains("target") || lowerName.contains("destination") ||
                lowerName.contains("generated") || lowerName.contains("build")) {
                return true;
            }
        }
        
        if (description != null) {
            String lowerDesc = description.toLowerCase();
            return lowerDesc.contains("output") || lowerDesc.contains("generate") || lowerDesc.contains("create") ||
                   lowerDesc.contains("write") || lowerDesc.contains("produce");
        }
        
        return false;
    }
    
    /**
     * Check if parameter is source-related
     */
    private boolean isSourceParameter(String name, String type, String description) {
        if (name != null && name.toLowerCase().contains("source")) return true;
        if (description != null && description.toLowerCase().contains("source")) return true;
        return false;
    }
    
    /**
     * Check if parameter is test-related
     */
    private boolean isTestParameter(String name, String type, String description) {
        if (name != null && name.toLowerCase().contains("test")) return true;
        if (description != null && description.toLowerCase().contains("test")) return true;
        return false;
    }
    
    /**
     * Check if parameter is resource-related
     */
    private boolean isResourceParameter(String name, String type, String description) {
        if (name != null && name.toLowerCase().contains("resource")) return true;
        if (description != null && description.toLowerCase().contains("resource")) return true;
        return false;
    }
    
    /**
     * Analyze plugin configuration XML to understand file/directory usage
     */
    private void analyzePluginConfiguration(MojoExecution mojoExecution, GoalIntrospectionResult result) {
        Xpp3Dom configuration = mojoExecution.getConfiguration();
        if (configuration != null) {
            analyzeConfigurationElement(configuration, result);
        }
    }
    
    /**
     * Recursively analyze configuration XML elements
     */
    private void analyzeConfigurationElement(Xpp3Dom element, GoalIntrospectionResult result) {
        if (element == null) return;
        
        String name = element.getName();
        String value = element.getValue();
        
        // Look for file/directory configurations
        if (value != null && (name.toLowerCase().contains("dir") || name.toLowerCase().contains("file") ||
                             name.toLowerCase().contains("path") || name.toLowerCase().contains("output"))) {
            result.addConfigurationPath(name, value);
        }
        
        // Recursively check child elements
        for (Xpp3Dom child : element.getChildren()) {
            analyzeConfigurationElement(child, result);
        }
    }
    
    /**
     * Analyze plugin type patterns (enhanced version of the old hardcoded logic)
     */
    private void analyzePluginTypePatterns(String artifactId, String goal, GoalIntrospectionResult result) {
        if (artifactId == null) return;
        
        // Use pattern matching but make it more flexible
        if (artifactId.contains("compiler")) {
            result.setProcessesSources(true);
            if (goal != null && goal.contains("test")) {
                result.setTestRelated(true);
            }
        } else if (artifactId.contains("surefire") || artifactId.contains("failsafe")) {
            result.setTestRelated(true);
            result.setProcessesSources(true);
        } else if (artifactId.contains("resources")) {
            result.setNeedsResources(true);
            if (goal != null && goal.contains("test")) {
                result.setTestRelated(true);
            }
        } else if (artifactId.contains("source") || artifactId.contains("javadoc")) {
            result.setProcessesSources(true);
        }
        
        // Framework-specific patterns
        if (artifactId.contains("quarkus") || artifactId.contains("spring-boot")) {
            result.setProcessesSources(true);
            result.setNeedsResources(true);
            if (goal != null && (goal.equals("dev") || goal.equals("test"))) {
                result.setTestRelated(true);
            }
        }
    }
    
    /**
     * Create fallback result when introspection fails
     */
    private GoalIntrospectionResult createFallbackResult(String goal) {
        GoalIntrospectionResult result = new GoalIntrospectionResult(goal);
        
        // Very conservative fallback
        if (goal != null) {
            if (goal.equals("compile") || goal.equals("testCompile")) {
                result.setProcessesSources(true);
                if (goal.equals("testCompile")) {
                    result.setTestRelated(true);
                }
            } else if (goal.equals("test")) {
                result.setTestRelated(true);
                result.setProcessesSources(true);
            }
        }
        
        return result;
    }
    
    /**
     * Result of goal introspection containing all discovered information
     */
    public static class GoalIntrospectionResult {
        private final String goal;
        private String pluginGroupId;
        private String pluginArtifactId;
        private String lifecyclePhase;
        private String executionId;
        private String description;
        private String requiresDependencyResolution;
        private boolean requiresProject;
        
        // Behavior flags
        private boolean processesSources = false;
        private boolean testRelated = false;
        private boolean needsResources = false;
        
        // Parameter and configuration analysis
        private final List<ParameterInfo> fileParameters = new ArrayList<>();
        private final Set<String> inputPatterns = new LinkedHashSet<>();
        private final Set<String> outputPatterns = new LinkedHashSet<>();
        private final Map<String, String> configurationPaths = new HashMap<>();
        
        public GoalIntrospectionResult(String goal) {
            this.goal = goal;
        }
        
        // Getters and setters
        public String getGoal() { return goal; }
        public String getPluginGroupId() { return pluginGroupId; }
        public void setPluginGroupId(String pluginGroupId) { this.pluginGroupId = pluginGroupId; }
        public String getPluginArtifactId() { return pluginArtifactId; }
        public void setPluginArtifactId(String pluginArtifactId) { this.pluginArtifactId = pluginArtifactId; }
        public String getLifecyclePhase() { return lifecyclePhase; }
        public void setLifecyclePhase(String lifecyclePhase) { this.lifecyclePhase = lifecyclePhase; }
        public String getExecutionId() { return executionId; }
        public void setExecutionId(String executionId) { this.executionId = executionId; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getRequiresDependencyResolution() { return requiresDependencyResolution; }
        public void setRequiresDependencyResolution(String requiresDependencyResolution) { 
            this.requiresDependencyResolution = requiresDependencyResolution; 
        }
        public boolean isRequiresProject() { return requiresProject; }
        public void setRequiresProject(boolean requiresProject) { this.requiresProject = requiresProject; }
        
        public boolean processesSources() { return processesSources; }
        public void setProcessesSources(boolean processesSources) { this.processesSources = processesSources; }
        public boolean isTestRelated() { return testRelated; }
        public void setTestRelated(boolean testRelated) { this.testRelated = testRelated; }
        public boolean needsResources() { return needsResources; }
        public void setNeedsResources(boolean needsResources) { this.needsResources = needsResources; }
        
        public void addFileParameter(String name, String type, String description) {
            fileParameters.add(new ParameterInfo(name, type, description));
        }
        
        public void addInputPattern(String pattern) { inputPatterns.add(pattern); }
        public void addOutputPattern(String pattern) { outputPatterns.add(pattern); }
        public void addConfigurationPath(String name, String path) { configurationPaths.put(name, path); }
        
        public List<ParameterInfo> getFileParameters() { return new ArrayList<>(fileParameters); }
        public Set<String> getInputPatterns() { return new LinkedHashSet<>(inputPatterns); }
        public Set<String> getOutputPatterns() { return new LinkedHashSet<>(outputPatterns); }
        public Map<String, String> getConfigurationPaths() { return new HashMap<>(configurationPaths); }
        
        /**
         * Convert to GoalBehavior for compatibility with existing code
         */
        public GoalBehavior toGoalBehavior() {
            GoalBehavior behavior = new GoalBehavior();
            behavior.setProcessesSources(processesSources);
            behavior.setTestRelated(testRelated);
            behavior.setNeedsResources(needsResources);
            return behavior;
        }
        
        @Override
        public String toString() {
            return "GoalIntrospectionResult{" +
                   "goal='" + goal + '\'' +
                   ", plugin=" + pluginGroupId + ":" + pluginArtifactId +
                   ", phase=" + lifecyclePhase +
                   ", sources=" + processesSources +
                   ", test=" + testRelated +
                   ", resources=" + needsResources +
                   ", fileParams=" + fileParameters.size() +
                   '}';
        }
    }
    
    /**
     * Information about a parameter
     */
    public static class ParameterInfo {
        private final String name;
        private final String type;
        private final String description;
        
        public ParameterInfo(String name, String type, String description) {
            this.name = name;
            this.type = type;
            this.description = description;
        }
        
        public String getName() { return name; }
        public String getType() { return type; }
        public String getDescription() { return description; }
        
        @Override
        public String toString() {
            return name + "(" + type + ")";
        }
    }
}