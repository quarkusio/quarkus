package io.quarkus.it.observation.reactive;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.sdk.testing.assertj.TracesAssert;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.opentelemetry.SpanDataList;
import io.quarkus.test.opentelemetry.SpanExporterClient;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
public class ObservationReactiveTest {

    @TestHTTPResource
    URI applicationUri;

    SpanExporterClient spanExporter() {
        return SpanExporterClient.at(applicationUri);
    }

    private static final Logger logger = Logger.getLogger(ObservationReactiveTest.class);

    @BeforeEach
    @AfterEach
    void reset() {
        given().get("/reset").then().statusCode(HTTP_OK);
        spanExporter().reset();
    }

    @Test
    void simpleReactiveObservation() {
        given()
                .queryParam("name", "Naruto")
                .get("/reactive")
                .then()
                .statusCode(HTTP_OK);

        // 1 SERVER span (auto-instrumented) +
        // 1 INTERNAL span (observation)
        SpanDataList spans = waitForSpans(2);
        TracesAssert.assertThat(spans).hasSize(1);

        SpanData serverSpan = spans.ofKind(SERVER).roots().single();
        assertThat(serverSpan).hasName("GET /reactive");

        SpanData observationSpan = spans.ofKind(INTERNAL).childrenOf(serverSpan).single();
        assertThat(observationSpan).hasName("reactive.hello.observation");
        assertThat(observationSpan).hasAttribute(stringKey("name"), "Naruto");

        // One request → the timer recorded at least one observation.
        await().atMost(5, SECONDS).untilAsserted(() -> assertThat(getTimerCount("reactive.hello.observation"))
                .isGreaterThanOrEqualTo(1));
    }

    @Test
    void observedUniProducesSpan() {
        given()
                .get("/reactive/observed-uni")
                .then()
                .statusCode(HTTP_OK);

        // 1 SERVER +
        // 1 INTERNAL (@Observed on Uni)
        SpanDataList spans = waitForSpans(2);
        TracesAssert.assertThat(spans).hasSize(1);

        SpanData serverSpan = spans.ofKind(SERVER).roots().single();

        SpanData observedSpan = spans.ofKind(INTERNAL).childrenOf(serverSpan).single();
        assertThat(observedSpan.getAttributes().size()).isEqualTo(3);
        logger.infov("reactive.hello.observation Attributes: {0}", observedSpan.getAttributes());

        // One @Observed invocation → the "reactiveWork" timer recorded at least one observation.
        await().atMost(5, SECONDS).untilAsserted(() -> assertThat(getTimerCount("reactiveWork"))
                .isGreaterThanOrEqualTo(1));
    }

    @Test
    void observedMultiProducesChildSpanPerElement() {
        given()
                .get("/reactive/observed-multi")
                .then()
                .statusCode(HTTP_OK);

        // 1 SERVER +
        // 1 INTERNAL parent (@Observed on Multi, one per subscription) +
        // 3 INTERNAL children (one "stream.item.observation" per emitted item) = 5 spans
        SpanDataList spans = waitForSpans(5);
        TracesAssert.assertThat(spans).hasSize(1);

        SpanData serverSpan = spans.ofKind(SERVER).roots().single();

        // Exactly one parent observation span (the @Observed stream) is the child of the SERVER span.
        SpanData parentSpan = spans.ofKind(INTERNAL).childrenOf(serverSpan).single();
        assertThat(parentSpan.getName()).contains("reactiveStream");

        // Each of the three emitted items opened its own child observation, parented to the
        // stream observation - so the parent span has exactly one child span per element.
        SpanDataList childSpans = spans.ofKind(INTERNAL).childrenOf(parentSpan);
        assertThat(childSpans).hasSize(3);
        assertThat(childSpans).allSatisfy(child -> assertThat(child).hasName("stream.item.observation"));
        assertThat(childSpans)
                .extracting(child -> child.getAttributes().get(stringKey("item")))
                .containsExactlyInAnyOrder("one", "two", "three");

        // One @Observed subscription → the "reactiveStream" parent timer recorded once; the child
        // "stream.item.observation" timer recorded once per emitted item (three).
        await().atMost(5, SECONDS).untilAsserted(() -> {
            assertThat(getTimerCount("reactiveStream")).isGreaterThanOrEqualTo(1);
            assertThat(getTimerCount("stream.item.observation")).isGreaterThanOrEqualTo(3);
        });
    }

