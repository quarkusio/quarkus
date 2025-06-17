# Complete Maven API Solution for Dynamic Goal Analysis

## Current Problem
The code in `TargetGenerationService.java` (lines 312-326) uses hardcoded logic:
```java
private boolean isSourceProcessingGoal(String goal) {
    return goal.contains("compile") || goal.contains("test") || goal.contains("build") ||
           goal.equals("dev") || goal.equals("run") || goal.contains("resources");
}

private boolean isTestGoal(String goal) {
    return goal.contains("test") || goal.equals("testCompile") || goal.equals("testResources") ||
           goal.equals("surefire:test") || goal.equals("failsafe:integration-test");
}

private boolean needsResources(String goal) {
    return goal.contains("compile") || goal.contains("resources") || 
           goal.equals("dev") || goal.equals("build") || goal.equals("run") ||
           goal.contains("test");
}
```

## Maven APIs Available for Dynamic Analysis

### 1. MojoExecution API (Already Available)
From `ExecutionPlanAnalysisService.java`:
```java
// Get execution plan and analyze each mojo
MavenExecutionPlan executionPlan = lifecycleExecutor.calculateExecutionPlan(session, phase);
for (MojoExecution mojoExecution : executionPlan.getMojoExecutions()) {
    String goal = mojoExecution.getGoal();
    String phase = mojoExecution.getLifecyclePhase();
    Plugin plugin = mojoExecution.getPlugin();
    Object configuration = mojoExecution.getConfiguration(); // Returns Xpp3Dom
}
```

### 2. Plugin Configuration Analysis (Pattern from DevMojo.java)
```java
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class PluginConfigurationAnalyzer {
    
    public static GoalBehavior analyzePluginConfiguration(Plugin plugin, String goal) {
        Xpp3Dom config = getPluginConfiguration(plugin, goal);
        GoalBehavior behavior = new GoalBehavior();
        
        // Analyze based on plugin artifact ID
        String artifactId = plugin.getArtifactId();
        
        if ("maven-compiler-plugin".equals(artifactId)) {
            behavior.setProcessesSources(true);
            if ("testCompile".equals(goal)) {
                behavior.setTestRelated(true);
            }
            
            // Get actual source directories from configuration
            List<String> sourcePaths = extractSourcePaths(config, goal);
            behavior.setSourcePaths(sourcePaths);
            
        } else if ("maven-surefire-plugin".equals(artifactId) || 
                   "maven-failsafe-plugin".equals(artifactId)) {
            behavior.setTestRelated(true);
            behavior.setProcessesSources(true); // Tests need compiled sources
            
        } else if ("maven-resources-plugin".equals(artifactId)) {
            behavior.setNeedsResources(true);
            if (goal.contains("test")) {
                behavior.setTestRelated(true);
            }
            
            // Get actual resource directories
            List<String> resourcePaths = extractResourcePaths(config, goal);
            behavior.setResourcePaths(resourcePaths);
            
        } else if (artifactId.contains("quarkus")) {
            // Quarkus plugins typically need sources and resources
            behavior.setProcessesSources(true);
            behavior.setNeedsResources(true);
            if ("dev".equals(goal) || "test".equals(goal)) {
                behavior.setTestRelated(true);
            }
        }
        
        return behavior;
    }
    
    private static List<String> extractSourcePaths(Xpp3Dom config, String goal) {
        List<String> paths = new ArrayList<>();
        
        if (config == null) return paths;
        
        // Check for custom source directories
        Xpp3Dom sourceRoots = config.getChild("compileSourceRoots");
        if (sourceRoots != null) {
            for (Xpp3Dom root : sourceRoots.getChildren()) {
                paths.add(root.getValue());
            }
        }
        
        // Check for test source directories
        if ("testCompile".equals(goal)) {
            Xpp3Dom testSourceRoots = config.getChild("testCompileSourceRoots");
            if (testSourceRoots != null) {
                for (Xpp3Dom root : testSourceRoots.getChildren()) {
                    paths.add(root.getValue());
                }
            }
        }
        
        return paths;
    }
    
    private static List<String> extractResourcePaths(Xpp3Dom config, String goal) {
        // Similar extraction for resource directories
        // Look for outputDirectory, testOutputDirectory, etc.
        return new ArrayList<>();
    }
}
```

