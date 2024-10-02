package io.quarkus.opentelemetry.deployment;

import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter.getSpanByKindAndParentId;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryMetricExporterProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.config.SmallRyeConfig;

public class OpenTelemetryResourceTest {
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

    @Inject
    SmallRyeConfig config;
    @Inject
    InMemoryExporter exporter;

    @BeforeEach
    void setup() {
        exporter.reset();
    }

    @Test
    void resource() {
        RestAssured.when()
                .get("/hello").then()
                .statusCode(200)
                .body(is("hello"));

        List<SpanData> spans = exporter.getSpanExporter().getFinishedSpanItems(1);

        final SpanData server = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertEquals("GET /hello", server.getName());
        assertEquals("authservice", server.getResource().getAttribute(AttributeKey.stringKey("service.name")));
        assertEquals(config.getRawValue("quarkus.uuid"),
                server.getResource().getAttribute(AttributeKey.stringKey("service.instance.id")));
        assertNotNull(server.getResource().getAttribute(AttributeKey.stringKey("host.name")));

        exporter.getMetricExporter().assertCountAtLeast(1);
        List<MetricData> finishedMetricItems = exporter.getMetricExporter().getFinishedMetricItems();
    }

    @Path("/hello")
    public static class HelloResource {
        @Inject
        Meter meter;

        @GET
        public String hello() {
            meter.counterBuilder("hello").build().add(1);
            return "hello";
        }
    }
}
