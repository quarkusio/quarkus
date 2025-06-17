# Maven API Examples for Dynamic Goal Analysis

## Key Maven API Classes Found in Codebase

### 1. MojoExecution API (ExecutionPlanAnalysisService.java)
```java
// Get all mojo executions for a phase
MavenExecutionPlan executionPlan = lifecycleExecutor.calculateExecutionPlan(session, phase);
for (MojoExecution mojoExecution : executionPlan.getMojoExecutions()) {
    String goal = mojoExecution.getGoal();
    String phase = mojoExecution.getLifecyclePhase();
    Plugin plugin = mojoExecution.getPlugin();
    String pluginArtifactId = plugin.getArtifactId();
    String pluginKey = plugin.getGroupId() + ":" + pluginArtifactId;
    String executionId = mojoExecution.getExecutionId();
    
    // Access execution configuration
    Object configuration = mojoExecution.getConfiguration();
}
```

### 2. Plugin Configuration Access (DevMojo.java pattern)
```java
// Access plugin configuration using Xpp3Dom
private Xpp3Dom getPluginConfig(Plugin plugin, String executionId, String goal) {
    Xpp3Dom mergedConfig = null;
    
    // Get execution-specific configuration
    for (PluginExecution exec : plugin.getExecutions()) {
        if (exec.getConfiguration() != null && exec.getGoals().contains(goal)) {
            mergedConfig = mergedConfig == null ? (Xpp3Dom) exec.getConfiguration()
                    : Xpp3Dom.mergeXpp3Dom(mergedConfig, (Xpp3Dom) exec.getConfiguration(), true);
        }
    }
    
    // Merge with plugin-level configuration
    if ((Xpp3Dom) plugin.getConfiguration() != null) {
        mergedConfig = mergedConfig == null ? (Xpp3Dom) plugin.getConfiguration()
                : Xpp3Dom.mergeXpp3Dom(mergedConfig, (Xpp3Dom) plugin.getConfiguration(), true);
    }
    
    return mergedConfig;
}

// Example: Read compiler plugin source directories
private List<String> getCompilerSourceRoots(Xpp3Dom config) {
    if (config == null) return Collections.emptyList();
    
    Xpp3Dom sourceRoots = config.getChild("compileSourceRoots");
    if (sourceRoots == null) return Collections.emptyList();
    
    List<String> paths = new ArrayList<>();
    for (Xpp3Dom root : sourceRoots.getChildren("compileSourceRoot")) {
        paths.add(root.getValue());
    }
    return paths;
}
```

### 3. PluginDescriptor API (QuarkusMavenPluginDocsGenerator.java)
```java
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptorBuilder;

// Load plugin descriptor from plugin.xml
PluginDescriptor pluginDescriptor;
try (Reader input = new XmlStreamReader(new FileInputStream(pluginXmlPath))) {
    pluginDescriptor = new PluginDescriptorBuilder().build(input);
}

// Analyze all mojos in plugin
for (MojoDescriptor mojo : pluginDescriptor.getMojos()) {
    String goalName = mojo.getGoal();
    String fullGoalName = mojo.getFullGoalName(); // plugin:goal format
    String description = mojo.getDescription();
    String phase = mojo.getPhase(); // Default phase binding
    
    // Analyze parameters to understand goal behavior
    for (Parameter parameter : mojo.getParameters()) {
        String paramName = parameter.getName();
        String paramType = parameter.getType();
        String expression = parameter.getExpression(); // ${property.name}
        String defaultValue = parameter.getDefaultValue();
        boolean required = parameter.isRequired();
        
        // Infer behavior from parameter types and names
        if (paramType.contains("File") && paramName.contains("source")) {
            // This goal processes source files
        }
        if (paramName.contains("test") || expression.contains("test")) {
            // This goal is test-related
        }
    }
}
```

## Dynamic Goal Analysis Implementation

### Proposed Service: DynamicGoalAnalysisService
```java
public class DynamicGoalAnalysisService {
    
    private final PluginManager pluginManager;
    private final MavenSession session;
    
    public GoalAnalysis analyzeGoal(String pluginKey, String goal, MavenProject project) {
        GoalAnalysis analysis = new GoalAnalysis(goal);
        
        // 1. Get plugin configuration
        Plugin plugin = findPlugin(project, pluginKey);
        if (plugin != null) {
            Xpp3Dom config = getPluginConfig(plugin, null, goal);
            analysis.setConfiguration(config);
        }
        
        // 2. Get mojo descriptor if available
        try {
            MojoDescriptor mojoDescriptor = getMojoDescriptor(pluginKey, goal);
            analysis.setMojoDescriptor(mojoDescriptor);
            
            // Analyze parameters to infer behavior
            analyzeParameters(mojoDescriptor, analysis);
        } catch (Exception e) {
            // Fallback to heuristic analysis
        }
        
        // 3. Analyze lifecycle phase binding
        String phase = findPhaseForGoal(project, goal);
        analysis.setPhase(phase);
        analyzePhaseBinding(phase, analysis);
        
        return analysis;
    }
    
    private void analyzeParameters(MojoDescriptor mojo, GoalAnalysis analysis) {
        for (Parameter param : mojo.getParameters()) {
            String name = param.getName().toLowerCase();
            String type = param.getType();
            String expression = param.getExpression();
            
            // Source processing indicators
            if ((name.contains("source") || name.contains("compile")) && 
                (type.contains("File") || type.contains("String"))) {
                analysis.setProcessesSources(true);
            }
            
            // Test indicators
            if (name.contains("test") || (expression != null && expression.contains("test"))) {
                analysis.setTestRelated(true);
            }
            
            // Resource indicators  
            if (name.contains("resource") && type.contains("File")) {
                analysis.setNeedsResources(true);
            }
            
            // Output indicators
            if ((name.contains("output") || name.contains("target")) && type.contains("File")) {
                analysis.addOutputPath(param.getDefaultValue());
            }
        }
    }
    
    private void analyzePhaseBinding(String phase, GoalAnalysis analysis) {
        if (phase == null) return;
        
        // Maven standard phases indicate goal behavior
        switch (phase) {
            case "generate-sources":
            case "process-sources":
            case "compile":
            case "process-classes":
                analysis.setProcessesSources(true);
                break;
                
            case "generate-test-sources":
            case "process-test-sources":
            case "test-compile":
            case "process-test-classes":
            case "test":
            case "integration-test":
                analysis.setTestRelated(true);
                analysis.setProcessesSources(true);
                break;
                
            case "generate-resources":
            case "process-resources":
            case "process-test-resources":
                analysis.setNeedsResources(true);
                break;
        }
    }
}

public class GoalAnalysis {
    private String goal;
    private String phase;
    private boolean processesSources = false;
    private boolean testRelated = false;
    private boolean needsResources = false;
    private List<String> outputPaths = new ArrayList<>();
    private Xpp3Dom configuration;
    private MojoDescriptor mojoDescriptor;
    
    // ... getters and setters
}
```

## Integration with TargetGenerationService

Replace hardcoded methods:
```java
// OLD: Hardcoded
private boolean isSourceProcessingGoal(String goal) {
    return goal.contains("compile") || goal.contains("test") || ...;
}

// NEW: Dynamic analysis
private boolean isSourceProcessingGoal(String goal, MavenProject project) {
    GoalAnalysis analysis = dynamicGoalAnalysisService.analyzeGoal(
        getPluginKeyForGoal(goal), goal, project);
    return analysis.processesSources();
}
```

This approach uses actual Maven APIs to understand goal behavior instead of hardcoded patterns.