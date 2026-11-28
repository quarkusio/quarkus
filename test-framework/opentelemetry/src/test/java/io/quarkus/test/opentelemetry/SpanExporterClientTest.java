package io.quarkus.test.opentelemetry;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.net.URI;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.testing.assertj.TracesAssert;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;

/**
 * Runs the route and the client against each other, over HTTP.
 */
class SpanExporterClientTest {

    private static final AttributeKey<String> URL_PATH = AttributeKey.stringKey("url.path");

    private final InMemorySpanExporter exporter = InMemorySpanExporter.create();
    // as in an application: finished spans wait in the batch span processor, here for so long that only a flush
    // gets them to the exporter
    private final SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(exporter).setScheduleDelay(Duration.ofMinutes(10)).build())
            .build();

    private Vertx vertx;
    private SpanExporterClient client;

    private URI serverUri;

    @BeforeEach
    void startServer() throws Exception {
        vertx = Vertx.vertx();
        Router router = Router.router(vertx);
        SpanExporterRoute.register(router, exporter, tracerProvider::forceFlush);
        // the same routes again, as an application with an HTTP root path serves them
        Router main = Router.router(vertx);
        main.get("/stalled" + SpanExporterRoute.PATH).handler(ctx -> ctx.response().setChunked(true).write("["));
        main.route("/root/*").subRouter(router);
        main.route().subRouter(router);
        HttpServer server = vertx.createHttpServer().requestHandler(main).listen(0)
                .toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
        serverUri = URI.create("http://localhost:" + server.actualPort() + "/");
        client = SpanExporterClient.at(serverUri);
    }

    @AfterEach
    void stopServer() throws Exception {
        vertx.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
        tracerProvider.close();
    }

    private void finishSpan(String name, SpanKind kind, Attributes attributes) {
        Span span = tracerProvider.get("test").spanBuilder(name).setSpanKind(kind).setAllAttributes(attributes).startSpan();
        span.end();
    }

    @Test
    void fetchesAwaitsAndResets() {
        assertThat(client.fetch()).isEmpty();

        finishSpan("first", SpanKind.INTERNAL, Attributes.empty());
        finishSpan("second", SpanKind.CLIENT, Attributes.empty());

        SpanDataList spans = client.awaitCount(2, Duration.ofSeconds(10));
        assertThat(spans.named("second").single()).hasKind(SpanKind.CLIENT).hasNoParent();
        TracesAssert.assertThat(spans).hasSize(2);

        client.reset();
        assertThat(client.fetch()).isEmpty();
    }

    @Test
    void leavesOutTheRequestsToTheRouteItself() {
        finishSpan("GET /test-otel/spans", SpanKind.SERVER, Attributes.of(URL_PATH, "/root/test-otel/spans"));
        finishSpan("GET /hello", SpanKind.SERVER, Attributes.of(URL_PATH, "/hello"));

        assertThat(client.fetch()).extracting(SpanData::getName)
                .isEqualTo(List.of("GET /hello"));
    }

    /**
     * A span that is finished before the reset is still queued in the batch span processor. It must be gone with the
     * reset, instead of reaching the exporter later and showing up among the spans of the next test.
     */
    @Test
    void resetAlsoForgetsTheSpansStillQueuedInTheSpanProcessor() {
        finishSpan("finished before the reset", SpanKind.INTERNAL, Attributes.empty());

        client.reset();

        // what the batch span processor eventually does on its own
        tracerProvider.forceFlush().join(10, TimeUnit.SECONDS);
        assertThat(exporter.getFinishedSpanItems()).isEmpty();
        assertThat(client.fetch()).isEmpty();
    }

    @Test
    void worksWithAnHttpRootPath() {
        SpanExporterClient rootPathClient = SpanExporterClient.at(serverUri.resolve("/root"));
        finishSpan("only", SpanKind.INTERNAL, Attributes.empty());

        assertThat(rootPathClient.awaitCount(1, Duration.ofSeconds(10)).single().getName()).isEqualTo("only");
        rootPathClient.reset();
        assertThat(rootPathClient.fetch()).isEmpty();
    }

    @Test
    void failsWhenTheApplicationDoesNotServeTheRoutes() {
        SpanExporterClient misplacedClient = SpanExporterClient.at(serverUri.resolve("/nowhere"));

        assertThatThrownBy(misplacedClient::fetch)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("/nowhere/test-otel/spans answered 404");
        assertThatThrownBy(misplacedClient::reset).isInstanceOf(IllegalStateException.class);
    }

    /**
     * The client creates an HTTP client for every operation. None of their threads may survive the operation.
     */
    @Test
    void leavesNoThreadBehind() {
        Set<Thread> before = httpClientThreads();

        client.fetch();
        finishSpan("only", SpanKind.INTERNAL, Attributes.empty());
        client.awaitCount(1, Duration.ofSeconds(10));
        assertThatThrownBy(() -> client.awaitCount(2, Duration.ofMillis(300))).isInstanceOf(AssertionError.class);
        client.reset();

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(httpClientThreads()).isSubsetOf(before));
    }

    private static Set<Thread> httpClientThreads() {
        Set<Thread> threads = new HashSet<>();
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.isAlive() && thread.getName().startsWith("HttpClient-")) {
                threads.add(thread);
            }
        }
        return threads;
    }

    @Test
    @Timeout(5)
    void pollingDeadlineAlsoAppliesToAnIncompleteResponse() {
        SpanExporterClient stalled = SpanExporterClient.at(serverUri.resolve("/stalled"));
        assertThatThrownBy(() -> stalled.awaitCount(1, Duration.ofMillis(400))).isInstanceOf(AssertionError.class)
                .hasMessageContaining("no completed response");
    }

    @Test
    void awaitFailureListsTheSpansFound() {
        finishSpan("only", SpanKind.INTERNAL, Attributes.empty());

        assertThatThrownBy(() -> client.awaitCount(2, Duration.ofMillis(300)))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected exactly 2 spans within PT0.3S but found 1 span(s):")
                .hasMessageContaining("INTERNAL 'only'");
    }
}
