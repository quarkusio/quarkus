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

public class MetricsBridgeSourceSelectionTest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((JavaArchive jar) -> jar
                    .addClasses(BridgeSamplerProbeResource.class)
                    .addAsResource(new StringAsset(
                            "quarkus.dev-ui.observability.metrics.sample-interval=200ms\n"
                                    + "quarkus.otel.metrics.enabled=true\n"
                                    + "quarkus.otel.exporter.otlp.metrics.enabled=false\n"
                                    + "quarkus.otel.traces.enabled=false\n"
                                    + "quarkus.micrometer.export.json.enabled=true\n"),
                            "application.properties"));

    public MetricsBridgeSourceSelectionTest() {
        super("devui-observability");
    }

    @Test
    public void micrometerSamplerSuppressedAndNoDuplicateSeries() throws Exception {
        // Presence matrix: bridge + OTel metrics enabled -> the sampler bean must NOT be registered.
        RestAssured.get("/probe/sampler-present").then().statusCode(200).body(is("false"));

        // Metrics still appear (via the OTel reader). Select everything the catalog knows, then
        // assert every stored series is source="otel" — no Micrometer-sourced duplicate slipped in.
        await().atMost(Duration.ofSeconds(6)).untilAsserted(() -> {
            selectAll();
            JsonNode snap = super.executeJsonRPCMethod("getSnapshot");
            JsonNode sections = snap.get("sections");
            assertThat(sections.isEmpty()).isFalse();
            assertAllSeriesSourcedFrom(sections, "otel");
        });
    }

    private void selectAll() throws Exception {
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
    }

    static void assertAllSeriesSourcedFrom(JsonNode sections, String expectedSource) {
        for (JsonNode section : sections) {
            for (JsonNode series : section.get("series")) {
                assertThat(series.get("source").asText()).isEqualTo(expectedSource);
            }
        }
    }
}
