package io.quarkus.it.opentelemetry.reactive;

import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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
import io.opentelemetry.sdk.testing.assertj.TracesAssert;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.opentelemetry.SpanDataList;
import io.quarkus.test.opentelemetry.SpanExporterClient;
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

    @TestHTTPResource
    URI applicationUri;

    SpanExporterClient spanExporter() {
        return SpanExporterClient.at(applicationUri);
    }

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
        spanExporter().reset();
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

        SpanDataList spans = spanExporter().awaitCount(3, Duration.ofSeconds(10));
        TracesAssert.assertThat(spans).hasSize(1);

        // First span is the one created by the test. It does not have a parent span.
        SpanData parent = spans.ofKind(INTERNAL).roots().single();
        assertThat(parent).hasName("parent");

        // The client span is a child of the span that was current when the client was called.
        SpanData client = spans.ofKind(CLIENT).childrenOf(parent).single();
        assertThat(client).hasName("POST /parent-span/string");

        // We should get one server span, from the client call. The client is the parent.
        SpanData server = spans.ofKind(SERVER).childrenOf(client).single();
        assertThat(server).hasName("POST /parent-span/string");
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

        SpanDataList spans = spanExporter().awaitCount(3, Duration.ofSeconds(10));
        TracesAssert.assertThat(spans).hasSize(1);

        SpanData parent = spans.ofKind(INTERNAL).roots().single();
        assertThat(parent).hasName("parent");

        SpanData client = spans.ofKind(CLIENT).childrenOf(parent).single();
        assertThat(client).hasName("POST /parent-span/string");

        SpanData server = spans.ofKind(SERVER).childrenOf(client).single();
        assertThat(server).hasName("POST /parent-span/string");
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

        SpanDataList spans = spanExporter().awaitCount(3, Duration.ofSeconds(10));
        TracesAssert.assertThat(spans).hasSize(1);

        SpanData parent = spans.ofKind(INTERNAL).roots().single();
        assertThat(parent).hasName("parent");

        SpanData client = spans.ofKind(CLIENT).childrenOf(parent).single();
        assertThat(client).hasName("POST /parent-span/stream");

        SpanData server = spans.ofKind(SERVER).childrenOf(client).single();
        assertThat(server).hasName("POST /parent-span/stream");
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

        SpanDataList spans = spanExporter().awaitCount(3, Duration.ofSeconds(10));
        TracesAssert.assertThat(spans).hasSize(1);

        SpanData parent = spans.ofKind(INTERNAL).roots().single();
        assertThat(parent).hasName("parent");

        SpanData client = spans.ofKind(CLIENT).childrenOf(parent).single();
        assertThat(client).hasName("POST /parent-span/stream");

        SpanData server = spans.ofKind(SERVER).childrenOf(client).single();
        assertThat(server).hasName("POST /parent-span/stream");
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

        SpanDataList spans = spanExporter().awaitCount(3, Duration.ofSeconds(10));
        TracesAssert.assertThat(spans).hasSize(1);

        SpanData parent = spans.ofKind(INTERNAL).roots().single();
        assertThat(parent).hasName("parent");

        SpanData client = spans.ofKind(CLIENT).childrenOf(parent).single();
        assertThat(client).hasName("POST /parent-span/stream");

        SpanData server = spans.ofKind(SERVER).childrenOf(client).single();
        assertThat(server).hasName("POST /parent-span/stream");
    }

    @Test
    void multiBody() {
        Span parentSpan = tracer.spanBuilder("parent").startSpan();
        try (Scope ignored = parentSpan.makeCurrent()) {
            assertArrayEquals(BYTES, client.multi(Multi.createFrom().items(Buffer.buffer(BYTES))));
        } finally {
            parentSpan.end();
        }

        SpanDataList spans = spanExporter().awaitCount(3, Duration.ofSeconds(10));
        TracesAssert.assertThat(spans).hasSize(1);

        SpanData parent = spans.ofKind(INTERNAL).roots().single();
        assertThat(parent).hasName("parent");

        SpanData client = spans.ofKind(CLIENT).childrenOf(parent).single();
        assertThat(client).hasName("POST /parent-span/stream");

        SpanData server = spans.ofKind(SERVER).childrenOf(client).single();
        assertThat(server).hasName("POST /parent-span/stream");
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

        SpanDataList spans = spanExporter().awaitCount(3, Duration.ofSeconds(10));
        TracesAssert.assertThat(spans).hasSize(1);

        SpanData parent = spans.ofKind(INTERNAL).roots().single();
        assertThat(parent).hasName("parent");

        SpanData client = spans.ofKind(CLIENT).childrenOf(parent).single();
        assertThat(client).hasName("POST /parent-span/multipart");

        SpanData server = spans.ofKind(SERVER).childrenOf(client).single();
        assertThat(server).hasName("POST /parent-span/multipart");
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
        SpanDataList spans = spanExporter().awaitCount(5, Duration.ofSeconds(10));
        TracesAssert.assertThat(spans).hasSize(2);

        // First call: the client span is a child of the span created by the test.
        SpanData parent = spans.ofKind(INTERNAL).roots().single();
        SpanData firstClient = spans.ofKind(CLIENT).childrenOf(parent).single();
        assertThat(parent).hasTraceId(firstClient.getTraceId());
        SpanData firstServer = spans.ofKind(SERVER).childrenOf(firstClient).single();
        assertThat(firstServer).hasTraceId(parent.getTraceId());

        // Second call: the client span does not have a parent span and is in another trace.
        SpanData secondClient = spans.ofKind(CLIENT).roots().single();
        SpanData secondServer = spans.ofKind(SERVER).childrenOf(secondClient).single();
        assertThat(secondClient).hasTraceId(secondServer.getTraceId());
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
        SpanDataList spans = spanExporter().awaitCount(calls * 3, Duration.ofSeconds(10));
        TracesAssert.assertThat(spans).hasSize(calls);

        SpanDataList parents = spans.ofKind(INTERNAL).roots();
        assertThat(parents).hasSize(calls);
        for (SpanData parent : parents) {
            SpanData client = spans.ofKind(CLIENT).childrenOf(parent).single();
            SpanData server = spans.ofKind(SERVER).childrenOf(client).single();
            assertThat(parent).hasTraceId(server.getTraceId());
        }
    }
}
