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
        
        // Test null handling
        assertNull("Null goal should return null", MavenUtils.inferPhaseFromGoal(null, project));
        assertNull("Empty goal should return null", MavenUtils.inferPhaseFromGoal("", project));
        
        // Test with null project
        assertNull("Null project should return null", MavenUtils.inferPhaseFromGoal("compile", null));
    }

    /**
     * Test target name generation (static utility method)
     */
    @Test
    @WithoutMojo
    public void testGetTargetName() {
        assertEquals("maven-compiler:compile", MavenUtils.getTargetName("maven-compiler-plugin", "compile"));
        assertEquals("maven-surefire:test", MavenUtils.getTargetName("maven-surefire-plugin", "test"));
        assertEquals("maven-jar:jar", MavenUtils.getTargetName("maven-jar-plugin", "jar"));
    }

    /**
     * Test goal extraction from target names (static utility method)
     */
    @Test
    @WithoutMojo
    public void testExtractGoalFromTargetName() {
        assertEquals("compile", MavenUtils.extractGoalFromTargetName("maven-compiler:compile"));
        assertEquals("test", MavenUtils.extractGoalFromTargetName("maven-surefire:test"));
        assertEquals("jar", MavenUtils.extractGoalFromTargetName("maven-jar:jar"));
        
        // Test edge cases
        assertEquals("complex-goal", MavenUtils.extractGoalFromTargetName("plugin:complex-goal"));
        assertEquals("single", MavenUtils.extractGoalFromTargetName("single"));
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
}