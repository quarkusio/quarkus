import org.apache.maven.plugin.testing.MojoRule
import org.apache.maven.plugin.testing.WithoutMojo
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.test.*

/**
 * Unit tests for NxAnalyzerMojo using Maven Plugin Testing Harness.
 * This approach provides real Maven context with minimal mocking.
 */
class NxAnalyzerMojoTest {
    
    @get:Rule
    val rule = MojoRule()

    /**
     * Test basic mojo configuration and parameter injection
     */
    @Test
    fun testMojoConfiguration() {
        val pom = File("target/test-classes/unit/basic-test")
        assertNotNull(pom)
        assertTrue(pom.exists())

        val mojo = rule.lookupConfiguredMojo(pom, "analyze") as NxAnalyzerMojo
        assertNotNull(mojo)
    }

    /**
     * Test mojo parameters can be accessed
     */
    @Test
    fun testMojoParameters() {
        val pom = File("target/test-classes/unit/basic-test")
        assertNotNull(pom)
        assertTrue(pom.exists())

        val mojo = rule.lookupConfiguredMojo(pom, "analyze") as NxAnalyzerMojo
        assertNotNull(mojo)
        
        // Test that we can access mojo properties without executing
        assertNotNull(mojo.log, "Mojo should have a logger")
    }

    /**
     * Test verbose mode configuration
     */
    @Test
    fun testVerboseConfiguration() {
        val pom = File("target/test-classes/unit/verbose-test")
        assertNotNull(pom)
        assertTrue(pom.exists())

        val mojo = rule.lookupConfiguredMojo(pom, "analyze") as NxAnalyzerMojo
        assertNotNull(mojo)
        
        // Test that verbose mojo can be configured without executing
        assertNotNull(mojo.log, "Verbose mojo should have a logger")
    }
    

    /**
     * Test without Maven project (testing harness validation)
     */
    @Test
    @WithoutMojo
    fun testWithoutMojo() {
        // This test validates that our test harness setup is working
        // even when no mojo is configured
        assertTrue(true, "Test harness should work without mojo")
    }
}