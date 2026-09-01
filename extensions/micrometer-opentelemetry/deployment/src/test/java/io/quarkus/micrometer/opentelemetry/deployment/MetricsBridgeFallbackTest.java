package io.quarkus.micrometer.opentelemetry.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import tools.jackson.databind.JsonNode;

public class MetricsBridgeFallbackTest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((JavaArchive jar) -> jar
                    .addClasses(BridgeSamplerProbeResource.class)
                    .addAsResource(new StringAsset(
                            "quarkus.dev-ui.observability.metrics.sample-interval=200ms\n"
                                    + "quarkus.otel.metrics.enabled=false\n"
                                    + "quarkus.micrometer.export.json.enabled=true\n"),
                            "application.properties"));

    public MetricsBridgeFallbackTest() {
        super("devui-observability");
    }

    @Test
    public void samplerRunsAsFallbackWhenOtelMetricsDisabled() throws Exception {
        // OTel metrics disabled -> no OTel reader -> the sampler must run.
        RestAssured.get("/probe/sampler-present").then().statusCode(200).body(is("true"));

        // Metrics still appear, all sourced from micrometer (the fallback path).
        await().atMost(Duration.ofSeconds(6)).untilAsserted(() -> {
            JsonNode catalog = super.executeJsonRPCMethod("getCatalog");
            List<String> names = new ArrayList<>();
            JsonNode groups = catalog.get("groups");
            if (groups != null) {
                for (JsonNode group : groups) {
                    for (JsonNode metric : group.get("metrics")) {
                        names.add(metric.get("name").asText());
                    }
                }
            }
            super.executeJsonRPCMethod("setSelection", java.util.Map.of("names", names));

            JsonNode snap = super.executeJsonRPCMethod("getSnapshot");
            JsonNode sections = snap.get("sections");
            assertThat(sections.isEmpty()).isFalse();
            MetricsBridgeSourceSelectionTest.assertAllSeriesSourcedFrom(sections, "micrometer");
        });
    }
}
