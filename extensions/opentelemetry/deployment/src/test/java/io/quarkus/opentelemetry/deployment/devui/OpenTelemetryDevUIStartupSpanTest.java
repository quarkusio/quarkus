package io.quarkus.opentelemetry.deployment.devui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;
import tools.jackson.databind.JsonNode;

/**
 * A span that ends while the application is still starting reaches the traces view. Spans are sent as telemetry
 * events, and a sender only builds one once events are enabled, so they have to be enabled before any startup code
 * runs, not by a startup observer that may run after it.
 */
public class OpenTelemetryDevUIStartupSpanTest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((JavaArchive jar) -> jar
                    .addClass(StartupSpan.class)
                    .addAsResource(new StringAsset(
                            "quarkus.otel.traces.exporter=none\n"
                                    + "quarkus.otel.metrics.exporter=none\n"
                                    + "quarkus.otel.logs.exporter=none\n"
                                    + "quarkus.devservices.enabled=false\n"),
                            "application.properties"));

    public OpenTelemetryDevUIStartupSpanTest() {
        super("devui-observability-traces");
    }

    @Test
    public void aSpanEndedDuringStartupIsCaptured() {
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            JsonNode snapshot = super.executeJsonRPCMethod("getSnapshot");
            assertThat(snapshot.toString()).contains("startup-work");
        });
    }
}
