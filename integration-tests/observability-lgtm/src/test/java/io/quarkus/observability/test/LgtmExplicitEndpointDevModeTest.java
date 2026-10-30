package io.quarkus.observability.test;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.observability.test.support.HelloEndpoint;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

/**
 * Verifies that dev mode boots successfully when an OTLP endpoint is explicitly
 * configured and LGTM Dev Services do not start.
 * <p>
 * Before the fix, {@code ObservabilityJsonRPCService} injected required config
 * properties ({@code grafana.endpoint}, {@code otel-collector.url}) that are only
 * provided by the LGTM container. When the container didn't start, the app
 * crashed at boot with {@code Failed to load config value}.
 */
@DisabledOnOs(OS.WINDOWS)
@Tag("devmode")
public class LgtmExplicitEndpointDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(HelloEndpoint.class)
                    .addAsResource(
                            "explicit-endpoint-application.properties",
                            "application.properties"));

    @Test
    public void testAppBootsWithExplicitEndpoint() {
        RestAssured.given()
                .get("/hello")
                .then()
                .statusCode(200);
    }
}
