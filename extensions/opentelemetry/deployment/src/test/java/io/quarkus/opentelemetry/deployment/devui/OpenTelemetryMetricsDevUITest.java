package io.quarkus.opentelemetry.deployment.devui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import tools.jackson.databind.JsonNode;

public class OpenTelemetryMetricsDevUITest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((JavaArchive jar) -> jar
                    .addClasses(CustomMetricResource.class)
                    .addAsResource(new StringAsset(
                            "quarkus.dev-ui.observability.metrics.sample-interval=200ms\n"
                                    + "quarkus.otel.metrics.enabled=true\n"
                                    + "quarkus.otel.traces.enabled=false\n"),
                            "application.properties"));

    public OpenTelemetryMetricsDevUITest() {
        super("devui-observability");
    }

    @Test
    public void nativeOtelCounterIsCapturedWithCumulativeTemporality() throws Exception {
        RestAssured.get("/otel-metrics-test/hit").then().statusCode(200);

        // Both a counter and an observable gauge surface in the catalog.
        await().atMost(Duration.ofSeconds(6)).untilAsserted(() -> {
            JsonNode catalog = super.executeJsonRPCMethod("getCatalog");
            assertThat(catalogHas(catalog, "custom.otel.hits")).isTrue();
            assertThat(catalogHas(catalog, "custom.otel.gauge")).isTrue();
        });

        super.executeJsonRPCMethod("setSelection",
                Map.of("names", List.of("custom.otel.hits", "custom.otel.gauge")));

        // Drive several increments across multiple export ticks so we capture ≥3 points.
        await().atMost(Duration.ofSeconds(8)).untilAsserted(() -> {
            RestAssured.get("/otel-metrics-test/hit");
            JsonNode snap = super.executeJsonRPCMethod("getSnapshot");
            JsonNode counter = section(snap, "custom.otel.hits");
            assertThat(counter).isNotNull();
            JsonNode counterPoints = counter.get("series").get(0).get("points");
            assertThat(counterPoints.size()).isGreaterThanOrEqualTo(3);
        });

        // LOAD-BEARING: the dev exporter pins CUMULATIVE temporality, so stored counter
        // values must be running totals (monotonically non-decreasing), NOT per-tick deltas.
        // If the exporter regressed to DELTA, consecutive values would hover around a small
        // constant instead of climbing, and the client would then difference an
        // already-differenced series -> wrong rates. This assertion guards that pin.
        JsonNode snap = super.executeJsonRPCMethod("getSnapshot");
        JsonNode counter = section(snap, "custom.otel.hits");
        JsonNode counterSeries = counter.get("series").get(0);
        assertThat(counterSeries.get("cumulative").asBoolean()).isTrue();
        assertThat(counterSeries.get("source").asText()).isEqualTo("otel");
        JsonNode points = counterSeries.get("points");
        double prev = Double.NEGATIVE_INFINITY;
        for (JsonNode point : points) {
            double v = point.get(1).asDouble();
            assertThat(v).isGreaterThanOrEqualTo(prev); // running total, never decreasing
            prev = v;
        }
        assertThat(prev).isGreaterThan(0.0); // actually climbed

        // The observable gauge is captured and flagged non-cumulative (rendered as-is).
        JsonNode gauge = section(snap, "custom.otel.gauge");
        assertThat(gauge).isNotNull();
        assertThat(gauge.get("series").get(0).get("cumulative").asBoolean()).isFalse();
    }

    private static JsonNode section(JsonNode snapshot, String name) {
        for (JsonNode s : snapshot.get("sections")) {
            if (name.equals(s.get("name").asText())) {
                return s;
            }
        }
        return null;
    }

    private static boolean catalogHas(JsonNode catalog, String name) {
        JsonNode groups = catalog.get("groups");
        if (groups == null) {
            return false;
        }
        for (JsonNode group : groups) {
            for (JsonNode metric : group.get("metrics")) {
                if (name.equals(metric.get("name").asText())) {
                    return true;
                }
            }
        }
        return false;
    }
}
