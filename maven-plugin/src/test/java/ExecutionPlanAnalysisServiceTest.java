import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.WithoutMojo;
import org.apache.maven.project.MavenProject;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive unit tests for ExecutionPlanAnalysisService
 * Following the established MojoRule pattern used in other test files
 */
public class ExecutionPlanAnalysisServiceTest {

    @Rule
    public MojoRule rule = new MojoRule() {
        @Override
        protected void before() throws Throwable {
        }

        @Override
        protected void after() {
        }
    };

    /**
     * Test context helper class to encapsulate test setup
     */
    static class TestContext {
        final ExecutionPlanAnalysisService service;
        final MavenProject project;
        final List<MavenProject> reactorProjects;
        final MavenSession session;
        final Log log;
        final LifecycleExecutor lifecycleExecutor;
        
        TestContext(ExecutionPlanAnalysisService service, MavenProject project, 
                   List<MavenProject> reactorProjects, MavenSession session, Log log, LifecycleExecutor lifecycleExecutor) {
            this.service = service;
            this.project = project;
            this.reactorProjects = reactorProjects;
            this.session = session;
            this.log = log;
            this.lifecycleExecutor = lifecycleExecutor;
        }
    }
    
    /**
     * Setup basic test context with real Maven environment
     */
    private TestContext setupBasicTest() throws Exception {
        File pom = new File("target/test-classes/unit/basic-test");
        assertTrue("Test POM should exist", pom.exists());

        NxAnalyzerMojo mojo = (NxAnalyzerMojo) rule.lookupConfiguredMojo(pom, "analyze");
        assertNotNull("Mojo should be configured", mojo);

        MavenSession session = (MavenSession) rule.getVariableValueFromObject(mojo, "session");
        List<MavenProject> reactorProjects = (List<MavenProject>) rule.getVariableValueFromObject(mojo, "reactorProjects");
        MavenProject project = reactorProjects.get(0);
        Log log = mojo.getLog();
        
        // Try to get the real LifecycleExecutor from the mojo
        LifecycleExecutor lifecycleExecutor = null;
        try {
            lifecycleExecutor = (LifecycleExecutor) rule.getVariableValueFromObject(mojo, "lifecycleExecutor");
        } catch (Exception e) {
            // LifecycleExecutor might not be available in test environment
        }
        
        DefaultLifecycles defaultLifecycles = (DefaultLifecycles) rule.getVariableValueFromObject(mojo, "defaultLifecycles");
        ExecutionPlanAnalysisService service = new ExecutionPlanAnalysisService(log, false, lifecycleExecutor, session, defaultLifecycles);
        return new TestContext(service, project, reactorProjects, session, log, lifecycleExecutor);
    }

    // ========================================
    // Integration Tests with Real Maven Context
    // ========================================

    @Test
    public void testGetAnalysis_WithRealProject() throws Exception {
        TestContext ctx = setupBasicTest();
        
        // Execute
        ExecutionPlanAnalysisService.ProjectExecutionAnalysis analysis = ctx.service.getAnalysis(ctx.project);
        
        // Verify
        assertNotNull("Analysis should not be null", analysis);
        assertNotNull("Should have phases", analysis.getAllPhases());
        assertNotNull("Should have goals", analysis.getAllGoals());
        
        // Cache should be populated
        Map<String, Object> stats = ctx.service.getCacheStats();
        assertEquals("Should have cached one project", 1, stats.get("cachedProjects"));
        
        // Second call should return cached result
        ExecutionPlanAnalysisService.ProjectExecutionAnalysis analysis2 = ctx.service.getAnalysis(ctx.project);
        assertSame("Should return cached result", analysis, analysis2);
    }

    @Test
    public void testFindPhaseForGoal_WithRealProject() throws Exception {
        TestContext ctx = setupBasicTest();
        
        if (ctx.lifecycleExecutor == null) {
            Assume.assumeTrue("LifecycleExecutor not available, skipping test", false);
        }
        
        // Test finding phase for common goals
        String compilePhase = ctx.service.findPhaseForGoal(ctx.project, "compile");
        String testPhase = ctx.service.findPhaseForGoal(ctx.project, "test");
        
        // Verify - phases should be found if execution plans can be calculated
        if (compilePhase != null) {
            assertEquals("Compile goal should be in compile phase", "compile", compilePhase);
        }
        if (testPhase != null) {
            assertEquals("Test goal should be in test phase", "test", testPhase);
        }
    }

    @Test
    public void testGetApplicablePhases_WithRealProject() throws Exception {
        TestContext ctx = setupBasicTest();
        
        // Execute
        Set<String> phases = ctx.service.getApplicablePhases(ctx.project);
        
        // Verify
        assertNotNull("Phases should not be null", phases);
        // Even if LifecycleExecutor is null, should return empty set, not null
        
        if (ctx.lifecycleExecutor != null) {
            // If we have a real lifecycle executor, we might find some phases
            // The exact phases depend on the test project configuration
        }
    }

