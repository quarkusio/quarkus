package io.quarkus.opentelemetry.deployment;

import static org.hamcrest.Matchers.is;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
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
                    .addClasses(TracerRouter.class, HelloResource.class, GlobalOpenTelemetryResource.class)
                    .addClasses(TestSpanExporter.class, TestSpanExporterProvider.class)
                    .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider")
                    .add(new StringAsset(ContinuousTestingTestUtils.appProperties(
                            "quarkus.otel.traces.exporter=test-span-exporter",
                            "quarkus.otel.metrics.exporter=none",
                            "quarkus.otel.logs.exporter=none",
                            "quarkus.datasource.devservices.enabled=false",
                            "quarkus.observability.enabled=false")), "application.properties"));

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

    @Test
    void globalIsTheBeanAfterEachReload() {
        RestAssured.get("/global").then().statusCode(200).body(is("v0 true"));
        // the resource is a nested class, so its source file is this one
        TEST.modifySourceFile(OpenTelemetryDevModeTest.class, s -> s.replace("\"v0 \" +", "\"v1 \" +"));
        RestAssured.get("/global").then().statusCode(200).body(is("v1 true"));
        TEST.modifySourceFile(OpenTelemetryDevModeTest.class, s -> s.replace("\"v1 \" +", "\"v2 \" +"));
        RestAssured.get("/global").then().statusCode(200).body(is("v2 true"));
    }

    @Path("/global")
    public static class GlobalOpenTelemetryResource {
        @Inject
        OpenTelemetry openTelemetry;

        @GET
        public String get() {
            return "v0 " + (GlobalOpenTelemetry.get().getTracerProvider() == openTelemetry.getTracerProvider());
        }
    }
}
