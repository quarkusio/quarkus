package io.quarkus.it.opentelemetry.reactive;

import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.quarkus.it.opentelemetry.reactive.Utils.getSpanByKindAndParentId;
import static io.quarkus.it.opentelemetry.reactive.Utils.getSpans;
import static io.quarkus.it.opentelemetry.reactive.Utils.getSpansByKindAndParentId;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.stream.Collectors.toSet;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import io.vertx.core.buffer.Buffer;

/**
 * The REST Client is called while a span is current, from a thread that is not on a Vert.x context. The HTTP client
 * span must be a child of that span, whatever the request body type.
 * <p>
 * See https://github.com/quarkusio/quarkus/issues/58573
 */
@QuarkusTest
public class RestClientParentSpanTest {

    private static final String NO_PARENT = "0000000000000000";
    private static final byte[] BYTES = { 1, 2, 3 };

    @Inject
    Tracer tracer;

    @Inject
    @RestClient
    ParentSpanRestClient client;

    @Inject
    @RestClient
    ParentSpanInterceptedRestClient interceptedClient;

    @TempDir
    Path tempDir;

    @BeforeEach
    @AfterEach
    void reset() {
        given().get("/reset").then().statusCode(HTTP_OK);
        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 0);
    }

    /**
     * This test was not failing before the fix.
     */
    @RepeatedTest(10)
    void stringBody() {
        Span parentSpan = tracer.spanBuilder("parent").startSpan();
        try (Scope ignored = parentSpan.makeCurrent()) {
            assertEquals("hello", client.string("hello"));
        } finally {
            parentSpan.end();
        }

        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 3);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(3, spans.size());
        assertEquals(1, spans.stream().map(map -> map.get("traceId")).collect(toSet()).size());

        // First span is the one created by the test. It does not have a parent span.
        Map<String, Object> parent = getSpanByKindAndParentId(spans, INTERNAL, NO_PARENT);
        assertEquals("parent", parent.get("name"));

        // The client span is a child of the span that was current when the client was called.
        Map<String, Object> client = getSpanByKindAndParentId(spans, CLIENT, parent.get("spanId"));
        assertEquals("POST /parent-span/string", client.get("name"));

        // We should get one server span, from the client call. The client is the parent.
        Map<String, Object> server = getSpanByKindAndParentId(spans, SERVER, client.get("spanId"));
        assertEquals("POST /parent-span/string", server.get("name"));
    }

    /**
     * Same as {@link #stringBody()}, but the span is created and the client is called on a new thread, not the test
     * thread.
     * <p>
     * Failed before the fix, in every repetition: the client span had no parent and started a second trace.
     */
    @RepeatedTest(10)
    void stringBodyFromNewThread() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            executor.submit(() -> {
                Span parentSpan = tracer.spanBuilder("parent").startSpan();
                try (Scope ignored = parentSpan.makeCurrent()) {
                    assertEquals("hello", client.string("hello"));
                } finally {
                    parentSpan.end();
                }
            }).get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 3);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(3, spans.size());
        assertEquals(1, spans.stream().map(map -> map.get("traceId")).collect(toSet()).size());

        Map<String, Object> parent = getSpanByKindAndParentId(spans, INTERNAL, NO_PARENT);
        assertEquals("parent", parent.get("name"));

        Map<String, Object> client = getSpanByKindAndParentId(spans, CLIENT, parent.get("spanId"));
        assertEquals("POST /parent-span/string", client.get("name"));

        Map<String, Object> server = getSpanByKindAndParentId(spans, SERVER, client.get("spanId"));
        assertEquals("POST /parent-span/string", server.get("name"));
    }

    /**
     * The reproducer of the issue: an InputStream body is streamed, the request is sent asynchronously.
     * <p>
     * Failed before the fix: the client span had no parent and started a second trace.
     */
    @Test
    void inputStreamBody() {
        Span parentSpan = tracer.spanBuilder("parent").startSpan();
        try (Scope ignored = parentSpan.makeCurrent()) {
            assertArrayEquals(BYTES, client.stream(new ByteArrayInputStream(BYTES)));
        } finally {
            parentSpan.end();
        }

        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 3);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(3, spans.size());
        assertEquals(1, spans.stream().map(map -> map.get("traceId")).collect(toSet()).size());

        Map<String, Object> parent = getSpanByKindAndParentId(spans, INTERNAL, NO_PARENT);
        assertEquals("parent", parent.get("name"));

        Map<String, Object> client = getSpanByKindAndParentId(spans, CLIENT, parent.get("spanId"));
        assertEquals("POST /parent-span/stream", client.get("name"));

        Map<String, Object> server = getSpanByKindAndParentId(spans, SERVER, client.get("spanId"));
        assertEquals("POST /parent-span/stream", server.get("name"));
    }

    /**
     * A WriterInterceptor makes the REST Client serialize the InputStream on a worker thread instead of streaming it.
     * <p>
     * Failed before the fix: the client span had no parent and started a second trace.
     */
    @Test
    void inputStreamBodyWithWriterInterceptor() {
        Span parentSpan = tracer.spanBuilder("parent").startSpan();
        try (Scope ignored = parentSpan.makeCurrent()) {
            assertArrayEquals(BYTES, interceptedClient.stream(new ByteArrayInputStream(BYTES)));
        } finally {
            parentSpan.end();
        }

        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 3);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(3, spans.size());
        assertEquals(1, spans.stream().map(map -> map.get("traceId")).collect(toSet()).size());

        Map<String, Object> parent = getSpanByKindAndParentId(spans, INTERNAL, NO_PARENT);
        assertEquals("parent", parent.get("name"));

        Map<String, Object> client = getSpanByKindAndParentId(spans, CLIENT, parent.get("spanId"));
        assertEquals("POST /parent-span/stream", client.get("name"));

        Map<String, Object> server = getSpanByKindAndParentId(spans, SERVER, client.get("spanId"));
        assertEquals("POST /parent-span/stream", server.get("name"));
    }

    /**
     * A File body is streamed, like an InputStream.
     * <p>
     * Failed before the fix: the client span had no parent and started a second trace.
     */
    @Test
    void fileBody() throws Exception {
        File file = Files.write(tempDir.resolve("body.bin"), BYTES).toFile();

        Span parentSpan = tracer.spanBuilder("parent").startSpan();
        try (Scope ignored = parentSpan.makeCurrent()) {
            assertArrayEquals(BYTES, client.file(file));
        } finally {
            parentSpan.end();
        }

        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 3);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(3, spans.size());
        assertEquals(1, spans.stream().map(map -> map.get("traceId")).collect(toSet()).size());

        Map<String, Object> parent = getSpanByKindAndParentId(spans, INTERNAL, NO_PARENT);
        assertEquals("parent", parent.get("name"));

        Map<String, Object> client = getSpanByKindAndParentId(spans, CLIENT, parent.get("spanId"));
        assertEquals("POST /parent-span/stream", client.get("name"));

        Map<String, Object> server = getSpanByKindAndParentId(spans, SERVER, client.get("spanId"));
        assertEquals("POST /parent-span/stream", server.get("name"));
    }

    @Test
    void multiBody() {
        Span parentSpan = tracer.spanBuilder("parent").startSpan();
        try (Scope ignored = parentSpan.makeCurrent()) {
            assertArrayEquals(BYTES, client.multi(Multi.createFrom().items(Buffer.buffer(BYTES))));
        } finally {
            parentSpan.end();
        }

        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 3);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(3, spans.size());
        assertEquals(1, spans.stream().map(map -> map.get("traceId")).collect(toSet()).size());

        Map<String, Object> parent = getSpanByKindAndParentId(spans, INTERNAL, NO_PARENT);
        assertEquals("parent", parent.get("name"));

        Map<String, Object> client = getSpanByKindAndParentId(spans, CLIENT, parent.get("spanId"));
        assertEquals("POST /parent-span/stream", client.get("name"));

        Map<String, Object> server = getSpanByKindAndParentId(spans, SERVER, client.get("spanId"));
        assertEquals("POST /parent-span/stream", server.get("name"));
    }

    @Test
    void multipartBody() throws Exception {
        File file = Files.write(tempDir.resolve("body.bin"), BYTES).toFile();

        Span parentSpan = tracer.spanBuilder("parent").startSpan();
        try (Scope ignored = parentSpan.makeCurrent()) {
            assertEquals("name", client.multipart("name", file));
        } finally {
            parentSpan.end();
        }

        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 3);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(3, spans.size());
        assertEquals(1, spans.stream().map(map -> map.get("traceId")).collect(toSet()).size());

        Map<String, Object> parent = getSpanByKindAndParentId(spans, INTERNAL, NO_PARENT);
        assertEquals("parent", parent.get("name"));

        Map<String, Object> client = getSpanByKindAndParentId(spans, CLIENT, parent.get("spanId"));
        assertEquals("POST /parent-span/multipart", client.get("name"));

        Map<String, Object> server = getSpanByKindAndParentId(spans, SERVER, client.get("spanId"));
        assertEquals("POST /parent-span/multipart", server.get("name"));
    }

    /**
     * The caller's context is stored on the Vert.x context of the call and never removed from it. A second call, made
     * without a current span, must not pick it up: it starts its own trace.
     * <p>
     * Failed before the fix, because the client span of the first call had no parent: there were three traces instead
     * of two.
     */
    @Test
    void secondCallWithoutSpanHasNoParent() {
        Span parentSpan = tracer.spanBuilder("parent").startSpan();
        try (Scope ignored = parentSpan.makeCurrent()) {
            assertEquals("first", client.string("first"));
        } finally {
            parentSpan.end();
        }
        assertEquals("second", client.string("second"));

        // parent, client and server of the first call, client and server of the second one
        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 5);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(5, spans.size());
        assertEquals(2, spans.stream().map(map -> map.get("traceId")).collect(toSet()).size());

        // First call: the client span is a child of the span created by the test.
        Map<String, Object> parent = getSpanByKindAndParentId(spans, INTERNAL, NO_PARENT);
        Map<String, Object> firstClient = getSpanByKindAndParentId(spans, CLIENT, parent.get("spanId"));
        assertEquals(parent.get("traceId"), firstClient.get("traceId"));

        // Second call: the client span does not have a parent span and is in another trace.
        Map<String, Object> secondClient = getSpanByKindAndParentId(spans, CLIENT, NO_PARENT);
        Map<String, Object> secondServer = getSpanByKindAndParentId(spans, SERVER, secondClient.get("spanId"));
        assertEquals(secondClient.get("traceId"), secondServer.get("traceId"));
    }

    /**
     * Calls made at the same time, each with its own span: every span created by the test gets exactly one client
     * span.
     * <p>
     * Failed before the fix: some client spans had no parent, so there were more traces than calls.
     */
    @Test
    void concurrentCallsKeepTheirOwnParent() throws Exception {
        int calls = 10;
        ExecutorService executor = Executors.newFixedThreadPool(calls);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < calls; i++) {
                futures.add(executor.submit(() -> {
                    Span parentSpan = tracer.spanBuilder("parent").startSpan();
                    try (Scope ignored = parentSpan.makeCurrent()) {
                        assertEquals("hello", client.string("hello"));
                    } finally {
                        parentSpan.end();
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        // parent, client and server for each call
        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == calls * 3);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(calls * 3, spans.size());
        assertEquals(calls, spans.stream().map(map -> map.get("traceId")).collect(toSet()).size());

        List<Map<String, Object>> parents = getSpansByKindAndParentId(spans, INTERNAL, NO_PARENT);
        assertEquals(calls, parents.size());
        for (Map<String, Object> parent : parents) {
            Map<String, Object> client = getSpanByKindAndParentId(spans, CLIENT, parent.get("spanId"));
            Map<String, Object> server = getSpanByKindAndParentId(spans, SERVER, client.get("spanId"));
            assertEquals(parent.get("traceId"), server.get("traceId"));
        }
    }
}
