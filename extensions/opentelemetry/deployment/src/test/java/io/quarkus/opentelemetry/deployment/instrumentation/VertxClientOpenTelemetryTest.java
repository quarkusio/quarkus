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
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.HttpMethod;

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
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public class VertxClientOpenTelemetryTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addPackage(TestSpanExporter.class.getPackage())
                    .addClasses(SemconvResolver.class)
                    .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider")
                    .addAsResource(new StringAsset(InMemoryMetricExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider")
                    .addAsResource(new StringAsset(InMemoryLogRecordExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider"))
            .withConfigurationResource("application-default.properties");

    @Inject
    TestSpanExporter spanExporter;
    @Inject
    Vertx vertx;
    @TestHTTPResource
    URI uri;

    @AfterEach
    void tearDown() {
        spanExporter.reset();
    }

    @Test
    void client() throws Exception {
        HttpResponse<Buffer> response = WebClient.create(vertx)
                .get(uri.getPort(), uri.getHost(), "/hello")
                .send()
                .toCompletionStage().toCompletableFuture()
                .get();

        assertEquals(HTTP_OK, response.statusCode());

        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);

        SpanData client = getSpanByKindAndParentId(spans, CLIENT, "0000000000000000");
        assertEquals("GET", client.getName());
        assertSemanticAttribute(client, (long) HTTP_OK, HTTP_RESPONSE_STATUS_CODE);
        assertSemanticAttribute(client, HttpMethod.GET, HTTP_REQUEST_METHOD);
        assertSemanticAttribute(client, uri.toString() + "hello", URL_FULL);
        assertSemanticAttribute(client, uri.getHost(), SERVER_ADDRESS);
        assertSemanticAttribute(client, (long) uri.getPort(), SERVER_PORT);

        SpanData server = getSpanByKindAndParentId(spans, SERVER, client.getSpanId());
        assertEquals(SERVER, server.getKind());
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
    void path() throws Exception {
        HttpResponse<Buffer> response = WebClient.create(vertx)
                .get(uri.getPort(), uri.getHost(), "/hello/naruto")
                .send()
                .toCompletionStage().toCompletableFuture()
                .get();

        assertEquals(HTTP_OK, response.statusCode());

        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);

        SpanData client = getSpanByKindAndParentId(spans, CLIENT, "0000000000000000");
        assertEquals(CLIENT, client.getKind());
        assertEquals("GET", client.getName());
        assertSemanticAttribute(client, (long) HTTP_OK, HTTP_RESPONSE_STATUS_CODE);
        assertSemanticAttribute(client, HttpMethod.GET, HTTP_REQUEST_METHOD);
        assertSemanticAttribute(client, uri.toString() + "hello/naruto", URL_FULL);
        assertSemanticAttribute(client, uri.getHost(), SERVER_ADDRESS);
        assertSemanticAttribute(client, (long) uri.getPort(), SERVER_PORT);

        SpanData server = getSpanByKindAndParentId(spans, SERVER, client.getSpanId());
        assertEquals(SERVER, server.getKind());
        assertEquals("GET /hello/:name", server.getName());
        assertSemanticAttribute(server, (long) HTTP_OK, HTTP_RESPONSE_STATUS_CODE);
        assertSemanticAttribute(server, HttpMethod.GET, HTTP_REQUEST_METHOD);
        assertEquals("/hello/:name", server.getAttributes().get(HTTP_ROUTE));
        assertSemanticAttribute(server, uri.getHost(), SERVER_ADDRESS);
        assertSemanticAttribute(server, (long) uri.getPort(), SERVER_PORT);
        assertTarget(server, uri.getPath() + "hello/naruto", null);

        assertEquals(client.getTraceId(), server.getTraceId());
    }

    @Test
    void query() throws Exception {
        HttpResponse<Buffer> response = WebClient.create(vertx)
                .get(uri.getPort(), uri.getHost(), "/hello?name=foo")
                .send()
                .toCompletionStage().toCompletableFuture()
                .get();

        assertEquals(HTTP_OK, response.statusCode());

        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);

        SpanData client = getSpanByKindAndParentId(spans, CLIENT, "0000000000000000");
        assertEquals(CLIENT, client.getKind());
        assertEquals("GET", client.getName());
        assertSemanticAttribute(client, (long) HTTP_OK, HTTP_RESPONSE_STATUS_CODE);
        assertSemanticAttribute(client, HttpMethod.GET, HTTP_REQUEST_METHOD);
        assertSemanticAttribute(client, uri.toString() + "hello?name=foo", URL_FULL);
        assertSemanticAttribute(client, uri.getHost(), SERVER_ADDRESS);
        assertSemanticAttribute(client, (long) uri.getPort(), SERVER_PORT);

        SpanData server = getSpanByKindAndParentId(spans, SERVER, client.getSpanId());
        assertEquals(SERVER, server.getKind());
        assertEquals("GET /hello", server.getName());
        assertSemanticAttribute(server, (long) HTTP_OK, HTTP_RESPONSE_STATUS_CODE);
        assertSemanticAttribute(server, HttpMethod.GET, HTTP_REQUEST_METHOD);
        assertEquals("/hello", server.getAttributes().get(HTTP_ROUTE));
        assertSemanticAttribute(server, uri.getHost(), SERVER_ADDRESS);
        assertSemanticAttribute(server, (long) uri.getPort(), SERVER_PORT);
        assertTarget(server, uri.getPath() + "hello", "name=foo");

        assertEquals(client.getTraceId(), server.getTraceId());
    }

    @Test
    void multiple() throws Exception {
        HttpResponse<Buffer> response = WebClient.create(vertx)
                .get(uri.getPort(), uri.getHost(), "/multiple")
                .putHeader("host", uri.getHost())
                .putHeader("port", uri.getPort() + "")
                .send()
                .toCompletionStage().toCompletableFuture()
                .get();

        assertEquals(HTTP_OK, response.statusCode());

        List<SpanData> spans = spanExporter.getFinishedSpanItems(6);
        assertEquals(1, spans.stream().map(SpanData::getTraceId).collect(toSet()).size());
    }

    @ApplicationScoped
    public static class HelloRouter {
        @Inject
        Router router;
        @Inject
        Vertx vertx;

        public void register(@Observes StartupEvent ev) {
            router.get("/hello").handler(rc -> rc.response().end("hello"));
            router.get("/hello/:name").handler(rc -> rc.response().end("hello " + rc.pathParam("name")));
            router.get("/multiple").handler(rc -> {
                String host = rc.request().getHeader("host");
                int port = Integer.parseInt(rc.request().getHeader("port"));
                WebClient webClient = WebClient.create(vertx);
                Future<HttpResponse<Buffer>> one = webClient.get(port, host, "/hello/naruto").send();
                Future<HttpResponse<Buffer>> two = webClient.get(port, host, "/hello/goku").send();
                CompositeFuture.join(one, two).onComplete(event -> rc.response().end());
            });
            router.get("/hello?name=foo").handler(rc -> rc.response().end("hello foo"));
        }
    }
}
