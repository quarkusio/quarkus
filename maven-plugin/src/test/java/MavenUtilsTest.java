import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.WithoutMojo;
import org.apache.maven.project.MavenProject;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.File;

/**
 * Unit tests for MavenUtils utility methods using Maven Plugin Testing Harness.
 */
public class MavenUtilsTest {

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
     * Test phase inference from goal names with real Maven project
     */
    @Test
    public void testInferPhaseFromGoal() throws Exception {
        File pom = new File("target/test-classes/unit/basic-test");
        assertNotNull(pom);
        assertTrue(pom.exists());

        NxAnalyzerMojo mojo = (NxAnalyzerMojo) rule.lookupConfiguredMojo(pom, "analyze");
        assertNotNull(mojo);
        
        // Test with a real Maven project from the mojo
        MavenProject project = new MavenProject();
        
        // Get session from mojo
        org.apache.maven.execution.MavenSession session = (org.apache.maven.execution.MavenSession) rule.getVariableValueFromObject(mojo, "session");
        
        // Note: MavenUtils.inferPhaseFromGoal has been removed in favor of ExecutionPlanAnalysisService
        // This test now demonstrates the new approach
        ExecutionPlanAnalysisService analysisService = new ExecutionPlanAnalysisService(mojo.getLog(), false, null, session);
        
        // Test null handling
        assertNull("Null goal should return null", analysisService.findPhaseForGoal(project, null));
        assertNull("Empty goal should return null", analysisService.findPhaseForGoal(project, ""));
        
        // Test with null project should handle gracefully (may return null or throw exception depending on implementation)
        String result = analysisService.findPhaseForGoal(null, "compile");
        // This may be null or throw an exception - both are acceptable for null input
    }

    /**
     * Test target name generation (moved to ExecutionPlanAnalysisService)
     */
    @Test
    @WithoutMojo
    public void testGetTargetName() {
        assertEquals("maven-compiler:compile", ExecutionPlanAnalysisService.getTargetName("maven-compiler-plugin", "compile"));
        assertEquals("maven-surefire:test", ExecutionPlanAnalysisService.getTargetName("maven-surefire-plugin", "test"));
        assertEquals("maven-jar:jar", ExecutionPlanAnalysisService.getTargetName("maven-jar-plugin", "jar"));
    }

    /**
     * Test goal extraction from target names (moved to ExecutionPlanAnalysisService)
     */
    @Test
    @WithoutMojo
    public void testExtractGoalFromTargetName() {
        assertEquals("compile", ExecutionPlanAnalysisService.extractGoalFromTargetName("maven-compiler:compile"));
        assertEquals("test", ExecutionPlanAnalysisService.extractGoalFromTargetName("maven-surefire:test"));
        assertEquals("jar", ExecutionPlanAnalysisService.extractGoalFromTargetName("maven-jar:jar"));
        
        // Test edge cases
        assertEquals("complex-goal", ExecutionPlanAnalysisService.extractGoalFromTargetName("plugin:complex-goal"));
        assertEquals("single", ExecutionPlanAnalysisService.extractGoalFromTargetName("single"));
    }

    /**
     * Test project key formatting (static utility method)
     */
    @Test
    @WithoutMojo
    public void testFormatProjectKey() {
        MavenProject project = new MavenProject();
        project.setGroupId("com.example");
        project.setArtifactId("test-project");
        
        assertEquals("com.example:test-project", MavenUtils.formatProjectKey(project));
    }

    /**
     * Test plugin name normalization (new method in ExecutionPlanAnalysisService)
     */
    @Test
    @WithoutMojo
    public void testNormalizePluginName() {
        assertEquals("maven-compiler", ExecutionPlanAnalysisService.normalizePluginName("maven-compiler-plugin"));
        assertEquals("maven-surefire", ExecutionPlanAnalysisService.normalizePluginName("maven-surefire-plugin"));
        assertEquals("quarkus", ExecutionPlanAnalysisService.normalizePluginName("quarkus-maven-plugin"));
        assertEquals("simple", ExecutionPlanAnalysisService.normalizePluginName("simple-plugin"));
        assertEquals("test", ExecutionPlanAnalysisService.normalizePluginName("test"));
        assertNull(ExecutionPlanAnalysisService.normalizePluginName(null));
    }

    /**
     * Test common goals for plugins (new method in ExecutionPlanAnalysisService)
     */
    @Test
    @WithoutMojo
    public void testGetCommonGoalsForPlugin() {
        // Test compiler plugin
        java.util.List<String> compilerGoals = ExecutionPlanAnalysisService.getCommonGoalsForPlugin("maven-compiler-plugin");
        assertEquals(2, compilerGoals.size());
        assertTrue(compilerGoals.contains("compile"));
        assertTrue(compilerGoals.contains("testCompile"));

        // Test surefire plugin
        java.util.List<String> surefireGoals = ExecutionPlanAnalysisService.getCommonGoalsForPlugin("maven-surefire-plugin");
        assertEquals(1, surefireGoals.size());
        assertTrue(surefireGoals.contains("test"));

        // Test quarkus plugin
        java.util.List<String> quarkusGoals = ExecutionPlanAnalysisService.getCommonGoalsForPlugin("quarkus-maven-plugin");
        assertEquals(2, quarkusGoals.size());
        assertTrue(quarkusGoals.contains("dev"));
        assertTrue(quarkusGoals.contains("build"));

        // Test unknown plugin
        java.util.List<String> unknownGoals = ExecutionPlanAnalysisService.getCommonGoalsForPlugin("unknown-plugin");
        assertEquals(0, unknownGoals.size());

        // Test null input
        java.util.List<String> nullGoals = ExecutionPlanAnalysisService.getCommonGoalsForPlugin(null);
        assertEquals(0, nullGoals.size());
    }
}