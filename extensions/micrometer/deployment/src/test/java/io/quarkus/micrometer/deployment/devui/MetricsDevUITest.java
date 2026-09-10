package io.quarkus.micrometer.deployment.devui;

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

/**
 * Verifies selective capture end-to-end: the catalog lists available metrics, nothing is
 * buffered until selected, and once selected the metric accumulates points.
 */
public class MetricsDevUITest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((JavaArchive jar) -> jar
                    .addClasses(MetricsResource.class)
                    .addAsResource(new StringAsset(
                            "quarkus.dev-ui.observability.metrics.sample-interval=200ms\n"
                                    + "quarkus.micrometer.export.json.enabled=true\n"),
                            "application.properties"));

    public MetricsDevUITest() {
        super("devui-observability"); // JSON-RPC namespace the metrics service is registered under
    }

    @Test
    public void catalogListsMetricsAndSelectionDrivesCapture() throws Exception {
        // Generate some meters.
        RestAssured.get("/metrics-test/hit").then().statusCode(200);

        // Catalog eventually lists demo.hits (poll; sampler runs every 200ms).
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            JsonNode catalog = super.executeJsonRPCMethod("getCatalog");
            assertThat(hasMetric(catalog, "demo.hits")).isTrue();
        });

        // Nothing selected yet -> snapshot empty.
        JsonNode empty = super.executeJsonRPCMethod("getSnapshot");
        assertThat(empty.get("sections").isEmpty()).isTrue();

        // Select demo.hits, keep hitting the endpoint, expect points to accumulate with the
        // correct metadata: the design requires asserting the cumulative flag and tag set,
        // and at least two points (so the client has something to derive a rate from).
        super.executeJsonRPCMethod("setSelection", Map.of("names", List.of("demo.hits")));
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            RestAssured.get("/metrics-test/hit");
            JsonNode snap = super.executeJsonRPCMethod("getSnapshot");
            JsonNode sections = snap.get("sections");
            assertThat(sections).isNotNull();
            assertThat(sections.isEmpty()).isFalse();
            JsonNode section = sections.get(0);
            assertThat(section.get("name").asText()).isEqualTo("demo.hits");

            JsonNode series = section.get("series").get(0);
            // A counter is monotonic -> cumulative=true (drives the client-side rate).
            assertThat(series.get("cumulative").asBoolean()).isTrue();
            assertThat(series.get("source").asText()).isEqualTo("micrometer");
            // Tag set survives capture.
            assertThat(series.get("tags").get("endpoint").asText()).isEqualTo("hit");
            // At least two points accumulated.
            assertThat(series.get("points").size()).isGreaterThanOrEqualTo(2);
        });
    }

    private static boolean hasMetric(JsonNode catalog, String name) {
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
