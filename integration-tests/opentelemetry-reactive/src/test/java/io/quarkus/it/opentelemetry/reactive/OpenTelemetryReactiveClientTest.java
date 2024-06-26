package io.quarkus.it.opentelemetry.reactive;

import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_ROUTE;
import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;
import static io.opentelemetry.semconv.UrlAttributes.URL_QUERY;
import static io.quarkus.it.opentelemetry.reactive.Utils.getSpanByKindAndParentId;
import static io.quarkus.it.opentelemetry.reactive.Utils.getSpans;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.stream.Collectors.toSet;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.SpanKind;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;

@QuarkusTest
public class OpenTelemetryReactiveClientTest {
    @Inject
    @RestClient
    ReactiveRestClient client;

    @BeforeEach
    @AfterEach
    void reset() {
        given().get("/reset").then().statusCode(HTTP_OK);
        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 0);
    }

    @Test
    void get() {
        Uni<String> result = client.helloGet("Naruto");
        assertEquals("Hello Naruto", result.await().indefinitely());

        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 3);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(3, spans.size());
        assertEquals(1, spans.stream().map(map -> map.get("traceId")).collect(toSet()).size());

        // First span is the client call. It does not have a parent span.
        Map<String, Object> client = getSpanByKindAndParentId(spans, CLIENT, "0000000000000000");
        assertEquals(SpanKind.CLIENT.toString(), client.get("kind"));
        assertEquals("GET /reactive", client.get("name"));
        assertEquals(HTTP_OK, ((Map<?, ?>) client.get("attributes")).get(HTTP_RESPONSE_STATUS_CODE.getKey()));
        assertEquals(HttpMethod.GET.name(), ((Map<?, ?>) client.get("attributes")).get(HTTP_REQUEST_METHOD.getKey()));

        // We should get one server span, from the client call. The client is the parent.
        Map<String, Object> server = getSpanByKindAndParentId(spans, SERVER, client.get("spanId"));
        assertEquals(SpanKind.SERVER.toString(), server.get("kind"));
        assertEquals(server.get("parentSpanId"), client.get("spanId"));
        assertEquals("GET /reactive", server.get("name"));
        assertEquals("/reactive", ((Map<?, ?>) server.get("attributes")).get(HTTP_ROUTE.getKey()));
        assertEquals("/reactive", ((Map<?, ?>) server.get("attributes")).get(URL_PATH.getKey()));
        assertEquals("name=Naruto", ((Map<?, ?>) server.get("attributes")).get(URL_QUERY.getKey()));
        assertEquals(HTTP_OK, ((Map<?, ?>) server.get("attributes")).get(HTTP_RESPONSE_STATUS_CODE.getKey()));
        assertEquals(HttpMethod.GET.name(), ((Map<?, ?>) server.get("attributes")).get(HTTP_REQUEST_METHOD.getKey()));

        // Final span is an internal one, created by the resource method call. The server is the parent
        Map<String, Object> internal = getSpanByKindAndParentId(spans, INTERNAL, server.get("spanId"));
        assertEquals(SpanKind.INTERNAL.toString(), internal.get("kind"));
        assertEquals("helloGet", internal.get("name"));
        assertEquals(internal.get("parentSpanId"), server.get("spanId"));
    }

    @Test
    void post() {
        Uni<String> result = client.helloPost("Naruto");
        assertEquals("Hello Naruto", result.await().indefinitely());

        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 3);

        List<Map<String, Object>> spans = getSpans();
        assertEquals(3, spans.size());
        assertEquals(1, spans.stream().map(map -> map.get("traceId")).collect(toSet()).size());

        // First span is the client call. It does not have a parent span.
        Map<String, Object> client = getSpanByKindAndParentId(spans, CLIENT, "0000000000000000");
        assertEquals(SpanKind.CLIENT.toString(), client.get("kind"));
        assertEquals("POST /reactive", client.get("name"));
        assertEquals(HTTP_OK, ((Map<?, ?>) client.get("attributes")).get(HTTP_RESPONSE_STATUS_CODE.getKey()));
        assertEquals(HttpMethod.POST.name(), ((Map<?, ?>) client.get("attributes")).get(HTTP_REQUEST_METHOD.getKey()));

        // We should get one server span, from the client call. The client is the parent.
        Map<String, Object> server = getSpanByKindAndParentId(spans, SERVER, client.get("spanId"));
        assertEquals(SpanKind.SERVER.toString(), server.get("kind"));
        assertEquals(server.get("parentSpanId"), client.get("spanId"));
        assertEquals("POST /reactive", server.get("name"));
        assertEquals("/reactive", ((Map<?, ?>) server.get("attributes")).get(HTTP_ROUTE.getKey()));
        assertEquals("/reactive", ((Map<?, ?>) server.get("attributes")).get(URL_PATH.getKey()));
        assertEquals(HTTP_OK, ((Map<?, ?>) server.get("attributes")).get(HTTP_RESPONSE_STATUS_CODE.getKey()));
        assertEquals(HttpMethod.POST.name(), ((Map<?, ?>) server.get("attributes")).get(HTTP_REQUEST_METHOD.getKey()));

        // Final span is an internal one, created by the resource method call. The server is the parent
        Map<String, Object> internal = getSpanByKindAndParentId(spans, INTERNAL, server.get("spanId"));
        assertEquals(SpanKind.INTERNAL.toString(), internal.get("kind"));
        assertEquals("helloPost", internal.get("name"));
        assertEquals(internal.get("parentSpanId"), server.get("spanId"));
    }
}