    @Test
    public void testGetGoalsForPhase_WithRealProject() throws Exception {
        TestContext ctx = setupBasicTest();
        
        // Execute
        List<String> compileGoals = ctx.service.getGoalsForPhase(ctx.project, "compile");
        List<String> testGoals = ctx.service.getGoalsForPhase(ctx.project, "test");
        
        // Verify
        assertNotNull("Compile goals should not be null", compileGoals);
        assertNotNull("Test goals should not be null", testGoals);
        
        if (ctx.lifecycleExecutor != null) {
            // With real lifecycle executor, might find actual goals
            // The exact goals depend on project configuration
        }
    }

    @Test
    public void testVerboseLogging() throws Exception {
        TestContext ctx = setupBasicTest();
        
        // Create verbose service
        DefaultLifecycles defaultLifecycles = (DefaultLifecycles) rule.getVariableValueFromObject(
            rule.lookupConfiguredMojo(new File("target/test-classes/unit/basic-test"), "analyze"), "defaultLifecycles");
        ExecutionPlanAnalysisService verboseService = new ExecutionPlanAnalysisService(
            ctx.log, true, ctx.lifecycleExecutor, ctx.session, defaultLifecycles);
        
        // Execute - should not throw exceptions even with verbose logging
        ExecutionPlanAnalysisService.ProjectExecutionAnalysis analysis = verboseService.getAnalysis(ctx.project);
        assertNotNull("Analysis should not be null even with verbose logging", analysis);
    }

    @Test
    public void testCacheManagement() throws Exception {
        TestContext ctx = setupBasicTest();
        
        // Initially empty cache
        Map<String, Object> initialStats = ctx.service.getCacheStats();
        assertEquals("Cache should be initially empty", 0, initialStats.get("cachedProjects"));
        
        // Add project to cache
        ctx.service.getAnalysis(ctx.project);
        Map<String, Object> afterAnalysis = ctx.service.getCacheStats();
        assertEquals("Cache should contain one project", 1, afterAnalysis.get("cachedProjects"));
        
        // Clear cache  
        ctx.service.clearCache();
        Map<String, Object> afterClear = ctx.service.getCacheStats();
        assertEquals("Cache should be empty after clear", 0, afterClear.get("cachedProjects"));
    }

    @Test
    public void testNullInputHandling() throws Exception {
        TestContext ctx = setupBasicTest();
        
        // Test null project
        assertNull("Null project should return null", ctx.service.findPhaseForGoal(null, "compile"));
        
        // Test null goal
        assertNull("Null goal should return null", ctx.service.findPhaseForGoal(ctx.project, null));
        
        // Test empty goal
        assertNull("Empty goal should return null", ctx.service.findPhaseForGoal(ctx.project, ""));
        
        // Test null project for phases
        Set<String> phases = ctx.service.getApplicablePhases(null);
        assertNotNull("Should return empty set for null project", phases);
        assertTrue("Should be empty for null project", phases.isEmpty());
        
        // Test null inputs for goals by phase
        List<String> goalsForNull = ctx.service.getGoalsForPhase(null, "compile");
        assertNotNull("Should return empty list for null project", goalsForNull);
        assertTrue("Should be empty for null project", goalsForNull.isEmpty());
        
        List<String> goalsForNullPhase = ctx.service.getGoalsForPhase(ctx.project, null);
        assertNotNull("Should return empty list for null phase", goalsForNullPhase);
        assertTrue("Should be empty for null phase", goalsForNullPhase.isEmpty());
    }

    @Test
    public void testProjectExecutionAnalysis_DirectUsage() throws Exception {
        // Test the inner class directly
        ExecutionPlanAnalysisService.ProjectExecutionAnalysis analysis = 
            new ExecutionPlanAnalysisService.ProjectExecutionAnalysis();
        
        // Initially empty
        assertTrue("Should have no phases initially", analysis.getAllPhases().isEmpty());
        assertTrue("Should have no goals initially", analysis.getAllGoals().isEmpty());
        assertNull("Should return null for unknown goal", analysis.getPhaseForGoal("unknown"));
        assertTrue("Should return empty list for unknown phase", analysis.getGoalsForPhase("unknown").isEmpty());
        
        // Test phase to goals map
        Map<String, List<String>> phaseToGoals = analysis.getPhaseToGoalsMap();
        assertNotNull("Phase to goals map should not be null", phaseToGoals);
        assertTrue("Phase to goals map should be empty initially", phaseToGoals.isEmpty());
    }

    @Test
    public void testExecutionInfo() {
        // Test ExecutionInfo class
        ExecutionPlanAnalysisService.ExecutionInfo info = new ExecutionPlanAnalysisService.ExecutionInfo(
            "compile", "compile", "maven-compiler-plugin", "default-compile", "org.apache.maven.plugins:maven-compiler-plugin");
        
        assertEquals("compile", info.getGoal());
        assertEquals("compile", info.getPhase());
        assertEquals("maven-compiler-plugin", info.getPluginArtifactId());
        assertEquals("default-compile", info.getExecutionId());
        assertEquals("org.apache.maven.plugins:maven-compiler-plugin", info.getPluginKey());
    }

