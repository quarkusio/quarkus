package io.quarkus.gradle.devmode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import com.google.common.collect.ImmutableMap;

import io.quarkus.gradle.continuoustesting.ContinuousTestingLogClient;

/**
 * Tests that compile-only dependencies (like Lombok) are available to the compiler
 * during live reload in quarkusTest mode.
 */
public class CompileOnlyContinuousTestingModeTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "compile-only-lombok";
    }

    @Override
    protected String[] buildArguments() {
        return new String[] { "quarkusTest" };
    }

    @Override
    protected void testDevMode() throws Exception {
        // In quarkusTest mode, there's no persistent HTTP endpoint.
        // Use the log-based client to parse test results from the build output.
        File logFile = new File(getProjectDir(), "command-output.log");
        ContinuousTestingLogClient testingClient = new ContinuousTestingLogClient(logFile);

        // Wait for the initial test run to complete
        ContinuousTestingLogClient.TestStatus results = testingClient.waitForNextCompletion();
        assertEquals(1, results.getTestsPassed(),
                "Initial test run should pass, actual results: " + results);
        assertEquals(0, results.getTestsFailed(),
                "Initial test run should have no failures, actual results: " + results);

        // Modify source files that use Lombok annotations.
        // This triggers live reload which requires the Lombok compiler to process annotations.
        // Before the fix for issue #50936, this would fail with
        // "package lombok.extern.slf4j does not exist" in quarkusTest mode.
        replace("src/main/java/io/playground/MyData.java",
                ImmutableMap.of("private String other;", "private final String other;"));
        replace("src/main/java/io/playground/MyDataProducer.java",
                ImmutableMap.of("return new MyData(\"lombok\");", "return new MyData(\"lombok\", \"!\");"));
        replace("src/main/java/io/playground/HelloResource.java",
                ImmutableMap.of("return \"hello \" + myData.getMessage();",
                        "return \"hello \" + myData.getMessage() + myData.getOther();"));

        // Update the test to expect the new response
        replace("src/test/java/io/playground/HelloResourceTest.java",
                ImmutableMap.of(".body(is(\"hello lombok\"));",
                        ".body(is(\"hello lombok!\"));"));

        // Wait for the next test run - this verifies that compile-only dependencies (Lombok)
        // are available to the compiler during live reload.
        // Before the fix, this would fail with "package lombok.extern.slf4j does not exist".
        results = testingClient.waitForNextCompletion();
        assertEquals(1, results.getTestsPassed(),
                "Test run after live reload should pass, actual results: " + results);
        assertEquals(0, results.getTestsFailed(),
                "Test run after live reload should have no failures, actual results: " + results);
    }
}
