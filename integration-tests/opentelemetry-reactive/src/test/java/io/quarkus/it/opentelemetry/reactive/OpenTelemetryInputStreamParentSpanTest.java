package io.quarkus.it.opentelemetry.reactive;

import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.quarkus.it.opentelemetry.reactive.Utils.getSpans;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.quarkus.test.junit.QuarkusTest;

/**
 * A call made off a Vert.x context must keep the span that was current at the call site as the parent of the HTTP
 * client span, including when the body is an {@code InputStream} and is written after the propagation scope closes.
 */
@QuarkusTest
public class OpenTelemetryInputStreamParentSpanTest {
    @Inject
    @RestClient
    ReactiveRestClient client;

    @Inject
    Tracer tracer;

    @BeforeEach
    void reset() {
        given().get("/reset").then().statusCode(HTTP_OK);
        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().isEmpty());
    }

    @Test
    void stringBodyKeepsCallerSpan() {
        assertClientSpanKeepsCaller(restClient -> assertEquals("Hello hello",
                restClient.helloPost("hello").await().indefinitely()));
    }

    @Test
    void inputStreamBodyKeepsCallerSpan() {
        assertClientSpanKeepsCaller(restClient -> {
            byte[] echoed = restClient.stream(new ByteArrayInputStream(new byte[] { 1, 2, 3 })).await().indefinitely();
            assertEquals(3, echoed.length);
        });
    }

    private void assertClientSpanKeepsCaller(Consumer<ReactiveRestClient> call) {
        Span parent = tracer.spanBuilder("parent").startSpan();
        try (Scope ignored = parent.makeCurrent()) {
            call.accept(client);
        } finally {
            parent.end();
        }
        String parentId = parent.getSpanContext().getSpanId();
        await().atMost(5, TimeUnit.SECONDS).until(() -> clientSpan(parentId) != null);
        assertEquals(parent.getSpanContext().getTraceId(), clientSpan(parentId).get("traceId"));
    }

    private static Map<String, Object> clientSpan(String parentId) {
        return getSpans().stream()
                .filter(span -> parentId.equals(span.get("parentSpanId")) && CLIENT.toString().equals(span.get("kind")))
                .findFirst()
                .orElse(null);
    }
}
