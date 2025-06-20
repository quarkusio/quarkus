import com.google.gson.Gson
import org.junit.Before
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Files
import kotlin.test.*

/**
 * Unit tests for NxMavenBatchExecutor
 */
class NxMavenBatchExecutorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var testProjectDir: File
    private lateinit var testPomFile: File
    private val gson = Gson()

    @Before
    fun setUp() {
        testProjectDir = tempFolder.newFolder("test-project")
        testPomFile = File(testProjectDir, "pom.xml")
        
        // Create a minimal test pom.xml
        val testPom = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                     http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.test</groupId>
                <artifactId>test-project</artifactId>
                <version>1.0.0</version>
                <packaging>jar</packaging>
                
                <properties>
                    <maven.compiler.source>21</maven.compiler.source>
                    <maven.compiler.target>21</maven.compiler.target>
                </properties>
            </project>
            """.trimIndent()
        
        Files.write(testPomFile.toPath(), testPom.toByteArray())
    }

    @Test
    fun testMainArgumentParsing() {
        // Test the argument parsing logic without calling main (which calls System.exit)
        // Instead, test the executeBatch method directly with different argument patterns
        
        // Test valid arguments by calling executeBatch directly
        val goals = listOf("help:help")
        val projects = listOf(".")
        
        val result = NxMavenBatchExecutor.executeBatch(goals, testProjectDir.absolutePath, projects, false)
        
        assertNotNull(result, "Result should not be null")
        assertNotNull(result.getGoalResults(), "Goal results should not be null")
    }

    @Test
    fun testInvalidArgumentsHandling() {
        // Test that invalid workspace path is handled gracefully
        val goals = listOf("help:help")
        val projects = listOf(".")
        val invalidWorkspace = "/nonexistent/workspace/path"
        
        val result = NxMavenBatchExecutor.executeBatch(goals, invalidWorkspace, projects, false)
        
        assertNotNull(result, "Result should not be null")
        assertFalse(result.isOverallSuccess(), "Should fail with invalid workspace")
        assertNotNull(result.getErrorMessage(), "Should have error message")
    }

    @Test
    fun testExecuteBatchWithValidGoals() {
        val goals = listOf("help:help")
        val projects = listOf(".")
        
        val result = NxMavenBatchExecutor.executeBatch(goals, testProjectDir.absolutePath, projects, false)
        
        assertNotNull(result, "Result should not be null")
        assertNotNull(result.getGoalResults(), "Goal results should not be null")
        assertFalse(result.getGoalResults().isEmpty(), "Goal results should not be empty")
        
        // Check that we have at least one goal result
        assertEquals(1, result.getGoalResults().size, "Should have one goal result")
        
        val goalResult = result.getGoalResults()[0]
        assertNotNull(goalResult, "Goal result should not be null")
        assertNotNull(goalResult.getGoal(), "Goal name should not be null")
    }

    @Test
    fun testExecuteBatchWithInvalidWorkspace() {
        val goals = listOf("help:help")
        val projects = listOf(".")
        val invalidWorkspace = "/nonexistent/path"
        
        val result = NxMavenBatchExecutor.executeBatch(goals, invalidWorkspace, projects, false)
        
        assertNotNull(result, "Result should not be null")
        assertFalse(result.isOverallSuccess(), "Should fail with invalid workspace")
        assertNotNull(result.getErrorMessage(), "Should have error message")
        assertTrue(result.getErrorMessage()!!.contains("pom.xml not found"), 
                  "Error message should mention workspace not found")
    }

    @Test
    fun testExecuteBatchWithEmptyGoals() {
        val goals = emptyList<String>()
        val projects = listOf(".")
        
        val result = NxMavenBatchExecutor.executeBatch(goals, testProjectDir.absolutePath, projects, false)
        
        assertNotNull(result, "Result should not be null")
        // Empty goals might fail because Maven needs at least one goal
        // The behavior should be consistent - either succeed with empty results or fail gracefully
        if (result.isOverallSuccess()) {
            assertTrue(result.getGoalResults().isEmpty(), "If successful, goal results should be empty")
        } else {
            assertNotNull(result.getErrorMessage(), "If failed, should have error message")
        }
    }

    @Test
    fun testExecuteBatchWithMultipleGoals() {
        val goals = listOf("help:help", "help:system")
        val projects = listOf(".")
        
        val result = NxMavenBatchExecutor.executeBatch(goals, testProjectDir.absolutePath, projects, false)
        
        assertNotNull(result, "Result should not be null")
        assertNotNull(result.getGoalResults(), "Goal results should not be null")
        
        // Should have one combined goal result (batch execution)
        assertEquals(1, result.getGoalResults().size, "Should have one batch goal result")
        
        val goalResult = result.getGoalResults()[0]
        assertNotNull(goalResult, "Goal result should not be null")
        assertTrue(goalResult.getGoal()!!.contains("help:help"), "Goal should contain both goals")
        assertTrue(goalResult.getGoal()!!.contains("help:system"), "Goal should contain both goals")
    }

    @Test
    fun testExecuteBatchWithVerboseMode() {
        val goals = listOf("help:help")
        val projects = listOf(".")
        
        // Capture output for verbose mode
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))
        
        try {
            val result = NxMavenBatchExecutor.executeBatch(goals, testProjectDir.absolutePath, projects, true)
            
            assertNotNull(result, "Result should not be null")
            
            val output = outputStream.toString()
            // Verbose mode should produce output
            assertFalse(output.trim().isEmpty(), "Verbose mode should produce output")
            
        } finally {
            System.setOut(originalOut)
        }
    }

    @Test
    fun testBatchExecutionResultJsonSerialization() {
        val result = NxMavenBatchExecutor.BatchExecutionResult()
        result.setOverallSuccess(true)
        result.setTotalDurationMs(1000)
        result.setErrorMessage("Test error")
        
        val goalResult = NxMavenBatchExecutor.GoalExecutionResult()
        goalResult.setGoal("test:goal")
        goalResult.setSuccess(true)
        goalResult.setDurationMs(500)
        goalResult.setExitCode(0)
        goalResult.setOutput(listOf("output line 1", "output line 2"))
        goalResult.setErrors(listOf("error line 1"))
        
        result.addGoalResult(goalResult)
        
        // Serialize to JSON
        val json = gson.toJson(result)
        assertNotNull(json, "JSON should not be null")
        assertTrue(json.contains("test:goal"), "JSON should contain goal")
        assertTrue(json.contains("true"), "JSON should contain success")
        
        // Deserialize from JSON
        val deserializedResult = gson.fromJson(json, NxMavenBatchExecutor.BatchExecutionResult::class.java)
        
        assertNotNull(deserializedResult, "Deserialized result should not be null")
        assertTrue(deserializedResult.isOverallSuccess(), "Should maintain overall success")
        assertEquals(1000L, deserializedResult.getTotalDurationMs(), "Should maintain duration")
        assertEquals("Test error", deserializedResult.getErrorMessage(), "Should maintain error message")
        assertEquals(1, deserializedResult.getGoalResults().size, "Should have one goal result")
        
        val deserializedGoalResult = deserializedResult.getGoalResults()[0]
        assertEquals("test:goal", deserializedGoalResult.getGoal(), "Should maintain goal name")
        assertTrue(deserializedGoalResult.isSuccess(), "Should maintain goal success")
        assertEquals(500L, deserializedGoalResult.getDurationMs(), "Should maintain goal duration")
        assertEquals(0, deserializedGoalResult.getExitCode(), "Should maintain exit code")
        assertEquals(2, deserializedGoalResult.getOutput().size, "Should maintain output")
        assertEquals(1, deserializedGoalResult.getErrors().size, "Should maintain errors")
    }
}