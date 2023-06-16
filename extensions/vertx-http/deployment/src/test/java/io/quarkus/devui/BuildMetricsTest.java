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

    @Test
    public void testGetBuildStepsMetrics() throws Exception {
        JsonNode buildStepsMetricsResponse = super.executeJsonRPCMethod("getBuildStepsMetrics");
        Assertions.assertNotNull(buildStepsMetricsResponse);

        int duration = buildStepsMetricsResponse.get("duration").asInt();
        Assertions.assertTrue(duration > 0);

        boolean dependencyGraphsIncluded = buildStepsMetricsResponse.get("dependencyGraphs").isObject();
        Assertions.assertTrue(dependencyGraphsIncluded);

    }

    @Override
    protected String getNamespace() {
        return "devui-build-metrics";
    }
}