    @Test
    void chainedReactiveCalls() {
        given()
                .get("/reactive/multiple-chain")
                .then()
                .statusCode(HTTP_OK);

        // 1 SERVER (root) +
        // 1 INTERNAL (chain.operation observation) +
        // 2 CLIENT (rest-client calls) +
        // 2 SERVER (handlers) +
        // 2 INTERNAL (handler observations "reactive.hello.observation") = 8 spans
        SpanDataList spans = waitForSpans(8);
        TracesAssert.assertThat(spans).hasSize(1);

        SpanData rootServer = spans.ofKind(SERVER).roots().single();
        assertThat(rootServer).hasName("GET /reactive/multiple-chain");

        SpanData chainObservation = spans.ofKind(INTERNAL).childrenOf(rootServer).single();
        assertThat(chainObservation).hasName("chain.operation.observation");

        SpanDataList clientSpans = spans.ofKind(CLIENT).childrenOf(chainObservation);
        assertThat(clientSpans).hasSize(2);

        for (SpanData clientSpan : clientSpans) {

            // Each CLIENT span should have a child SERVER span (the handler)
            SpanData handlerServer = spans.ofKind(SERVER).childrenOf(clientSpan).single();
            assertThat(handlerServer).hasName("GET /reactive");

            // Each handler SERVER span should have a child INTERNAL span (reactive.hello.observation)
            SpanData handlerObservation = spans.ofKind(INTERNAL).childrenOf(handlerServer).single();
            assertThat(handlerObservation).hasName("reactive.hello.observation");
            logger.infov("reactive.hello.observation Attributes: {0}", handlerObservation.getAttributes());
        }

        // One request → the chain timer recorded once, and each of the two handler calls
        // recorded a "reactive.hello.observation".
        await().atMost(5, SECONDS).untilAsserted(() -> {
            assertThat(getTimerCount("chain.operation.observation")).isGreaterThanOrEqualTo(1);
            assertThat(getTimerCount("reactive.hello.observation")).isGreaterThanOrEqualTo(2);
        });
    }

    @Test
    void combinedReactiveCalls() {
        given()
                .get("/reactive/multiple-combine")
                .then()
                .statusCode(HTTP_OK);

        // Same structure as chain:
        // 1 SERVER +
        // 1 INTERNAL +
        // 2 CLIENT +
        // 2 SERVER +
        // 2 INTERNAL = 8
        SpanDataList spans = waitForSpans(8);
        TracesAssert.assertThat(spans).hasSize(1);

        SpanData rootServer = spans.ofKind(SERVER).roots().single();

        SpanData combineObservation = spans.ofKind(INTERNAL).childrenOf(rootServer).single();
        assertThat(combineObservation).hasName("combine.operation.observation");

        SpanDataList clientSpans = spans.ofKind(CLIENT).childrenOf(combineObservation);
        assertThat(clientSpans).hasSize(2);

        for (SpanData clientSpan : clientSpans) {

            SpanData handlerServer = spans.ofKind(SERVER).childrenOf(clientSpan).single();
            assertThat(handlerServer).hasName("GET /reactive");

            SpanData handlerObservation = spans.ofKind(INTERNAL).childrenOf(handlerServer).single();
            assertThat(handlerObservation).hasName("reactive.hello.observation");
            logger.infov("reactive.hello.observation Attributes: {0}", handlerObservation.getAttributes());
        }

        // One request → the combine timer recorded once, plus two handler observations.
        await().atMost(5, SECONDS).untilAsserted(() -> {
            assertThat(getTimerCount("combine.operation.observation")).isGreaterThanOrEqualTo(1);
            assertThat(getTimerCount("reactive.hello.observation")).isGreaterThanOrEqualTo(2);
        });
    }

