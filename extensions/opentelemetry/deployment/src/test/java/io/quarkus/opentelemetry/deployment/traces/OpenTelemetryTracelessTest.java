package io.quarkus.opentelemetry.deployment.traces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryMetricExporterProvider;
import io.quarkus.opentelemetry.deployment.common.traces.TraceMeResource;
import io.quarkus.opentelemetry.deployment.common.traces.TracelessClassLevelResource;
import io.quarkus.opentelemetry.deployment.common.traces.TracelessHelloResource;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class OpenTelemetryTracelessTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(InMemoryExporter.class.getPackage())
                    .addAsResource("resource-config/application.properties", "application.properties")
                    .addAsResource(
                            "META-INF/services-config/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider",
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider")
                    .addAsResource(new StringAsset(InMemoryMetricExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider")
                    .addClasses(TracelessHelloResource.class, TracelessClassLevelResource.class, TraceMeResource.class));

    @Inject
    InMemoryExporter exporter;

    @BeforeEach
    void setup() {
        exporter.reset();
    }

    @Test
    @DisplayName("Should not trace when the method @Path uses @PathParam")
    void testingWithPathParam() {
        RestAssured.when()
                .get("/hello/mask/1").then()
                .statusCode(200)
                .body(is("mask-1"));

        RestAssured.when()
                .get("/trace-me").then()
                .statusCode(200)
                .body(is("trace-me"));

        List<SpanData> spans = exporter.getSpanExporter().getFinishedSpanItems(1);

        assertThat(spans)
                .hasSize(1)
                .satisfiesOnlyOnce(span -> assertThat(span.getName()).containsOnlyOnce("trace-me"));
    }

    @Test
    @DisplayName("Should not trace when the annotation @Traceless is at method level")
    void testingTracelessHelloHi() {

        RestAssured.when()
                .get("/hello").then()
                .statusCode(200)
                .body(is("hello"));

        RestAssured.when()
                .get("/hello/hi").then()
                .statusCode(200)
                .body(is("hi"));

        RestAssured.when()
                .get("/trace-me").then()
                .statusCode(200)
                .body(is("trace-me"));

        List<SpanData> spans = exporter.getSpanExporter().getFinishedSpanItems(1);
        assertThat(spans)
                .hasSize(1)
                .satisfiesOnlyOnce(span -> assertThat(span.getName()).containsOnlyOnce("trace-me"));
    }

    @Test
    @DisplayName("Should not trace when the method @Path is without '/'")
    void testingHelloNoSlash() {
        RestAssured.when()
                .get("/hello/no-slash").then()
                .statusCode(200)
                .body(is("no-slash"));

        RestAssured.when()
                .get("/trace-me").then()
                .statusCode(200)
                .body(is("trace-me"));

        List<SpanData> spans = exporter.getSpanExporter().getFinishedSpanItems(1);
        assertThat(spans)
                .hasSize(1)
                .satisfiesOnlyOnce(span -> assertThat(span.getName()).containsOnlyOnce("trace-me"));
    }

    @Test
    @DisplayName("Should not trace when the annotation is at class level")
    void testingTracelessAtClassLevel() {

        RestAssured.when()
                .get("class-level").then()
                .statusCode(200)
                .body(is("class-level"));

        RestAssured.when()
                .get("/class-level/first-method").then()
                .statusCode(200)
                .body(is("first-method"));

        RestAssured.when()
                .get("/class-level/second-method").then()
                .statusCode(200)
                .body(is("second-method"));

        RestAssured.when()
                .get("/trace-me").then()
                .statusCode(200)
                .body(is("trace-me"));

        List<SpanData> spans = exporter.getSpanExporter().getFinishedSpanItems(1);
        assertThat(spans)
                .hasSize(1)
                .satisfiesOnlyOnce(span -> assertThat(span.getName()).containsOnlyOnce("trace-me"));
    }
}
