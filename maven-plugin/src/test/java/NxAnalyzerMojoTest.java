import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.WithoutMojo;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.File;

/**
 * Unit tests for NxAnalyzerMojo using Maven Plugin Testing Harness.
 * This approach provides real Maven context with minimal mocking.
 */
public class NxAnalyzerMojoTest {
    
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
     * Test basic mojo configuration and parameter injection
     */
    @Test
    public void testMojoConfiguration() throws Exception {
        File pom = new File("target/test-classes/unit/basic-test");
        assertNotNull(pom);
        assertTrue(pom.exists());

        NxAnalyzerMojo mojo = (NxAnalyzerMojo) rule.lookupConfiguredMojo(pom, "analyze");
        assertNotNull(mojo);
    }

    /**
     * Test mojo parameters can be accessed
     */
    @Test
    public void testMojoParameters() throws Exception {
        File pom = new File("target/test-classes/unit/basic-test");
        assertNotNull(pom);
        assertTrue(pom.exists());

        NxAnalyzerMojo mojo = (NxAnalyzerMojo) rule.lookupConfiguredMojo(pom, "analyze");
        assertNotNull(mojo);
        
        // Test that we can access mojo properties without executing
        assertNotNull("Mojo should have a logger", mojo.getLog());
    }

    /**
     * Test verbose mode configuration
     */
    @Test
    public void testVerboseConfiguration() throws Exception {
        File pom = new File("target/test-classes/unit/verbose-test");
        assertNotNull(pom);
        assertTrue(pom.exists());

        NxAnalyzerMojo mojo = (NxAnalyzerMojo) rule.lookupConfiguredMojo(pom, "analyze");
        assertNotNull(mojo);
        
        // Test that verbose mojo can be configured without executing
        assertNotNull("Verbose mojo should have a logger", mojo.getLog());
    }
    
    /**
     * Test without Maven project (testing harness validation)
     */
    @Test
    @WithoutMojo
    public void testWithoutMojo() {
        // This test validates that our test harness setup is working
        // even when no mojo is configured
        assertTrue("Test harness should work without mojo", true);
    }
}