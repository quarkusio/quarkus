package io.quarkus.devui;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.tests.DevUITest;
import io.quarkus.devui.tests.JsonRPCServiceClient;
import io.quarkus.devui.tests.Namespace;
import io.quarkus.test.QuarkusDevModeTest;

@DevUITest(@Namespace("devui-build-metrics"))
public class BuildMetricsTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withEmptyApplication();

    @Test
    public void testGetBuildStepsMetrics(JsonRPCServiceClient client) throws Exception {
        JsonNode buildStepsMetricsResponse = client
                .request("getBuildMetrics")
                .send()
                .get(5, TimeUnit.SECONDS);
        Assertions.assertNotNull(buildStepsMetricsResponse);
        int duration = buildStepsMetricsResponse.get("duration").asInt();
        Assertions.assertTrue(duration > 0);

        boolean recordsIncluded = buildStepsMetricsResponse.get("records").isArray();
        Assertions.assertTrue(recordsIncluded);

    }
}
