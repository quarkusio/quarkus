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
import io.quarkus.opentelemetry.deployment.common.TracerRouter;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryMetricExporterProvider;
import io.quarkus.opentelemetry.deployment.common.traces.TraceMeResource;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class OpenTelemetrySuppressAppUrisTest {
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
                    .addClasses(TracerRouter.class, TraceMeResource.class))
            .overrideConfigKey("quarkus.otel.traces.suppress-application-uris", "tracer,/hello/Itachi");

    @Inject
    InMemoryExporter exporter;

    @BeforeEach
    void setup() {
        exporter.reset();
    }

    @Test
    @DisplayName("Should not trace when the using configuration quarkus.otel.traces.suppress-application-uris without slash")
    void testingSuppressAppUrisWithoutSlash() {
        RestAssured.when()
                .get("/tracer").then()
                .statusCode(200)
                .body(is("Hello Tracer!"));

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
    @DisplayName("Should not trace when the using configuration quarkus.otel.traces.suppress-application-uris with slash")
    void testingSuppressAppUrisWithSlash() {
        RestAssured.when()
                .get("/hello/Itachi").then()
                .statusCode(200)
                .body(is("Amaterasu!"));

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
