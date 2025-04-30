package io.quarkus.opentelemetry.deployment;

import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.opentelemetry.deployment.common.HelloResource;
import io.quarkus.opentelemetry.deployment.common.TracerRouter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.test.ContinuousTestingTestUtils;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class OpenTelemetryDevModeTest {
    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TracerRouter.class, HelloResource.class)
                    .addClasses(TestSpanExporter.class, TestSpanExporterProvider.class)
                    .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider")
                    .add(new StringAsset(ContinuousTestingTestUtils.appProperties(
                            "quarkus.otel.traces.exporter=test-span-exporter",
                            "quarkus.otel.metrics.exporter=none")), "application.properties"));

    @Test
    void testDevMode() {
        //make sure we have the correct span in dev mode
        //and the hot replacement stuff is not messing things up
        RestAssured.when().get("/hello").then()
                .statusCode(200)
                .body(is("GET"));

        RestAssured.when().get("/tracer").then()
                .statusCode(200)
                .body(is("Hello Tracer!"));

        TEST.modifySourceFile(TracerRouter.class, s -> s.replace("Hello", "Goodbye"));

        RestAssured.when().get("/tracer").then()
                .statusCode(200)
                .body(is("Goodbye Tracer!"));
    }
}
