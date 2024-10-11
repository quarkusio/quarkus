package io.quarkus.micrometer.opentelemetry.deployment.compatibility;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.quarkus.micrometer.opentelemetry.deployment.common.InMemoryMetricExporter;
import io.quarkus.micrometer.opentelemetry.deployment.common.InMemoryMetricExporterProvider;
import io.quarkus.micrometer.opentelemetry.deployment.common.MetricDataFilter;
import io.quarkus.rest.client.reactive.Url;
import io.quarkus.test.QuarkusUnitTest;

public class RestClientUriParameterTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(
                    jar -> jar.addClasses(Resource.class, Client.class)
                            .addClasses(InMemoryMetricExporter.class,
                                    InMemoryMetricExporterProvider.class,
                                    MetricDataFilter.class)
                            .addClasses(InMemoryMetricExporter.class, InMemoryMetricExporterProvider.class,
                                    MetricDataFilter.class)
                            .addAsResource(new StringAsset(InMemoryMetricExporterProvider.class.getCanonicalName()),
                                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider"))
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.otel.metrics.exporter", "in-memory")
            .overrideConfigKey("quarkus.otel.metric.export.interval", "300ms")
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .overrideConfigKey("quarkus.rest-client.\"client\".url", "http://does-not-exist.io");

    @Inject
    InMemoryMetricExporter metricExporter;

    @RestClient
    Client client;

    @ConfigProperty(name = "quarkus.http.test-port")
    Integer testPort;

    @Test
    public void testOverride() {
        String result = client.getById("http://localhost:" + testPort, "bar");
        assertEquals("bar", result);

        metricExporter.assertCountDataPointsAtLeastOrEqual("http.client.requests", null, 1);
        assertEquals(1, metricExporter.find("http.client.requests")
                .tag("uri", "/example/{id}")
                .lastReadingDataPoint(HistogramPointData.class).getCount());
    }

    @Path("/example")
    @RegisterRestClient(baseUri = "http://dummy")
    public interface Client {

        @GET
        @Path("/{id}")
        String getById(@Url String baseUri, @PathParam("id") String id);
    }

    @Path("/example")
    public static class Resource {

        @RestClient
        Client client;

        @GET
        @Path("/{id}")
        @Produces(MediaType.TEXT_PLAIN)
        public String example() {
            return "bar";
        }

        @GET
        @Path("/call")
        @Produces(MediaType.TEXT_PLAIN)
        public String call() {
            return client.getById("http://localhost:8080", "1");
        }
    }
}
