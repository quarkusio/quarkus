package io.quarkus.it.opentelemetry.reactive;

import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.quarkus.it.opentelemetry.reactive.Utils.getSpans;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Vertx;

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

    @Inject
    Vertx vertx;

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

    @Test
    void alternatingParentAndRootRequestsDoNotShareContext() {
        Set<String> parentIds = new HashSet<>();
        Set<String> parentTraceIds = new HashSet<>();
        Map<String, String> expectedTraces = new HashMap<>();
        for (int i = 0; i < 5; i++) {
            Span parent = tracer.spanBuilder("sequential-parent").startSpan();
            parentIds.add(parent.getSpanContext().getSpanId());
            parentTraceIds.add(parent.getSpanContext().getTraceId());
            expectedTraces.put(parent.getSpanContext().getSpanId(), parent.getSpanContext().getTraceId());
            try (Scope ignored = parent.makeCurrent()) {
                assertEquals(3, client.stream(new ByteArrayInputStream(new byte[] { 1, 2, 3 }))
                        .await().atMost(Duration.ofSeconds(10)).length);
            } finally {
                parent.end();
            }
            try (Scope ignored = Context.root().makeCurrent()) {
                assertEquals(3, client.stream(new ByteArrayInputStream(new byte[] { 1, 2, 3 }))
                        .await().atMost(Duration.ofSeconds(10)).length);
            }
        }
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Map<String, Object>> spans = getSpans().stream()
                    .filter(span -> CLIENT.toString().equals(span.get("kind"))).toList();
            assertEquals(10, spans.size());
            assertEquals(5, spans.stream().filter(span -> parentIds.contains(span.get("parentSpanId"))
                    && expectedTraces.get(span.get("parentSpanId")).equals(span.get("traceId"))).count());
            assertEquals(5, spans.stream().filter(span -> SpanContext.getInvalid().getSpanId().equals(span.get("parentSpanId"))
                    && !parentTraceIds.contains(span.get("traceId"))).count());
        });
    }

    @Test
    void concurrentRequestsKeepTheirOwnParent() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch ready = new CountDownLatch(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<SpanContext>> calls = new ArrayList<>();
        try {
            for (int i = 0; i < 8; i++) {
                calls.add(executor.submit(() -> {
                    Span parent = tracer.spanBuilder("concurrent-parent").startSpan();
                    try (Scope ignored = parent.makeCurrent()) {
                        ready.countDown();
                        if (!start.await(10, TimeUnit.SECONDS)) {
                            throw new AssertionError("Timed out waiting for concurrent requests");
                        }
                        assertEquals(3, client.stream(new ByteArrayInputStream(new byte[] { 1, 2, 3 }))
                                .await().atMost(Duration.ofSeconds(10)).length);
                        return parent.getSpanContext();
                    } finally {
                        parent.end();
                    }
                }));
            }
            if (!ready.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting for request workers");
            }
            start.countDown();
            List<SpanContext> parents = new ArrayList<>();
            for (Future<SpanContext> call : calls) {
                parents.add(call.get(30, TimeUnit.SECONDS));
            }
            await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
                List<Map<String, Object>> spans = getSpans().stream()
                        .filter(span -> CLIENT.toString().equals(span.get("kind"))).toList();
                assertEquals(parents.size(), spans.size());
                for (SpanContext parent : parents) {
                    assertEquals(1, spans.stream().filter(span -> parent.getSpanId().equals(span.get("parentSpanId"))
                            && parent.getTraceId().equals(span.get("traceId"))).count());
                }
            });
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void requestFromDuplicatedVertxContextKeepsParent() throws Exception {
        io.vertx.core.Context context = VertxContext.createNewDuplicatedContext(vertx.getOrCreateContext());
        VertxContextSafetyToggle.setContextSafe(context, true);
        CompletableFuture<SpanContext> done = new CompletableFuture<>();
        context.runOnContext(ignored -> {
            Span parent = tracer.spanBuilder("vertx-parent").startSpan();
            try (Scope scope = parent.makeCurrent()) {
                client.stream(new ByteArrayInputStream(new byte[] { 1, 2, 3 })).subscribe().with(result -> {
                    parent.end();
                    if (result.length == 3) {
                        done.complete(parent.getSpanContext());
                    } else {
                        done.completeExceptionally(new AssertionError("Unexpected response length"));
                    }
                }, error -> {
                    parent.end();
                    done.completeExceptionally(error);
                });
            } catch (Throwable error) {
                parent.end();
                done.completeExceptionally(error);
            }
        });
        SpanContext parent = done.get(30, TimeUnit.SECONDS);
        await().atMost(10, TimeUnit.SECONDS).until(() -> clientSpan(parent.getSpanId()) != null);
        assertEquals(parent.getTraceId(), clientSpan(parent.getSpanId()).get("traceId"));
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