### 3. Lifecycle Phase Analysis 
```java
public class LifecyclePhaseAnalyzer {
    
    public static GoalBehavior analyzeByPhase(String phase) {
        GoalBehavior behavior = new GoalBehavior();
        
        if (phase == null) return behavior;
        
        switch (phase) {
            // Source processing phases
            case "generate-sources":
            case "process-sources":
            case "compile":
            case "process-classes":
                behavior.setProcessesSources(true);
                break;
                
            // Test phases
            case "generate-test-sources":
            case "process-test-sources":
            case "test-compile":
            case "process-test-classes":
            case "test":
            case "integration-test":
                behavior.setTestRelated(true);
                behavior.setProcessesSources(true);
                break;
                
            // Resource phases
            case "generate-resources":
            case "process-resources":
                behavior.setNeedsResources(true);
                break;
                
            case "process-test-resources":
                behavior.setNeedsResources(true);
                behavior.setTestRelated(true);
                break;
        }
        
        return behavior;
    }
}
```

### 4. Goal Behavior Data Class
```java
public class GoalBehavior {
    private boolean processesSources = false;
    private boolean testRelated = false;
    private boolean needsResources = false;
    private List<String> sourcePaths = new ArrayList<>();
    private List<String> resourcePaths = new ArrayList<>();
    private List<String> outputPaths = new ArrayList<>();
    
    // Getters and setters...
    
    public GoalBehavior merge(GoalBehavior other) {
        GoalBehavior merged = new GoalBehavior();
        merged.processesSources = this.processesSources || other.processesSources;
        merged.testRelated = this.testRelated || other.testRelated;
        merged.needsResources = this.needsResources || other.needsResources;
        merged.sourcePaths.addAll(this.sourcePaths);
        merged.sourcePaths.addAll(other.sourcePaths);
        // ... merge other lists
        return merged;
    }
}
```

## Integration with Existing Code

### Replace Hardcoded Methods in TargetGenerationService
```java
public class TargetGenerationService {
    
    // Add new field
    private final DynamicGoalAnalysisService dynamicAnalysis;
    
    // Modified constructor
    public TargetGenerationService(Log log, boolean verbose, MavenSession session, 
                                 ExecutionPlanAnalysisService executionPlanAnalysisService) {
        this.log = log;
        this.verbose = verbose;
        this.session = session;
        this.executionPlanAnalysisService = executionPlanAnalysisService;
        this.dynamicAnalysis = new DynamicGoalAnalysisService(session, executionPlanAnalysisService);
    }
    
    // Replace hardcoded method
    private List<String> getSmartInputsForGoal(String goal, MavenProject project, String projectRootToken) {
        List<String> inputs = new ArrayList<>();
        
        // Always include POM
        inputs.add(projectRootToken + "/pom.xml");
        
        // Get dynamic analysis
        GoalBehavior behavior = dynamicAnalysis.analyzeGoal(goal, project);
        
        if (behavior.processesSources()) {
            // Use actual source directories from Maven configuration
            List<String> sourcePaths = behavior.getSourcePaths();
            if (sourcePaths.isEmpty()) {
                // Fallback to project defaults
                sourcePaths = project.getCompileSourceRoots();
            }
            
            for (String sourcePath : sourcePaths) {
                String relativePath = getRelativePathFromProject(sourcePath, project);
                if (relativePath != null && !relativePath.isEmpty()) {
                    inputs.add(projectRootToken + "/" + relativePath + "/**/*");
                }
            }
            
            // Add test sources for test-related goals
            if (behavior.isTestRelated()) {
                for (String testSourceRoot : project.getTestCompileSourceRoots()) {
                    String relativePath = getRelativePathFromProject(testSourceRoot, project);
                    if (relativePath != null && !relativePath.isEmpty()) {
                        inputs.add(projectRootToken + "/" + relativePath + "/**/*");
                    }
                }
            }
        }
        
        if (behavior.needsResources()) {
            // Use actual resource directories from Maven configuration
            List<String> resourcePaths = behavior.getResourcePaths();
            if (resourcePaths.isEmpty()) {
                // Fallback to project defaults
                if (project.getBuild() != null && project.getBuild().getResources() != null) {
                    for (Resource resource : project.getBuild().getResources()) {
                        if (resource.getDirectory() != null) {
                            resourcePaths.add(resource.getDirectory());
                        }
                    }
                }
            }
            
            for (String resourcePath : resourcePaths) {
                String relativePath = getRelativePathFromProject(resourcePath, project);
                if (relativePath != null && !relativePath.isEmpty()) {
                    inputs.add(projectRootToken + "/" + relativePath + "/**/*");
                }
            }
        }
        
        return inputs;
    }
    
    // Remove old hardcoded methods:
    // private boolean isSourceProcessingGoal(String goal) { ... }
    // private boolean isTestGoal(String goal) { ... } 
    // private boolean needsResources(String goal) { ... }
}
```

