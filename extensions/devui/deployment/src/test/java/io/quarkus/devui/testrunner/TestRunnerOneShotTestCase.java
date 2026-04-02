package io.quarkus.devui.testrunner;

import java.util.Map;
import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;

public class TestRunnerOneShotTestCase extends DevUIJsonRPCTest {

    @RegisterExtension
    static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClasses(HelloResource.class, UnitService.class)
                            .add(new StringAsset(
                                    "quarkus.test.continuous-testing=disabled\nquarkus.console.basic=true\nquarkus.console.disable-input=true\n"),
                                    "application.properties");
                }
            })
            .setTestArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class).addClasses(SimpleET.class, UnitET.class);
                }
            });

    public TestRunnerOneShotTestCase() {
        super("devui-testing");
    }

    @Test
    public void testRunTests() throws Exception {
        JsonNode result = super.executeJsonRPCMethod("runTests");
        Assertions.assertNotNull(result, "runTests should return a result");

        Assertions.assertTrue(result.has("passedCount"), "Result should have passedCount");
        Assertions.assertTrue(result.has("failedCount"), "Result should have failedCount");
        Assertions.assertTrue(result.has("skippedCount"), "Result should have skippedCount");
        Assertions.assertTrue(result.has("failing"), "Result should have failing list");
        Assertions.assertTrue(result.has("passing"), "Result should have passing list");
        Assertions.assertTrue(result.has("skipped"), "Result should have skipped list");

        // The tests include SimpleET (2 tests) and UnitET (2 tests)
        // SimpleET.testHelloEndpoint will fail (no /hello route initially)
        // SimpleET.testGreetingEndpoint will pass
        // UnitET tests will fail (expects "UNIT" but gets "unit", expects "Hi" but gets "hello")
        long totalTests = result.get("passedCount").asLong() + result.get("failedCount").asLong()
                + result.get("skippedCount").asLong();
        Assertions.assertTrue(totalTests > 0, "Should have run at least one test");

        // Verify the result has timing info
        Assertions.assertTrue(result.has("startedTime"), "Result should have startedTime");
        Assertions.assertTrue(result.has("completedTime"), "Result should have completedTime");
        Assertions.assertTrue(result.has("totalTime"), "Result should have totalTime");
        Assertions.assertTrue(result.get("totalTime").asLong() > 0, "Total time should be positive");
    }

    @Test
    public void testRunTestWithClassName() throws Exception {
        JsonNode result = super.executeJsonRPCMethod("runTest",
                Map.of("className", "io.quarkus.devui.testrunner.UnitET"));
        Assertions.assertNotNull(result, "runTest should return a result");

        // Should only contain results for UnitET
        long totalTests = result.get("passedCount").asLong() + result.get("failedCount").asLong()
                + result.get("skippedCount").asLong();
        Assertions.assertTrue(totalTests > 0, "Should have run at least one test");

        // All results should be from UnitET
        assertAllResultsFromClass(result.get("failing"), "io.quarkus.devui.testrunner.UnitET");
        assertAllResultsFromClass(result.get("passing"), "io.quarkus.devui.testrunner.UnitET");
        assertAllResultsFromClass(result.get("skipped"), "io.quarkus.devui.testrunner.UnitET");
    }

    @Test
    public void testRunTestWithMethodName() throws Exception {
        JsonNode result = super.executeJsonRPCMethod("runTest",
                Map.of("className", "io.quarkus.devui.testrunner.SimpleET",
                        "methodName", "testGreetingEndpoint"));
        Assertions.assertNotNull(result, "runTest should return a result");

        // Should only have run one test method
        long totalTests = result.get("passedCount").asLong() + result.get("failedCount").asLong()
                + result.get("skippedCount").asLong();
        Assertions.assertEquals(1, totalTests, "Should have run exactly one test");
    }

    @Test
    public void testRunAffectedTests() throws Exception {
        JsonNode result = super.executeJsonRPCMethod("runAffectedTests");
        Assertions.assertNotNull(result, "runAffectedTests should return a result");

        Assertions.assertTrue(result.has("usageDataAvailable"), "Result should have usageDataAvailable");
        Assertions.assertTrue(result.has("passedCount"), "Result should have passedCount");

        long totalTests = result.get("passedCount").asLong() + result.get("failedCount").asLong()
                + result.get("skippedCount").asLong();
        Assertions.assertTrue(totalTests > 0, "Should have run at least one test");
    }

    private void assertAllResultsFromClass(JsonNode results, String expectedClassName) {
        if (results != null && results.isArray()) {
            for (JsonNode testResult : results) {
                Assertions.assertEquals(expectedClassName, testResult.get("className").asText(),
                        "Test result should be from " + expectedClassName);
            }
        }
    }
}
