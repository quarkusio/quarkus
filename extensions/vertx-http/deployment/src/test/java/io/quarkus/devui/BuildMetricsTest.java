package io.quarkus.devui;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;

public class BuildMetricsTest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest().withEmptyApplication();

    public BuildMetricsTest() {
        super("devui-build-metrics");
    }

    @Test
    public void testGetBuildStepsMetrics() throws Exception {
        JsonNode buildStepsMetricsResponse = super.executeJsonRPCMethod("getBuildMetrics");
        Assertions.assertNotNull(buildStepsMetricsResponse);
        int duration = buildStepsMetricsResponse.get("duration").asInt();
        Assertions.assertTrue(duration > 0);

        boolean recordsIncluded = buildStepsMetricsResponse.get("records").isArray();
        Assertions.assertTrue(recordsIncluded);

    }
}
