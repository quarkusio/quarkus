package io.quarkus.test.opentelemetry;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;

/**
 * Holds no resource between two calls: every operation opens a {@link Connection} and closes it before returning, so
 * that a test has nothing to clean up and nothing can leak, whatever the test does or forgets to do. In particular
 * there is no static state, as it would outlive the application and pin its class loader.
 * <p>
 * The requests are made with the HTTP client of the JDK, on purpose. An HTTP client of the application, or one built
 * on its Vert.x instance, is instrumented: the requests made here would show up as client spans and client metrics
 * among the telemetry the test is about to check.
 */
final class SpanExporterClientImpl implements SpanExporterClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final URI spansUri;

    SpanExporterClientImpl(URI applicationUri) {
        String root = applicationUri.toString();
        if (root.endsWith("/")) {
            root = root.substring(0, root.length() - 1);
        }
        this.spansUri = URI.create(root + SpanExporterRoute.PATH);
    }

    @Override
    public SpanDataList fetch() {
        try (Connection connection = new Connection(spansUri)) {
            return connection.fetch();
        }
    }

    @Override
    public void reset() {
        try (Connection connection = new Connection(spansUri)) {
            Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> {
                connection.send("DELETE", 204);
                return connection.fetch().isEmpty();
            });
        }
    }

    @Override
    public SpanDataList awaitCount(int expected, Duration timeout) {
        return awaitUntil("exactly " + expected + " spans", timeout, spans -> spans.size() == expected);
    }

    @Override
    public SpanDataList awaitUntil(String description, Duration timeout, Predicate<SpanDataList> condition) {
        try (Connection connection = new Connection(spansUri)) {
            AtomicReference<SpanDataList> last = new AtomicReference<>();
            try {
                return Awaitility.await().atMost(timeout).until(connection::fetch, spans -> {
                    last.set(spans);
                    return condition.test(spans);
                });
            } catch (ConditionTimeoutException e) {
                SpanDataList observed = last.get();
                throw new AssertionError("Expected " + description + " within " + timeout + " but found "
                        + (observed == null ? "no completed response" : observed), e);
            }
        }
    }

    /**
     * The HTTP client of one operation.
     */
    private static final class Connection implements AutoCloseable {

        private final URI spansUri;
        private final HttpClient client = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();

        Connection(URI spansUri) {
            this.spansUri = spansUri;
        }

        SpanDataList fetch() {
            return new SpanDataListImpl(SpanWireFormat.decode(send("GET", 200)));
        }

        String send(String method, int expectedStatus) {
            HttpRequest request = HttpRequest.newBuilder(spansUri)
                    .method(method, HttpRequest.BodyPublishers.noBody())
                    .timeout(REQUEST_TIMEOUT)
                    .build();
            HttpResponse<String> response;
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException e) {
                throw new IllegalStateException(method + " " + spansUri + " failed", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted during " + method + " " + spansUri, e);
            }
            if (response.statusCode() != expectedStatus) {
                throw new IllegalStateException(method + " " + spansUri + " answered " + response.statusCode()
                        + ", is quarkus-test-opentelemetry a dependency of the application?");
            }
            return response.body();
        }

        @Override
        public void close() {
            client.close();
        }
    }
}
