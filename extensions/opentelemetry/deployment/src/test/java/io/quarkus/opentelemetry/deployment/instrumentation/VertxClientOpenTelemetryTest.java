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
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.HttpMethod;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.deployment.common.TestSpanExporter;
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
            .withApplicationRoot((jar) -> jar.addClass(TestSpanExporter.class));

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
        assertEquals("HTTP GET", client.getName());
        assertEquals(HTTP_OK, client.getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, client.getAttributes().get(HTTP_METHOD));
        assertEquals(uri.toString() + "hello", client.getAttributes().get(HTTP_URL));

        SpanData server = getSpanByKindAndParentId(spans, SERVER, client.getSpanId());
        assertEquals(SERVER, server.getKind());
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
        assertEquals("HTTP GET", client.getName());
        assertEquals(HTTP_OK, client.getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, client.getAttributes().get(HTTP_METHOD));
        assertEquals(uri.toString() + "hello/naruto", client.getAttributes().get(HTTP_URL));

        SpanData server = getSpanByKindAndParentId(spans, SERVER, client.getSpanId());
        assertEquals(SERVER, server.getKind());
        assertEquals("/hello/:name", server.getName());
        assertEquals(HTTP_OK, server.getAttributes().get(HTTP_STATUS_CODE));
        assertEquals(HttpMethod.GET, server.getAttributes().get(HTTP_METHOD));
        assertEquals("/hello/:name", server.getAttributes().get(HTTP_ROUTE));
        assertEquals(uri.getHost(), server.getAttributes().get(NET_HOST_NAME));
        assertEquals(uri.getPort(), server.getAttributes().get(NET_HOST_PORT));
        assertEquals(uri.getPath() + "hello/naruto", server.getAttributes().get(HTTP_TARGET));

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
        }
    }
}
