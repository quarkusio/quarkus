import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for NxMavenBatchExecutor
 */
public class NxMavenBatchExecutorTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File testProjectDir;
    private File testPomFile;
    private Gson gson = new Gson();

    @Before
    public void setUp() throws Exception {
        testProjectDir = tempFolder.newFolder("test-project");
        testPomFile = new File(testProjectDir, "pom.xml");
        
        // Create a minimal test pom.xml
        String testPom = """
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
            """;
        
        Files.write(testPomFile.toPath(), testPom.getBytes());
    }

    @Test
    public void testMainArgumentParsing() {
        // Test the argument parsing logic without calling main (which calls System.exit)
        // Instead, test the executeBatch method directly with different argument patterns
        
        // Test valid arguments by calling executeBatch directly
        List<String> goals = Arrays.asList("help:help");
        List<String> projects = Arrays.asList(".");
        
        NxMavenBatchExecutor.BatchExecutionResult result = 
            NxMavenBatchExecutor.executeBatch(goals, testProjectDir.getAbsolutePath(), projects, false);
        
        assertNotNull("Result should not be null", result);
        assertNotNull("Goal results should not be null", result.getGoalResults());
    }

    @Test
    public void testInvalidArgumentsHandling() {
        // Test that invalid workspace path is handled gracefully
        List<String> goals = Arrays.asList("help:help");
        List<String> projects = Arrays.asList(".");
        String invalidWorkspace = "/nonexistent/workspace/path";
        
        NxMavenBatchExecutor.BatchExecutionResult result = 
            NxMavenBatchExecutor.executeBatch(goals, invalidWorkspace, projects, false);
        
        assertNotNull("Result should not be null", result);
        assertFalse("Should fail with invalid workspace", result.isOverallSuccess());
        assertNotNull("Should have error message", result.getErrorMessage());
    }

    @Test
    public void testExecuteBatchWithValidGoals() {
        List<String> goals = Arrays.asList("help:help");
        List<String> projects = Arrays.asList(".");
        
        NxMavenBatchExecutor.BatchExecutionResult result = 
            NxMavenBatchExecutor.executeBatch(goals, testProjectDir.getAbsolutePath(), projects, false);
        
        assertNotNull("Result should not be null", result);
        assertNotNull("Goal results should not be null", result.getGoalResults());
        assertFalse("Goal results should not be empty", result.getGoalResults().isEmpty());
        
        // Check that we have at least one goal result
        assertEquals("Should have one goal result", 1, result.getGoalResults().size());
        
        NxMavenBatchExecutor.GoalExecutionResult goalResult = result.getGoalResults().get(0);
        assertNotNull("Goal result should not be null", goalResult);
        assertNotNull("Goal name should not be null", goalResult.getGoal());
    }

    @Test
    public void testExecuteBatchWithInvalidWorkspace() {
        List<String> goals = Arrays.asList("help:help");
        List<String> projects = Arrays.asList(".");
        String invalidWorkspace = "/nonexistent/path";
        
        NxMavenBatchExecutor.BatchExecutionResult result = 
            NxMavenBatchExecutor.executeBatch(goals, invalidWorkspace, projects, false);
        
        assertNotNull("Result should not be null", result);
        assertFalse("Should fail with invalid workspace", result.isOverallSuccess());
        assertNotNull("Should have error message", result.getErrorMessage());
        assertTrue("Error message should mention workspace not found", 
                   result.getErrorMessage().contains("pom.xml not found"));
    }

    @Test
    public void testExecuteBatchWithEmptyGoals() {
        List<String> goals = Arrays.asList();
        List<String> projects = Arrays.asList(".");
        
        NxMavenBatchExecutor.BatchExecutionResult result = 
            NxMavenBatchExecutor.executeBatch(goals, testProjectDir.getAbsolutePath(), projects, false);
        
        assertNotNull("Result should not be null", result);
        // Empty goals might fail because Maven needs at least one goal
        // The behavior should be consistent - either succeed with empty results or fail gracefully
        if (result.isOverallSuccess()) {
            assertTrue("If successful, goal results should be empty", result.getGoalResults().isEmpty());
        } else {
            assertNotNull("If failed, should have error message", result.getErrorMessage());
        }
    }

    @Test
    public void testExecuteBatchWithMultipleGoals() {
        List<String> goals = Arrays.asList("help:help", "help:system");
        List<String> projects = Arrays.asList(".");
        
        NxMavenBatchExecutor.BatchExecutionResult result = 
            NxMavenBatchExecutor.executeBatch(goals, testProjectDir.getAbsolutePath(), projects, false);
        
        assertNotNull("Result should not be null", result);
        assertNotNull("Goal results should not be null", result.getGoalResults());
        
        // Should have one combined goal result (batch execution)
        assertEquals("Should have one batch goal result", 1, result.getGoalResults().size());
        
        NxMavenBatchExecutor.GoalExecutionResult goalResult = result.getGoalResults().get(0);
        assertNotNull("Goal result should not be null", goalResult);
        assertTrue("Goal should contain both goals", goalResult.getGoal().contains("help:help"));
        assertTrue("Goal should contain both goals", goalResult.getGoal().contains("help:system"));
    }

    @Test
    public void testExecuteBatchWithVerboseMode() {
        List<String> goals = Arrays.asList("help:help");
        List<String> projects = Arrays.asList(".");
        
        // Capture output for verbose mode
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            NxMavenBatchExecutor.BatchExecutionResult result = 
                NxMavenBatchExecutor.executeBatch(goals, testProjectDir.getAbsolutePath(), projects, true);
            
            assertNotNull("Result should not be null", result);
            
            String output = outputStream.toString();
            // Verbose mode should produce output
            assertFalse("Verbose mode should produce output", output.trim().isEmpty());
            
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void testBatchExecutionResultJsonSerialization() {
        NxMavenBatchExecutor.BatchExecutionResult result = new NxMavenBatchExecutor.BatchExecutionResult();
        result.setOverallSuccess(true);
        result.setTotalDurationMs(1000);
        result.setErrorMessage("Test error");
        
        NxMavenBatchExecutor.GoalExecutionResult goalResult = new NxMavenBatchExecutor.GoalExecutionResult();
        goalResult.setGoal("test:goal");
        goalResult.setSuccess(true);
        goalResult.setDurationMs(500);
        goalResult.setExitCode(0);
        goalResult.setOutput(Arrays.asList("output line 1", "output line 2"));
        goalResult.setErrors(Arrays.asList("error line 1"));
        
        result.addGoalResult(goalResult);
        
        // Serialize to JSON
        String json = gson.toJson(result);
        assertNotNull("JSON should not be null", json);
        assertTrue("JSON should contain goal", json.contains("test:goal"));
        assertTrue("JSON should contain success", json.contains("true"));
        
        // Deserialize from JSON
        NxMavenBatchExecutor.BatchExecutionResult deserializedResult = 
            gson.fromJson(json, NxMavenBatchExecutor.BatchExecutionResult.class);
        
        assertNotNull("Deserialized result should not be null", deserializedResult);
        assertTrue("Should maintain overall success", deserializedResult.isOverallSuccess());
        assertEquals("Should maintain duration", 1000, deserializedResult.getTotalDurationMs());
        assertEquals("Should maintain error message", "Test error", deserializedResult.getErrorMessage());
        assertEquals("Should have one goal result", 1, deserializedResult.getGoalResults().size());
        
        NxMavenBatchExecutor.GoalExecutionResult deserializedGoalResult = deserializedResult.getGoalResults().get(0);
        assertEquals("Should maintain goal name", "test:goal", deserializedGoalResult.getGoal());
        assertTrue("Should maintain goal success", deserializedGoalResult.isSuccess());
        assertEquals("Should maintain goal duration", 500, deserializedGoalResult.getDurationMs());
        assertEquals("Should maintain exit code", 0, deserializedGoalResult.getExitCode());
        assertEquals("Should maintain output", 2, deserializedGoalResult.getOutput().size());
        assertEquals("Should maintain errors", 1, deserializedGoalResult.getErrors().size());
    }
}