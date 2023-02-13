package io.quarkus.opentelemetry.deployment.instrumentation;

import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_METHOD;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_ROUTE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_STATUS_CODE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_TARGET;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_URL;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_NAME;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_PORT;
import static io.quarkus.opentelemetry.deployment.common.TestSpanExporter.getSpanByKindAndParentId;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.deployment.common.TestSpanExporter;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class RestClientOpenTelemetryTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot((jar) -> jar.addClass(TestSpanExporter.class))
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
        assertEquals("HTTP GET", client.getName());
        assertEquals(HTTP_OK, client.getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, client.getAttributes().get(HTTP_METHOD));
        assertEquals(uri.toString() + "hello", client.getAttributes().get(HTTP_URL));

        SpanData server = getSpanByKindAndParentId(spans, SERVER, client.getSpanId());
        assertEquals("/hello", server.getName());
        assertEquals(HTTP_OK, server.getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, server.getAttributes().get(HTTP_METHOD));
        assertEquals("/hello", server.getAttributes().get(HTTP_ROUTE));
        assertEquals(uri.getHost(), server.getAttributes().get(NET_HOST_NAME));
        assertEquals(uri.getPort(), server.getAttributes().get(NET_HOST_PORT));
        assertEquals(uri.getPath() + "hello", server.getAttributes().get(HTTP_TARGET));

        assertEquals(client.getTraceId(), server.getTraceId());
    }

    @Test
    void spanNameWithoutQueryString() {
        Response response = client.hello("1");
        assertEquals(response.getStatus(), HTTP_OK);
        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);

        SpanData client = getSpanByKindAndParentId(spans, CLIENT, "0000000000000000");
        assertEquals(CLIENT, client.getKind());
        assertEquals("HTTP GET", client.getName());
        assertEquals(HTTP_OK, client.getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, client.getAttributes().get(HTTP_METHOD));
        assertEquals(uri.toString() + "hello?query=1", client.getAttributes().get(HTTP_URL));

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
        assertEquals("HTTP GET", client.getName());
        assertEquals(HTTP_OK, client.getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, client.getAttributes().get(HTTP_METHOD));
        assertEquals(uri.toString() + "hello?query=1", client.getAttributes().get(HTTP_URL));

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
        assertEquals("HTTP GET", client.getName());
        assertEquals(HTTP_OK, client.getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, client.getAttributes().get(HTTP_METHOD));
        assertEquals(uri.toString() + "hello/another", client.getAttributes().get(HTTP_URL));

        SpanData server = getSpanByKindAndParentId(spans, SERVER, client.getSpanId());
        assertEquals(SERVER, server.getKind());
        assertEquals("/hello/{path}", server.getName());
        assertEquals(HTTP_OK, server.getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, server.getAttributes().get(HTTP_METHOD));
        assertEquals("/hello/{path}", server.getAttributes().get(HTTP_ROUTE));
        assertEquals(uri.getHost(), server.getAttributes().get(NET_HOST_NAME));
        assertEquals(uri.getPort(), server.getAttributes().get(NET_HOST_PORT));
        assertEquals(uri.getPath() + "hello/another", server.getAttributes().get(HTTP_TARGET));

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
