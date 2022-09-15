package io.quarkus.it.opentelemetry.reactive;

import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_METHOD;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_ROUTE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_STATUS_CODE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_TARGET;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.SpanKind;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;

@QuarkusTest
public class OpenTelemetryReactiveClientTest {
    @Inject
    @RestClient
    ReactiveRestClient client;

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
        assertEquals(spans.get(0).get("traceId"), spans.get(1).get("traceId"));
        assertEquals(spans.get(0).get("traceId"), spans.get(2).get("traceId"));

        Map<String, Object> internal = spans.get(0);
        Map<String, Object> server = spans.get(1);
        Map<String, Object> client = spans.get(2);

        assertEquals(SpanKind.INTERNAL.toString(), internal.get("kind"));
        assertEquals("helloGet", internal.get("name"));
        assertEquals(internal.get("parentSpanId"), server.get("spanId"));

        assertEquals(SpanKind.SERVER.toString(), server.get("kind"));
        assertEquals(server.get("parentSpanId"), client.get("spanId"));
        assertEquals("/reactive", server.get("name"));
        assertEquals("/reactive", ((Map<?, ?>) server.get("attributes")).get(HTTP_ROUTE.getKey()));
        assertEquals("/reactive?name=Naruto", ((Map<?, ?>) server.get("attributes")).get(HTTP_TARGET.getKey()));
        assertEquals(HTTP_OK, ((Map<?, ?>) server.get("attributes")).get(HTTP_STATUS_CODE.getKey()));
        assertEquals(HttpMethod.GET.name(), ((Map<?, ?>) server.get("attributes")).get(HTTP_METHOD.getKey()));

        assertEquals(SpanKind.CLIENT.toString(), client.get("kind"));
        assertEquals("HTTP GET", client.get("name"));

        assertEquals(HTTP_OK, ((Map<?, ?>) client.get("attributes")).get(HTTP_STATUS_CODE.getKey()));
        assertEquals(HttpMethod.GET.name(), ((Map<?, ?>) client.get("attributes")).get(HTTP_METHOD.getKey()));
    }

    @Test
    void post() {
        Uni<String> result = client.helloPost("Naruto");
        assertEquals("Hello Naruto", result.await().indefinitely());

        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 3);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(3, spans.size());
        assertEquals(spans.get(0).get("traceId"), spans.get(1).get("traceId"));
        assertEquals(spans.get(0).get("traceId"), spans.get(2).get("traceId"));

        Map<String, Object> internal = spans.get(0);
        Map<String, Object> server = spans.get(1);
        Map<String, Object> client = spans.get(2);

        assertEquals(SpanKind.INTERNAL.toString(), internal.get("kind"));
        assertEquals("helloPost", internal.get("name"));
        assertEquals(internal.get("parentSpanId"), server.get("spanId"));

        assertEquals(SpanKind.SERVER.toString(), server.get("kind"));
        assertEquals(server.get("parentSpanId"), client.get("spanId"));
        assertEquals("/reactive", server.get("name"));
        assertEquals("/reactive", ((Map<?, ?>) server.get("attributes")).get(HTTP_ROUTE.getKey()));
        assertEquals("/reactive", ((Map<?, ?>) server.get("attributes")).get(HTTP_TARGET.getKey()));
        assertEquals(HTTP_OK, ((Map<?, ?>) server.get("attributes")).get(HTTP_STATUS_CODE.getKey()));
        assertEquals(HttpMethod.POST.name(), ((Map<?, ?>) server.get("attributes")).get(HTTP_METHOD.getKey()));

        assertEquals(SpanKind.CLIENT.toString(), client.get("kind"));
        assertEquals("HTTP POST", client.get("name"));

        assertEquals(HTTP_OK, ((Map<?, ?>) client.get("attributes")).get(HTTP_STATUS_CODE.getKey()));
        assertEquals(HttpMethod.POST.name(), ((Map<?, ?>) client.get("attributes")).get(HTTP_METHOD.getKey()));
    }

    private static List<Map<String, Object>> getSpans() {
        return when().get("/export").body().as(new TypeRef<>() {
        });
    }
}
