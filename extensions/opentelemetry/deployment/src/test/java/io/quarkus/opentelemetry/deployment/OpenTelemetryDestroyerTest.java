package io.quarkus.opentelemetry.deployment;

import static org.hamcrest.Matchers.is;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.opentelemetry.OpenTelemetryDestroyer;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class OpenTelemetryDestroyerTest {

    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestSpanExporter.class,
                            TestSpanExporterProvider.class,
                            HelloResource.class)
                    .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider")
                    .add(new StringAsset(
                            """
                                    quarkus.otel.traces.exporter=test-span-exporter
                                    quarkus.otel.metrics.exporter=none
                                    quarkus.otel.experimental.shutdown-wait-time=PT60S
                                    """),
                            "application.properties"));

    @Test
    void getShutdownWaitTime() {
        RestAssured.when()
                .get("/hello").then()
                .statusCode(200)
                .body(is("PT1M"));
    }

    @Path("/hello")
    public static class HelloResource {
        @GET
        public String hello() {
            return OpenTelemetryDestroyer.getShutdownWaitTime().toString();
        }
    }
}