### New Dynamic Analysis Service
```java
public class DynamicGoalAnalysisService {
    
    private final MavenSession session;
    private final ExecutionPlanAnalysisService executionPlanAnalysis;
    private final Map<String, GoalBehavior> analysisCache = new ConcurrentHashMap<>();
    
    public DynamicGoalAnalysisService(MavenSession session, ExecutionPlanAnalysisService executionPlanAnalysis) {
        this.session = session;
        this.executionPlanAnalysis = executionPlanAnalysis;
    }
    
    public GoalBehavior analyzeGoal(String goal, MavenProject project) {
        String cacheKey = project.getId() + ":" + goal;
        
        return analysisCache.computeIfAbsent(cacheKey, k -> {
            GoalBehavior behavior = new GoalBehavior();
            
            // 1. Find the plugin that provides this goal
            Plugin plugin = findPluginForGoal(goal, project);
            if (plugin != null) {
                GoalBehavior pluginBehavior = PluginConfigurationAnalyzer.analyzePluginConfiguration(plugin, goal);
                behavior = behavior.merge(pluginBehavior);
            }
            
            // 2. Analyze by lifecycle phase
            String phase = executionPlanAnalysis.findPhaseForGoal(project, goal);
            if (phase != null) {
                GoalBehavior phaseBehavior = LifecyclePhaseAnalyzer.analyzeByPhase(phase);
                behavior = behavior.merge(phaseBehavior);
            }
            
            // 3. Fallback analysis by goal name patterns (for unknown plugins)
            if (!behavior.hasAnyBehavior()) {
                behavior = analyzeFallbackPatterns(goal);
            }
            
            return behavior;
        });
    }
    
    private Plugin findPluginForGoal(String goal, MavenProject project) {
        // Implementation to find which plugin provides the goal
        // This could check execution plans or plugin configurations
        return null;
    }
    
    private GoalBehavior analyzeFallbackPatterns(String goal) {
        // Fallback to some pattern matching if all else fails
        // This would be much more limited than current hardcoded approach
        GoalBehavior behavior = new GoalBehavior();
        
        if (goal.contains("compile")) {
            behavior.setProcessesSources(true);
        }
        if (goal.contains("test")) {
            behavior.setTestRelated(true);
        }
        
        return behavior;
    }
}
```

## Benefits of This Approach

1. **Accurate Analysis**: Uses actual Maven project configuration instead of string matching
2. **Extensible**: Can analyze new/custom plugins without code changes
3. **Performant**: Caches analysis results per project
4. **Maintainable**: Removes hardcoded goal lists that need manual updates
5. **Maven-Native**: Leverages Maven's own understanding of plugins and lifecycles

This solution transforms hardcoded pattern matching into dynamic Maven API-based analysis that understands the actual build configuration.