    @Test
    void parallelCombinedCallsPreserveParentHierarchy() {
        int parallelRequests = 10;

        // Fire 10 requests in parallel
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(parallelRequests);
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(parallelRequests);
        for (int i = 0; i < parallelRequests; i++) {
            executor.submit(() -> {
                try {
                    given()
                            .get("/reactive/multiple-combine")
                            .then()
                            .statusCode(HTTP_OK);
                } finally {
                    latch.countDown();
                }
            });
        }
        try {
            latch.await(10, SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        executor.shutdown();

        // Each request produces 8 spans: 1 SERVER + 1 INTERNAL + 2 CLIENT + 2 SERVER + 2 INTERNAL
        SpanDataList spans = waitForSpans(parallelRequests * 8);

        // Find all root SERVER spans (parentSpanId = 0)
        SpanDataList rootServers = spans.ofKind(SERVER).roots().named("GET /reactive/multiple-combine");
        assertThat(rootServers).hasSize(parallelRequests);

        // Verify each request's span hierarchy is self-contained within its traceId
        for (SpanData rootServer : rootServers) {
            String traceId = rootServer.getTraceId();

            SpanDataList traceSpans = spans.filter(s -> traceId.equals(s.getTraceId()));
            assertThat(traceSpans).hasSize(8);

            // Root SERVER → INTERNAL (combine.operation.observation)
            SpanData combineObservation = traceSpans.ofKind(INTERNAL).childrenOf(rootServer).single();
            assertThat(combineObservation).hasName("combine.operation.observation");

            // Observation → 2 CLIENT spans
            SpanDataList clientSpans = traceSpans.ofKind(CLIENT).childrenOf(combineObservation);
            assertThat(clientSpans)
                    .withFailMessage("Expected 2 CLIENT spans for traceId=" + traceId)
                    .hasSize(2);

            for (SpanData clientSpan : clientSpans) {
                // CLIENT → SERVER (handler)
                SpanData handlerServer = traceSpans.ofKind(SERVER).childrenOf(clientSpan).single();

                // SERVER (handler) → INTERNAL (reactive.hello.observation)
                SpanData handlerObservation = traceSpans.ofKind(INTERNAL).childrenOf(handlerServer).single();
                assertThat(handlerObservation).hasName("reactive.hello.observation");
            }
        }

        // The combine timer must have recorded at least one observation per parallel request,
        // and each request drove two handler observations.
        await().atMost(5, SECONDS).untilAsserted(() -> {
            assertThat(getTimerCount("combine.operation.observation"))
                    .isGreaterThanOrEqualTo(parallelRequests);
            assertThat(getTimerCount("reactive.hello.observation"))
                    .isGreaterThanOrEqualTo(parallelRequests * 2L);
        });
    }

    @Test
    void workerDispatchPropagatesObservationAcrossThreads() {
        given()
                .get("/reactive/worker")
                .then()
                .statusCode(HTTP_OK);

        // 1 SERVER +
        // 1 INTERNAL (worker.parent.observation, request thread) +
        // 1 INTERNAL (child @Observed, worker thread) = 3 spans
        SpanDataList spans = waitForSpans(3);
        TracesAssert.assertThat(spans).hasSize(1);

        SpanData serverSpan = spans.ofKind(SERVER).roots().single();

        SpanData workerSpan = spans.ofKind(INTERNAL).childrenOf(serverSpan).single();
        assertThat(workerSpan).hasName("worker.parent.observation");

        // The child observation, produced on the worker thread, must be parented to the parent
        // observation (worker) opened on the request thread - proving the scope propagated across threads.
        SpanData childSpan = spans.ofKind(INTERNAL).childrenOf(workerSpan).single();
        assertThat(childSpan.getName()).contains("doChildWork");

        // Both the parent (request thread) and the child @Observed (worker thread) timers recorded.
        await().atMost(5, SECONDS).untilAsserted(() -> {
            assertThat(getTimerCount("worker.parent.observation")).isGreaterThanOrEqualTo(1);
            assertThat(getTimerCount("doChildWork")).isGreaterThanOrEqualTo(1);
        });
    }

    @Test
    void virtualThreadPropagatesObservation() {
        given()
                .get("/reactive/virtual")
                .then()
                .statusCode(HTTP_OK);

        // 1 SERVER +
        // 1 INTERNAL (virtual.parent.observation) +
        // 1 INTERNAL (nested child @Observed) = 3 spans
        SpanDataList spans = waitForSpans(3);
        TracesAssert.assertThat(spans).hasSize(1);

        SpanData serverSpan = spans.ofKind(SERVER).roots().single();

        SpanData parentSpan = spans.ofKind(INTERNAL).childrenOf(serverSpan).single();
        assertThat(parentSpan).hasName("virtual.parent.observation");

        SpanData childSpan = spans.ofKind(INTERNAL).childrenOf(parentSpan).single();
        assertThat(childSpan.getName()).contains("doChildWork");

        // Both the parent and the nested child @Observed timers recorded on the (virtual) thread.
        await().atMost(5, SECONDS).untilAsserted(() -> {
            assertThat(getTimerCount("virtual.parent.observation")).isGreaterThanOrEqualTo(1);
            assertThat(getTimerCount("doChildWork")).isGreaterThanOrEqualTo(1);
        });
    }

    @Test
    void reactiveError() {
        given()
                .get("/reactive/error")
                .then()
                .statusCode(HTTP_OK);

        // 1 SERVER +
        // 1 INTERNAL (error observation)
        SpanDataList spans = waitForSpans(2);
        TracesAssert.assertThat(spans).hasSize(1);

        SpanData serverSpan = spans.ofKind(SERVER).roots().single();

        SpanData errorSpan = spans.ofKind(INTERNAL).childrenOf(serverSpan).single();
        assertThat(errorSpan).hasName("error.operation.observation");

        // The failing observation is still recorded once by the timer.
        await().atMost(5, SECONDS).untilAsserted(() -> assertThat(getTimerCount("error.operation.observation"))
                .isGreaterThanOrEqualTo(1));
    }

    private SpanDataList waitForSpans(int minCount) {
        return spanExporter().awaitUntil("at least " + minCount + " spans", Duration.ofSeconds(5),
                spans -> spans.size() >= minCount);
    }

    private List<Map<String, Object>> getMetrics(String metricName) {
        return given()
                .queryParam("name", metricName)
                .get("/export/metrics")
                .body().as(new TypeRef<>() {
                });
    }

    /**
     * Returns the total recording count of an observation timer metric. A single metric can be
     * partitioned into several data points (one per attribute set, e.g. {@code name=Naruto} and
     * {@code name=Goku}) and the exporter may hold several cumulative snapshots. This sums the
     * counts of all points within a snapshot and returns the maximum across snapshots, which under
     * cumulative temporality is the total number of recordings. Assertions use {@code >=} the number
     * of requests the test issued, since the timer is cumulative for the JVM lifetime.
     */
    @SuppressWarnings("unchecked")
    private long getTimerCount(String metricName) {
        List<Map<String, Object>> metrics = getMetrics(metricName);
        assertThat(metrics).isNotEmpty();
        long max = 0;
        for (Map<String, Object> metric : metrics) {
            Map<String, Object> data = (Map<String, Object>) metric.get("data");
            List<Map<String, Object>> points = (List<Map<String, Object>>) data.get("points");
            long snapshotCount = points.stream()
                    .mapToLong(point -> ((Number) point.get("count")).longValue())
                    .sum();
            max = Math.max(max, snapshotCount);
        }
        return max;
    }
}
