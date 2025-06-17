import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.junit.Test;

import java.io.File;

/**
 * Test Maven plugin introspection APIs to demonstrate dynamic plugin analysis
 */
public class MavenPluginIntrospectionServiceTest extends AbstractMojoTestCase {
    
    @Test
    public void testIntrospectCompilerPlugin() throws Exception {
        // Setup test project with compiler plugin
        File pom = getTestFile("src/test/resources/unit/basic-test/pom.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());
        
        MavenProject project = readMavenProject(pom);
        assertNotNull(project);
        
        // Mock session and lifecycle executor (in real usage these would be injected)
        MavenSession session = newMavenSession(project);
        LifecycleExecutor lifecycleExecutor = lookup(LifecycleExecutor.class);
        
        // Create introspection service
        MavenPluginIntrospectionService service = new MavenPluginIntrospectionService(
            session, lifecycleExecutor, getContainer().getLogger(), true);
        
        // Test introspection of compile goal
        MavenPluginIntrospectionService.GoalIntrospectionResult result = 
            service.analyzeGoal("compile", project);
        
        assertNotNull("Introspection result should not be null", result);
        assertEquals("Goal should match", "compile", result.getGoal());
        
        // Compiler plugin should process sources
        assertTrue("Compile goal should process sources", result.processesSources());
        
        // Convert to GoalBehavior
        GoalBehavior behavior = result.toGoalBehavior();
        assertTrue("Behavior should indicate source processing", behavior.processesSources());
        
        System.out.println("Compiler plugin introspection result: " + result);
        System.out.println("File parameters found: " + result.getFileParameters().size());
        for (MavenPluginIntrospectionService.ParameterInfo param : result.getFileParameters()) {
            System.out.println("  - " + param.getName() + " (" + param.getType() + ")");
        }
    }
    
    @Test
    public void testIntrospectTestGoal() throws Exception {
        File pom = getTestFile("src/test/resources/unit/basic-test/pom.xml");
        MavenProject project = readMavenProject(pom);
        MavenSession session = newMavenSession(project);
        LifecycleExecutor lifecycleExecutor = lookup(LifecycleExecutor.class);
        
        MavenPluginIntrospectionService service = new MavenPluginIntrospectionService(
            session, lifecycleExecutor, getContainer().getLogger(), true);
        
        // Test introspection of test goal
        MavenPluginIntrospectionService.GoalIntrospectionResult result = 
            service.analyzeGoal("test", project);
        
        assertNotNull("Test introspection result should not be null", result);
        
        GoalBehavior behavior = result.toGoalBehavior();
        assertTrue("Test goal should be test-related", behavior.isTestRelated());
        assertTrue("Test goal should process sources", behavior.processesSources());
        
        System.out.println("Test goal introspection result: " + result);
        System.out.println("Plugin: " + result.getPluginGroupId() + ":" + result.getPluginArtifactId());
        System.out.println("Phase: " + result.getLifecyclePhase());
        System.out.println("Requires project: " + result.isRequiresProject());
        System.out.println("Dependency resolution: " + result.getRequiresDependencyResolution());
    }
    
    @Test
    public void testEnhancedDynamicAnalysis() throws Exception {
        File pom = getTestFile("src/test/resources/unit/basic-test/pom.xml");
        MavenProject project = readMavenProject(pom);
        MavenSession session = newMavenSession(project);
        LifecycleExecutor lifecycleExecutor = lookup(LifecycleExecutor.class);
        
        // Create enhanced analysis service
        ExecutionPlanAnalysisService executionPlanService = new ExecutionPlanAnalysisService(
            getContainer().getLogger(), true, lifecycleExecutor, session, null);
        
        EnhancedDynamicGoalAnalysisService enhancedService = new EnhancedDynamicGoalAnalysisService(
            session, executionPlanService, lifecycleExecutor, getContainer().getLogger(), true);
        
        // Test enhanced analysis
        GoalBehavior compileResult = enhancedService.analyzeGoal("compile", project);
        assertNotNull("Enhanced compile analysis should not be null", compileResult);
        assertTrue("Enhanced analysis should detect source processing", compileResult.processesSources());
        
        GoalBehavior testResult = enhancedService.analyzeGoal("test", project);
        assertNotNull("Enhanced test analysis should not be null", testResult);
        assertTrue("Enhanced analysis should detect test nature", testResult.isTestRelated());
        assertTrue("Enhanced analysis should detect source processing for tests", testResult.processesSources());
        
        // Get detailed introspection result
        MavenPluginIntrospectionService.GoalIntrospectionResult detailedResult = 
            enhancedService.getIntrospectionResult("compile", project);
        
        System.out.println("Enhanced analysis - Compile goal:");
        System.out.println("  Processes sources: " + compileResult.processesSources());
        System.out.println("  Needs resources: " + compileResult.needsResources());
        System.out.println("  Test related: " + compileResult.isTestRelated());
        System.out.println("  File parameters: " + detailedResult.getFileParameters().size());
        System.out.println("  Input patterns: " + detailedResult.getInputPatterns());
        System.out.println("  Output patterns: " + detailedResult.getOutputPatterns());
        
        System.out.println("Enhanced analysis - Test goal:");
        System.out.println("  Processes sources: " + testResult.processesSources());
        System.out.println("  Needs resources: " + testResult.needsResources());
        System.out.println("  Test related: " + testResult.isTestRelated());
    }
    
    @Test
    public void testParameterAnalysis() throws Exception {
        File pom = getTestFile("src/test/resources/unit/basic-test/pom.xml");
        MavenProject project = readMavenProject(pom);
        MavenSession session = newMavenSession(project);
        LifecycleExecutor lifecycleExecutor = lookup(LifecycleExecutor.class);
        
        MavenPluginIntrospectionService service = new MavenPluginIntrospectionService(
            session, lifecycleExecutor, getContainer().getLogger(), true);
        
        // Test parameter analysis for different goals
        String[] goalNames = {"compile", "test", "package"};
        
        for (String goalName : goalNames) {
            MavenPluginIntrospectionService.GoalIntrospectionResult result = 
                service.analyzeGoal(goalName, project);
            
            System.out.println("\nGoal: " + goalName);
            System.out.println("Plugin: " + result.getPluginArtifactId());
            System.out.println("Description: " + result.getDescription());
            System.out.println("File parameters:");
            
            for (MavenPluginIntrospectionService.ParameterInfo param : result.getFileParameters()) {
                System.out.println("  - " + param.getName() + " (" + param.getType() + ")");
                if (param.getDescription() != null && param.getDescription().length() < 100) {
                    System.out.println("    Description: " + param.getDescription());
                }
            }
            
            System.out.println("Configuration paths:");
            result.getConfigurationPaths().forEach((key, value) -> 
                System.out.println("  - " + key + " = " + value));
        }
    }
    
    /**
     * This test demonstrates how the introspection service provides much more
     * information than the old hardcoded approach
     */
    @Test
    public void testComparisonWithHardcodedApproach() throws Exception {
        File pom = getTestFile("src/test/resources/unit/basic-test/pom.xml");
        MavenProject project = readMavenProject(pom);
        MavenSession session = newMavenSession(project);
        LifecycleExecutor lifecycleExecutor = lookup(LifecycleExecutor.class);
        
        // Old hardcoded approach (simplified version)
        GoalBehavior hardcodedResult = analyzeGoalHardcodedWay("compile");
        
        // New introspection approach
        MavenPluginIntrospectionService service = new MavenPluginIntrospectionService(
            session, lifecycleExecutor, getContainer().getLogger(), true);
        MavenPluginIntrospectionService.GoalIntrospectionResult introspectionResult = 
            service.analyzeGoal("compile", project);
        GoalBehavior introspectionBehavior = introspectionResult.toGoalBehavior();
        
        System.out.println("Comparison for 'compile' goal:");
        System.out.println("Hardcoded approach:");
        System.out.println("  Processes sources: " + hardcodedResult.processesSources());
        System.out.println("  Test related: " + hardcodedResult.isTestRelated());
        System.out.println("  Needs resources: " + hardcodedResult.needsResources());
        System.out.println("  Available info: Goal name only");
        
        System.out.println("Introspection approach:");
        System.out.println("  Processes sources: " + introspectionBehavior.processesSources());
        System.out.println("  Test related: " + introspectionBehavior.isTestRelated());
        System.out.println("  Needs resources: " + introspectionBehavior.needsResources());
        System.out.println("  Plugin: " + introspectionResult.getPluginGroupId() + ":" + introspectionResult.getPluginArtifactId());
        System.out.println("  Phase: " + introspectionResult.getLifecyclePhase());
        System.out.println("  Parameters: " + introspectionResult.getFileParameters().size());
        System.out.println("  Description: " + introspectionResult.getDescription());
        System.out.println("  Requires project: " + introspectionResult.isRequiresProject());
        System.out.println("  Dependency resolution: " + introspectionResult.getRequiresDependencyResolution());
        
        // The introspection approach provides much more detailed information
        assertTrue("Both approaches should agree on basic behavior", 
                  hardcodedResult.processesSources() == introspectionBehavior.processesSources());
    }
    
    /**
     * Simulate old hardcoded approach for comparison
     */
    private GoalBehavior analyzeGoalHardcodedWay(String goal) {
        GoalBehavior behavior = new GoalBehavior();
        
        // This is how the old code worked - pure pattern matching
        if ("compile".equals(goal)) {
            behavior.setProcessesSources(true);
        } else if ("testCompile".equals(goal)) {
            behavior.setProcessesSources(true);
            behavior.setTestRelated(true);
        } else if ("test".equals(goal)) {
            behavior.setTestRelated(true);
            behavior.setProcessesSources(true);
        }
        
        return behavior;
    }
}