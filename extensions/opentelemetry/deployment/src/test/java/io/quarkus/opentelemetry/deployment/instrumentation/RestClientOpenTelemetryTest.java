package io.quarkus.opentelemetry.deployment.instrumentation;

import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_ROUTE;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_ADDRESS;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_PORT;
import static io.opentelemetry.semconv.UrlAttributes.URL_FULL;
import static io.quarkus.opentelemetry.deployment.common.SemconvResolver.assertSemanticAttribute;
import static io.quarkus.opentelemetry.deployment.common.SemconvResolver.assertTarget;
import static io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter.getSpanByKindAndParentId;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.deployment.common.SemconvResolver;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryLogRecordExporterProvider;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryMetricExporterProvider;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class RestClientOpenTelemetryTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addPackage(TestSpanExporter.class.getPackage())
            .addClasses(SemconvResolver.class)
            .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider")
            .addAsResource(new StringAsset(InMemoryMetricExporterProvider.class.getCanonicalName()),
                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider")
            .addAsResource(new StringAsset(InMemoryLogRecordExporterProvider.class.getCanonicalName()),
                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider"))
            .withConfigurationResource("application-default.properties")
            .overrideConfigKey("quarkus.rest-client.client.url", "${test.url}");

    @Inject
    TestSpanExporter spanExporter;
    @Inject
    @RestClient
    HelloClient client;
    @TestHTTPResource
    URI uri;

    @AfterEach
    void tearDown() {
        spanExporter.reset();
    }

    @Test
    void client() {
        Response response = client.hello();
        assertEquals(response.getStatus(), HTTP_OK);
        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);

        SpanData client = getSpanByKindAndParentId(spans, CLIENT, "0000000000000000");
        assertEquals("GET /hello", client.getName());
        assertSemanticAttribute(client, (long) HTTP_OK, HTTP_RESPONSE_STATUS_CODE);
        assertSemanticAttribute(client, HttpMethod.GET, HTTP_REQUEST_METHOD);
        assertSemanticAttribute(client, uri.toString() + "hello", URL_FULL);

        SpanData server = getSpanByKindAndParentId(spans, SERVER, client.getSpanId());
        assertEquals("GET /hello", server.getName());
        assertSemanticAttribute(server, (long) HTTP_OK, HTTP_RESPONSE_STATUS_CODE);
        assertSemanticAttribute(server, HttpMethod.GET, HTTP_REQUEST_METHOD);
        assertEquals("/hello", server.getAttributes().get(HTTP_ROUTE));
        assertSemanticAttribute(server, uri.getHost(), SERVER_ADDRESS);
        assertSemanticAttribute(server, (long) uri.getPort(), SERVER_PORT);
        assertTarget(server, uri.getPath() + "hello", null);

        assertEquals(client.getTraceId(), server.getTraceId());
    }

    @Test
    void spanNameWithoutQueryString() {
        Response response = client.hello("1");
        assertEquals(response.getStatus(), HTTP_OK);
        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);

        SpanData client = getSpanByKindAndParentId(spans, CLIENT, "0000000000000000");
        assertEquals(CLIENT, client.getKind());
        assertEquals("GET /hello", client.getName());
        assertSemanticAttribute(client, (long) HTTP_OK, HTTP_RESPONSE_STATUS_CODE);
        assertSemanticAttribute(client, HttpMethod.GET, HTTP_REQUEST_METHOD);
        assertSemanticAttribute(client, uri.toString() + "hello?query=1", URL_FULL);

        SpanData server = getSpanByKindAndParentId(spans, SERVER, client.getSpanId());
        assertEquals(client.getTraceId(), server.getTraceId());
    }

    @Test
    void urlWithoutAuthentication() {
        WebTarget target = ClientBuilder.newClient()
                .target(UriBuilder.fromUri(uri).userInfo("username:password").path("hello").queryParam("query", "1"));
        Response response = target.request().get();
        assertEquals(response.getStatus(), HTTP_OK);
        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);

        SpanData client = getSpanByKindAndParentId(spans, CLIENT, "0000000000000000");
        assertEquals(CLIENT, client.getKind());
        assertEquals("GET", client.getName());
        assertSemanticAttribute(client, (long) HTTP_OK, HTTP_RESPONSE_STATUS_CODE);
        assertSemanticAttribute(client, HttpMethod.GET, HTTP_REQUEST_METHOD);
        assertSemanticAttribute(client, uri.toString() + "hello?query=1", URL_FULL);

        SpanData server = getSpanByKindAndParentId(spans, SERVER, client.getSpanId());
        assertEquals(client.getTraceId(), server.getTraceId());
    }

    @Test
    void path() {
        Response response = client.path("another");
        assertEquals(response.getStatus(), HTTP_OK);
        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);

        SpanData client = getSpanByKindAndParentId(spans, CLIENT, "0000000000000000");
        assertEquals(CLIENT, client.getKind());
        assertEquals("GET /hello/{path}", client.getName());
        assertSemanticAttribute(client, (long) HTTP_OK, HTTP_RESPONSE_STATUS_CODE);
        assertSemanticAttribute(client, HttpMethod.GET, HTTP_REQUEST_METHOD);
        assertSemanticAttribute(client, uri.toString() + "hello/another", URL_FULL);

        SpanData server = getSpanByKindAndParentId(spans, SERVER, client.getSpanId());
        assertEquals(SERVER, server.getKind());
        assertEquals("GET /hello/{path}", server.getName());
        assertSemanticAttribute(server, (long) HTTP_OK, HTTP_RESPONSE_STATUS_CODE);
        assertSemanticAttribute(server, HttpMethod.GET, HTTP_REQUEST_METHOD);
        assertEquals("/hello/{path}", server.getAttributes().get(HTTP_ROUTE));
        assertSemanticAttribute(server, uri.getHost(), SERVER_ADDRESS);
        assertSemanticAttribute(server, (long) uri.getPort(), SERVER_PORT);
        assertTarget(server, uri.getPath() + "hello/another", null);
        assertEquals(client.getTraceId(), server.getTraceId());
    }

    @Path("/hello")
    public static class HelloResource {
        @GET
        public String hello(@QueryParam("query") String query) {
            return "hello";
        }

        @GET
        @Path("/{path}")
        public String path(@PathParam("path") String path) {
            return "hello";
        }
    }

    @RegisterRestClient(configKey = "client")
    @Path("/hello")
    public interface HelloClient {
        @GET
        Response hello();

        @GET
        Response hello(@QueryParam("query") String query);

        @GET
        @Path("/{path}")
        Response path(@PathParam("path") String path);
    }
}