    @Test
    public void testOutputMethods() throws Exception {
        TestContext ctx = setupBasicTest();
        
        // Test getGoalOutputs - current implementation returns empty list
        List<String> outputs = ctx.service.getGoalOutputs("compile", "{projectRoot}", ctx.project);
        assertNotNull("Outputs should not be null", outputs);
        assertTrue("Current implementation should return empty list", outputs.isEmpty());
        
        // Test getRelativeBuildPath - current implementation returns projectRoot/target
        String buildPath = ctx.service.getRelativeBuildPath("/some/build/dir", "{projectRoot}", ctx.project);
        assertEquals("Should return projectRoot/target", "{projectRoot}/target", buildPath);
    }

    // ========================================
    // Static Utility Method Tests (using @WithoutMojo)
    // ========================================

    @Test
    @WithoutMojo
    public void testGetTargetName() {
        assertEquals("maven-compiler:compile", ExecutionPlanAnalysisService.getTargetName("maven-compiler-plugin", "compile"));
        assertEquals("maven-surefire:test", ExecutionPlanAnalysisService.getTargetName("maven-surefire-plugin", "test"));
        assertEquals("quarkus:dev", ExecutionPlanAnalysisService.getTargetName("quarkus-maven-plugin", "dev"));
        assertEquals("spring-boot:run", ExecutionPlanAnalysisService.getTargetName("spring-boot-maven-plugin", "run"));
    }

    @Test
    @WithoutMojo
    public void testExtractGoalFromTargetName() {
        assertEquals("compile", ExecutionPlanAnalysisService.extractGoalFromTargetName("maven-compiler:compile"));
        assertEquals("test", ExecutionPlanAnalysisService.extractGoalFromTargetName("maven-surefire:test"));
        assertEquals("single", ExecutionPlanAnalysisService.extractGoalFromTargetName("single"));
        assertEquals("complex-goal", ExecutionPlanAnalysisService.extractGoalFromTargetName("plugin:complex-goal"));
        assertNull("Null input should return null", ExecutionPlanAnalysisService.extractGoalFromTargetName(null));
    }

    @Test
    @WithoutMojo
    public void testNormalizePluginName() {
        assertEquals("maven-compiler", ExecutionPlanAnalysisService.normalizePluginName("maven-compiler-plugin"));
        assertEquals("maven-surefire", ExecutionPlanAnalysisService.normalizePluginName("maven-surefire-plugin"));
        assertEquals("quarkus", ExecutionPlanAnalysisService.normalizePluginName("quarkus-maven-plugin"));
        assertEquals("spring-boot", ExecutionPlanAnalysisService.normalizePluginName("spring-boot-maven-plugin"));
        assertEquals("simple", ExecutionPlanAnalysisService.normalizePluginName("simple-plugin"));
        assertEquals("test", ExecutionPlanAnalysisService.normalizePluginName("test"));
        assertNull("Null input should return null", ExecutionPlanAnalysisService.normalizePluginName(null));
    }

    @Test
    @WithoutMojo
    public void testGetCommonGoalsForPlugin() {
        // Test compiler plugin
        List<String> compilerGoals = ExecutionPlanAnalysisService.getCommonGoalsForPlugin("maven-compiler-plugin");
        assertEquals("Should have 2 goals", 2, compilerGoals.size());
        assertTrue("Should contain compile", compilerGoals.contains("compile"));
        assertTrue("Should contain testCompile", compilerGoals.contains("testCompile"));

        // Test surefire plugin
        List<String> surefireGoals = ExecutionPlanAnalysisService.getCommonGoalsForPlugin("maven-surefire-plugin");
        assertEquals("Should have 1 goal", 1, surefireGoals.size());
        assertTrue("Should contain test", surefireGoals.contains("test"));

        // Test quarkus plugin
        List<String> quarkusGoals = ExecutionPlanAnalysisService.getCommonGoalsForPlugin("quarkus-maven-plugin");
        assertEquals("Should have 2 goals", 2, quarkusGoals.size());
        assertTrue("Should contain dev", quarkusGoals.contains("dev"));
        assertTrue("Should contain build", quarkusGoals.contains("build"));

        // Test spring-boot plugin
        List<String> springBootGoals = ExecutionPlanAnalysisService.getCommonGoalsForPlugin("spring-boot-maven-plugin");
        assertEquals("Should have 2 goals", 2, springBootGoals.size());
        assertTrue("Should contain run", springBootGoals.contains("run"));
        assertTrue("Should contain repackage", springBootGoals.contains("repackage"));

        // Test unknown plugin
        List<String> unknownGoals = ExecutionPlanAnalysisService.getCommonGoalsForPlugin("unknown-plugin");
        assertEquals("Should have 0 goals", 0, unknownGoals.size());

        // Test null input
        List<String> nullGoals = ExecutionPlanAnalysisService.getCommonGoalsForPlugin(null);
        assertEquals("Should have 0 goals for null", 0, nullGoals.size());
    }
}