package io.quarkus.micrometer.deployment.devui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import tools.jackson.databind.JsonNode;

/**
 * Selects a metric, accumulates points, then triggers an app-code reload and asserts the
 * captured history survives (the store holder lives in the reused base classloader).
 */
public class MetricsReloadTest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((JavaArchive jar) -> jar
                    .addClasses(MetricsResource.class, ReloadMarker.class)
                    .addAsResource(new StringAsset(
                            "quarkus.dev-ui.observability.metrics.sample-interval=200ms\n"
                                    + "quarkus.micrometer.export.json.enabled=true\n"),
                            "application.properties"));

    public MetricsReloadTest() {
        super("devui-observability");
    }

    @Test
    public void historySurvivesAppCodeReload() throws Exception {
        RestAssured.get("/metrics-test/hit").then().statusCode(200);
        super.executeJsonRPCMethod("setSelection", java.util.Map.of("names", List.of("demo.hits")));

        // Accumulate at least a couple of points.
        AtomicInteger before = new AtomicInteger();
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            RestAssured.get("/metrics-test/hit");
            int n = pointCount(super.executeJsonRPCMethod("getSnapshot"), "demo.hits");
            before.set(n);
            assertThat(n).isGreaterThanOrEqualTo(2);
        });

        // Force an app-code reload by editing a source file.
        test.modifySourceFile(ReloadMarker.class, s -> s.replace("\"v1\"", "\"v2\""));

        // After the reload the selection and history are still there (store holder survived),
        // and capture resumes into the same series — the point count never resets to 0.
        await().atMost(Duration.ofSeconds(6)).untilAsserted(() -> {
            RestAssured.get("/metrics-test/hit");
            int n = pointCount(super.executeJsonRPCMethod("getSnapshot"), "demo.hits");
            assertThat(n).isGreaterThanOrEqualTo(before.get());
        });
    }

    private static int pointCount(JsonNode snapshot, String name) {
        for (JsonNode s : snapshot.get("sections")) {
            if (name.equals(s.get("name").asText())) {
                return s.get("series").get(0).get("points").size();
            }
        }
        return 0;
    }
}
