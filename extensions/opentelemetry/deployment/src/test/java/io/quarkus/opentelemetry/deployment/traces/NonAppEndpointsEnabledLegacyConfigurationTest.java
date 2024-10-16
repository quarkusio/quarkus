package io.quarkus.opentelemetry.deployment.traces;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.opentelemetry.deployment.common.TracerRouter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class NonAppEndpointsEnabledLegacyConfigurationTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(TracerRouter.class)
                    .addClasses(TestSpanExporter.class, TestSpanExporterProvider.class)
                    .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider"))
            .withConfigurationResource("resource-config/application-no-metrics.properties")
            .overrideConfigKey("quarkus.otel.traces.suppress-non-application-uris", "false");

    @Inject
    TestSpanExporter testSpanExporter;

    @Test
    void testHealthEndpointNotTraced() throws InterruptedException {
        RestAssured.when().get("/q/health").then()
                .statusCode(200)
                .body(containsString("\"status\": \"UP\""));

        RestAssured.when().get("/q/health/live").then()
                .statusCode(200)
                .body(containsString("\"status\": \"UP\""));

        RestAssured.when().get("/q/health/ready").then()
                .statusCode(200)
                .body(containsString("\"status\": \"UP\""));

        RestAssured.when().get("/tracer").then()
                .statusCode(200)
                .body(is("Hello Tracer!"));

        testSpanExporter.assertSpanCount(5);
    }
}
