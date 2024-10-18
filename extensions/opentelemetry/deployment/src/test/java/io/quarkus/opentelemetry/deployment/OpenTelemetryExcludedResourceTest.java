package io.quarkus.opentelemetry.deployment;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.metrics.Meter;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryMetricExporterProvider;
import io.quarkus.opentelemetry.runtime.tracing.Traceless;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.config.SmallRyeConfig;

public class OpenTelemetryExcludedResourceTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(InMemoryExporter.class.getPackage())
                    .addAsResource("resource-config/application.properties", "application.properties")
                    .addAsResource(
                            "META-INF/services-config/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider",
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider")
                    .addAsResource(new StringAsset(InMemoryMetricExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider"));
    private static final Logger log = LoggerFactory.getLogger(OpenTelemetryExcludedResourceTest.class);

    @Inject
    SmallRyeConfig config;
    @Inject
    InMemoryExporter exporter;

    @BeforeEach
    void setup() {
        exporter.reset();
    }

    @Test
    void testingHello() {
        RestAssured.when()
                .get("/hello").then()
                .statusCode(200)
                .body(is("hello"));

        //      should fail, because there is no span
        assertThrows(org.awaitility.core.ConditionTimeoutException.class,
                () -> exporter.getSpanExporter().getFinishedSpanItems(1));
    }

    @Test
    void testingHi() {
        RestAssured.when()
                .get("/hello/hi").then()
                .statusCode(200)
                .body(is("hi"));

        //      should fail, because there is no span
        assertThrows(org.awaitility.core.ConditionTimeoutException.class,
                () -> exporter.getSpanExporter().getFinishedSpanItems(1));
    }

    @Test
    void testingNoSlash() {
        RestAssured.when()
                .get("/hello/no-slash").then()
                .statusCode(200)
                .body(is("no-slash"));

        //      should fail, because there is no span
        assertThrows(org.awaitility.core.ConditionTimeoutException.class,
                () -> exporter.getSpanExporter().getFinishedSpanItems(1));
    }

    @Test
    void testingAtClassLevel() {
        RestAssured.when()
                .get("/class-level").then()
                .statusCode(200)
                .body(is("no-slash"));

        //      should fail, because there is no span
        assertThrows(org.awaitility.core.ConditionTimeoutException.class,
                () -> exporter.getSpanExporter().getFinishedSpanItems(1));
    }

    @Path("/class-level")
    @Traceless
    public static class ClassLevelResource {

        @Inject
        Meter meter;

        @GET
        @Path("/first-method")

        public String firstMethod() {
            meter.counterBuilder("method").build().add(1);
            return "method";
        }

        @Path("/second-method")
        @GET
        public String secondMethod() {
            meter.counterBuilder("method").build().add(1);
            return "method";
        }
    }

    @Path("/hello")
    public static class HelloResource {
        @Inject
        Meter meter;

        @GET
        @Traceless
        public String hello() {
            meter.counterBuilder("hello").build().add(1);
            return "hello";
        }

        @Path("/hi")
        @GET
        @Traceless
        public String hi() {
            meter.counterBuilder("hi").build().add(1);
            return "hi";
        }

        @Path("no-slash")
        @GET
        @Traceless
        public String noSlash() {
            meter.counterBuilder("no-slash").build().add(1);
            return "no-slash";
        }
    }
}
