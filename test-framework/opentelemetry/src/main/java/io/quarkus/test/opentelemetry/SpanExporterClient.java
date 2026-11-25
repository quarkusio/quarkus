package io.quarkus.test.opentelemetry;

import java.net.URI;
import java.time.Duration;
import java.util.function.Predicate;

import io.opentelemetry.sdk.trace.data.SpanData;

/**
 * Fetches the spans finished by the application under test from the routes of {@link SpanExporterRoute}.
 * <p>
 * A test tells the client where the application is, usually with the injected test URL:
 *
 * <pre>{@code
 * &#64;TestHTTPResource
 * URI applicationUri;
 *
 * SpanExporterClient spanExporter() {
 *     return SpanExporterClient.at(applicationUri);
 * }
 * }</pre>
 *
 * Spans are exported asynchronously, so a test first waits for the spans it expects and then picks them apart. The
 * spans are the {@link SpanData} of OpenTelemetry, to be checked with its own
 * {@link io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions assertions}:
 *
 * <pre>{@code
 * SpanDataList spans = spanExporter().awaitCount(3, Duration.ofSeconds(10));
 * TracesAssert.assertThat(spans).hasSize(1); // all the spans belong to one trace
 * SpanData parent = spans.ofKind(INTERNAL).roots().single();
 * SpanData client = spans.ofKind(CLIENT).childrenOf(parent).single();
 * assertThat(client).hasName("POST /hello").hasParent(parent).hasAttribute(HTTP_RESPONSE_STATUS_CODE, 200L);
 * }</pre>
 *
 * A client holds no resource between two calls, so there is nothing to close: every call releases everything it
 * opened before it returns.
 */
public interface SpanExporterClient {

    /**
     * @param applicationUri the root URI of the application under test, including its HTTP root path if it has one
     */
    static SpanExporterClient at(URI applicationUri) {
        return new SpanExporterClientImpl(applicationUri);
    }

    /**
     * Fetch the spans finished so far, without waiting.
     */
    SpanDataList fetch();

    /**
     * Forget the spans finished so far. As the spans of an earlier request can still be on their way, this keeps
     * resetting until no span is left.
     */
    void reset();

    /**
     * Wait until exactly {@code expected} spans are finished, and return them.
     */
    SpanDataList awaitCount(int expected, Duration timeout);

    /**
     * Wait until the finished spans satisfy the condition, and return them.
     *
     * @param description what is awaited, for the failure message
     */
    SpanDataList awaitUntil(String description, Duration timeout, Predicate<SpanDataList> condition);
}
