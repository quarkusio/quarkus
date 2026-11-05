package io.quarkus.opentelemetry.deployment;

import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter.getSpanByKindAndParentId;
import static org.assertj.core.api.Assertions.assertThat;
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
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.internal.EntityUtil;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.ServiceAttributes;
import io.quarkus.opentelemetry.deployment.common.ResourceWarningGuard;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryMetricExporterProvider;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.smallrye.config.SmallRyeConfig;

public class OpenTelemetryResourceTest {
    @RegisterExtension
    static final QuarkusExtensionTest TEST = ResourceWarningGuard.apply(new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(InMemoryExporter.class.getPackage())
                    .addClass(ResourceWarningGuard.class)
                    .addAsResource("resource-config/application.properties", "application.properties")
                    .addAsResource(
                            "META-INF/services-config/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider",
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider")
                    .addAsResource(new StringAsset(InMemoryMetricExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider")));

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
        assertEquals(config.getConfigValue("quarkus.uuid").getValue(),
                server.getResource().getAttribute(AttributeKey.stringKey("service.instance.id")));
        assertNotNull(server.getResource().getAttribute(AttributeKey.stringKey("host.name")));
        assertEntities(server.getResource());

        exporter.getMetricExporter().assertCountAtLeast(1);
        List<MetricData> finishedMetricItems = exporter.getMetricExporter().getFinishedMetricItems();
    }

    /**
     * The service resource entity survives the configured service.name, which is the whole point of
     * the fix.
     * <p>
     * There is deliberately no service.instance entity on this path: a user supplied
     * service.instance.id switches off the SDK's own provider, so the value lands as a plain attribute
     * with no entity built from it. That is an accepted retreat, not an oversight — if a future SDK
     * version makes an entity appear here anyway, this assertion is meant to notice.
     */
    private static void assertEntities(Resource resource) {
        assertThat(EntityUtil.getEntities(resource))
                .filteredOn(entity -> "service".equals(entity.getType()))
                .as("the service resource entity must survive the configured service.name")
                .singleElement()
                .extracting(entity -> entity.getId().get(ServiceAttributes.SERVICE_NAME))
                .isEqualTo("authservice");

        assertThat(EntityUtil.getEntities(resource))
                .as("a user supplied service.instance.id leaves no service.instance entity")
                .noneMatch(entity -> "service.instance".equals(entity.getType()));
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
