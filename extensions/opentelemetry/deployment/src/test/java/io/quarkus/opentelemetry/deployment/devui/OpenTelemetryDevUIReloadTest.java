package io.quarkus.opentelemetry.deployment.devui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.Duration;

import org.awaitility.Awaitility;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.opentelemetry.deployment.common.HelloResource;
import io.quarkus.opentelemetry.deployment.common.TracerRouter;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import tools.jackson.databind.JsonNode;

/**
 * Verifies that captured traces survive a dev-mode live reload. The store is kept in a
 * static holder in the (base-classloader-loaded) OpenTelemetry runtime jar, so a reload
 * that recreates the app's beans does not clear the ring buffer.
 */
@DisabledOnOs(OS.WINDOWS)
public class OpenTelemetryDevUIReloadTest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((JavaArchive jar) -> jar
                    .addClasses(HelloResource.class, TracerRouter.class)
                    .addAsResource(new StringAsset(
                            "quarkus.otel.traces.exporter=none\n"
                                    + "quarkus.otel.metrics.exporter=none\n"
                                    + "quarkus.otel.logs.exporter=none\n"
                                    + "quarkus.devservices.enabled=false\n"),
                            "application.properties"));

    public OpenTelemetryDevUIReloadTest() {
        super("quarkus-opentelemetry");
    }

    @Test
    public void capturedTracesSurviveLiveReload() throws Exception {
        // Capture a span before the reload.
        RestAssured.when().get("/hello").then().statusCode(200);
        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            JsonNode snapshot = super.executeJsonRPCMethod("getSnapshot");
            assertThat(snapshot.toString()).contains("/hello");
        });

        // Trigger a live reload by editing a source file; the next request applies it.
        config.modifySourceFile(TracerRouter.class, s -> s.replace("Hello Tracer!", "Goodbye Tracer!"));
        RestAssured.when().get("/tracer").then().statusCode(200).body(is("Goodbye Tracer!"));

        // The pre-reload "/hello" span must still be present after the restart. We do NOT
        // hit "/hello" again, so its presence proves the buffer survived the reload.
        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            JsonNode snapshot = super.executeJsonRPCMethod("getSnapshot");
            assertThat(snapshot.toString())
                    .as("traces captured before a live reload should survive the reload")
                    .contains("/hello");
        });
    }
}
