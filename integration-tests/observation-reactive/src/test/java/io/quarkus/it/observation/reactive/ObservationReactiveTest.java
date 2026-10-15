package io.quarkus.it.observation.reactive;

import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.SpanKind;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
public class ObservationReactiveTest {

    private static final Logger logger = Logger.getLogger(ObservationReactiveTest.class);

    @BeforeEach
    @AfterEach
    void reset() {
        given().get("/reset").then().statusCode(HTTP_OK);
        await().atMost(5, SECONDS).until(() -> getSpans().isEmpty());
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
        List<Map<String, Object>> spans = waitForSpans(2);

        Map<String, Object> serverSpan = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertThat(serverSpan).isNotNull();
        assertThat(serverSpan.get("name")).isEqualTo("GET /reactive");

        Map<String, Object> observationSpan = getSpanByKindAndParentId(spans, INTERNAL, serverSpan.get("spanId"));
        assertThat(observationSpan).isNotNull();
        assertThat(observationSpan.get("name")).isEqualTo("reactive.hello.observation");
        assertThat(observationSpan.get("traceId")).isEqualTo(serverSpan.get("traceId"));
        Map attributes = (Map) observationSpan.get("attributes");
        assertThat(attributes.get("name")).isEqualTo("Naruto");

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
        List<Map<String, Object>> spans = waitForSpans(2);

        Map<String, Object> serverSpan = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertThat(serverSpan).isNotNull();

        Map<String, Object> observedSpan = getSpanByKindAndParentId(spans, INTERNAL, serverSpan.get("spanId"));
        assertThat(observedSpan).isNotNull();
        assertThat(observedSpan.get("traceId")).isEqualTo(serverSpan.get("traceId"));
        assertThat(((Map) observedSpan.get("attributes")).size()).isEqualTo(3);
        logger.infov("reactive.hello.observation Attributes: {0}", observedSpan.get("attributes"));

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
        List<Map<String, Object>> spans = waitForSpans(5);

        Map<String, Object> serverSpan = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertThat(serverSpan).isNotNull();

        // Exactly one parent observation span (the @Observed stream) is the child of the SERVER span.
        List<Map<String, Object>> parentSpans = getSpansByKindAndParentId(spans, INTERNAL,
                serverSpan.get("spanId"));
        assertThat(parentSpans).hasSize(1);
        Map<String, Object> parentSpan = parentSpans.get(0);
        assertThat(parentSpan.get("traceId")).isEqualTo(serverSpan.get("traceId"));
        assertThat((String) parentSpan.get("name")).contains("reactiveStream");

        // Each of the three emitted items opened its own child observation, parented to the
        // stream observation - so the parent span has exactly one child span per element.
        List<Map<String, Object>> childSpans = getSpansByKindAndParentId(spans, INTERNAL,
                parentSpan.get("spanId"));
        assertThat(childSpans).hasSize(3);
        assertThat(childSpans).allSatisfy(child -> {
            assertThat(child.get("traceId")).isEqualTo(serverSpan.get("traceId"));
            assertThat(child.get("name")).isEqualTo("stream.item.observation");
        });
        assertThat(childSpans)
                .extracting(child -> ((Map) child.get("attributes")).get("item"))
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
        List<Map<String, Object>> spans = waitForSpans(8);

        Map<String, Object> rootServer = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertThat(rootServer).isNotNull();
        assertThat(rootServer.get("name")).isEqualTo("GET /reactive/multiple-chain");

        Map<String, Object> chainObservation = getSpanByKindAndParentId(spans, INTERNAL,
                rootServer.get("spanId"));
        assertThat(chainObservation).isNotNull();
        assertThat(chainObservation.get("name")).isEqualTo("chain.operation.observation");
        assertThat(chainObservation.get("traceId")).isEqualTo(rootServer.get("traceId"));

        List<Map<String, Object>> clientSpans = getSpansByKindAndParentId(spans, CLIENT,
                chainObservation.get("spanId"));
        assertThat(clientSpans).hasSize(2);

        for (Map<String, Object> clientSpan : clientSpans) {
            assertThat(clientSpan.get("traceId")).isEqualTo(rootServer.get("traceId"));

            // Each CLIENT span should have a child SERVER span (the handler)
            Map<String, Object> handlerServer = getSpanByKindAndParentId(spans, SERVER,
                    clientSpan.get("spanId"));
            assertThat(handlerServer).isNotNull();
            assertThat(handlerServer.get("name")).isEqualTo("GET /reactive");
            assertThat(handlerServer.get("traceId")).isEqualTo(rootServer.get("traceId"));

            // Each handler SERVER span should have a child INTERNAL span (reactive.hello.observation)
            Map<String, Object> handlerObservation = getSpanByKindAndParentId(spans, INTERNAL,
                    handlerServer.get("spanId"));
            assertThat(handlerObservation).isNotNull();
            assertThat(handlerObservation.get("name")).isEqualTo("reactive.hello.observation");
            assertThat(handlerObservation.get("traceId")).isEqualTo(rootServer.get("traceId"));
            logger.infov("reactive.hello.observation Attributes: {0}", handlerObservation.get("attributes"));
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
        List<Map<String, Object>> spans = waitForSpans(8);

        Map<String, Object> rootServer = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertThat(rootServer).isNotNull();

        Map<String, Object> combineObservation = getSpanByKindAndParentId(spans, INTERNAL,
                rootServer.get("spanId"));
        assertThat(combineObservation).isNotNull();
        assertThat(combineObservation.get("name")).isEqualTo("combine.operation.observation");
        assertThat(combineObservation.get("traceId")).isEqualTo(rootServer.get("traceId"));

        List<Map<String, Object>> clientSpans = getSpansByKindAndParentId(spans, CLIENT,
                combineObservation.get("spanId"));
        assertThat(clientSpans).hasSize(2);

        for (Map<String, Object> clientSpan : clientSpans) {
            assertThat(clientSpan.get("traceId")).isEqualTo(rootServer.get("traceId"));

            Map<String, Object> handlerServer = getSpanByKindAndParentId(spans, SERVER,
                    clientSpan.get("spanId"));
            assertThat(handlerServer).isNotNull();
            assertThat(handlerServer.get("name")).isEqualTo("GET /reactive");
            assertThat(handlerServer.get("traceId")).isEqualTo(rootServer.get("traceId"));

            Map<String, Object> handlerObservation = getSpanByKindAndParentId(spans, INTERNAL,
                    handlerServer.get("spanId"));
            assertThat(handlerObservation).isNotNull();
            assertThat(handlerObservation.get("name")).isEqualTo("reactive.hello.observation");
            assertThat(handlerObservation.get("traceId")).isEqualTo(rootServer.get("traceId"));
            logger.infov("reactive.hello.observation Attributes: {0}", handlerObservation.get("attributes"));
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
        List<Map<String, Object>> spans = waitForSpans(parallelRequests * 8);

        // Find all root SERVER spans (parentSpanId = 0)
        List<Map<String, Object>> rootServers = spans.stream()
                .filter(s -> SERVER.toString().equals(s.get("kind")))
                .filter(s -> "0000000000000000".equals(s.get("parentSpanId")))
                .filter(s -> "GET /reactive/multiple-combine".equals(s.get("name")))
                .toList();
        assertThat(rootServers).hasSize(parallelRequests);

        // Verify each request's span hierarchy is self-contained within its traceId
        for (Map<String, Object> rootServer : rootServers) {
            String traceId = (String) rootServer.get("traceId");

            List<Map<String, Object>> traceSpans = spans.stream()
                    .filter(s -> traceId.equals(s.get("traceId")))
                    .toList();
            assertThat(traceSpans).hasSize(8);

            // Root SERVER → INTERNAL (combine.operation.observation)
            Map<String, Object> combineObservation = getSpanByKindAndParentId(traceSpans, INTERNAL,
                    rootServer.get("spanId"));
            assertThat(combineObservation)
                    .withFailMessage("Missing observation span for traceId=" + traceId)
                    .isNotNull();
            assertThat(combineObservation.get("name")).isEqualTo("combine.operation.observation");

            // Observation → 2 CLIENT spans
            List<Map<String, Object>> clientSpans = getSpansByKindAndParentId(traceSpans, CLIENT,
                    combineObservation.get("spanId"));
            assertThat(clientSpans)
                    .withFailMessage("Expected 2 CLIENT spans for traceId=" + traceId)
                    .hasSize(2);

            for (Map<String, Object> clientSpan : clientSpans) {
                // CLIENT → SERVER (handler)
                Map<String, Object> handlerServer = getSpanByKindAndParentId(traceSpans, SERVER,
                        clientSpan.get("spanId"));
                assertThat(handlerServer)
                        .withFailMessage("Missing handler SERVER span for traceId=" + traceId)
                        .isNotNull();

                // SERVER (handler) → INTERNAL (reactive.hello.observation)
                Map<String, Object> handlerObservation = getSpanByKindAndParentId(traceSpans, INTERNAL,
                        handlerServer.get("spanId"));
                assertThat(handlerObservation)
                        .withFailMessage("Missing handler observation span for traceId=" + traceId)
                        .isNotNull();
                assertThat(handlerObservation.get("name")).isEqualTo("reactive.hello.observation");
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
        List<Map<String, Object>> spans = waitForSpans(3);

        Map<String, Object> serverSpan = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertThat(serverSpan).isNotNull();

        Map<String, Object> workerSpan = getSpanByKindAndParentId(spans, INTERNAL, serverSpan.get("spanId"));
        assertThat(workerSpan).isNotNull();
        assertThat(workerSpan.get("name")).isEqualTo("worker.parent.observation");

        // The child observation, produced on the worker thread, must be parented to the parent
        // observation (worker) opened on the request thread - proving the scope propagated across threads.
        Map<String, Object> childSpan = getSpanByKindAndParentId(spans, INTERNAL, workerSpan.get("spanId"));
        assertThat(childSpan).isNotNull();
        assertThat(childSpan.get("traceId")).isEqualTo(serverSpan.get("traceId"));
        assertThat((String) childSpan.get("name")).contains("doChildWork");

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
        List<Map<String, Object>> spans = waitForSpans(3);

        Map<String, Object> serverSpan = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertThat(serverSpan).isNotNull();

        Map<String, Object> parentSpan = getSpanByKindAndParentId(spans, INTERNAL, serverSpan.get("spanId"));
        assertThat(parentSpan).isNotNull();
        assertThat(parentSpan.get("name")).isEqualTo("virtual.parent.observation");

        Map<String, Object> childSpan = getSpanByKindAndParentId(spans, INTERNAL, parentSpan.get("spanId"));
        assertThat(childSpan).isNotNull();
        assertThat(childSpan.get("traceId")).isEqualTo(serverSpan.get("traceId"));
        assertThat((String) childSpan.get("name")).contains("doChildWork");

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
        List<Map<String, Object>> spans = waitForSpans(2);

        Map<String, Object> serverSpan = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertThat(serverSpan).isNotNull();

        Map<String, Object> errorSpan = getSpanByKindAndParentId(spans, INTERNAL, serverSpan.get("spanId"));
        assertThat(errorSpan).isNotNull();
        assertThat(errorSpan.get("name")).isEqualTo("error.operation.observation");
        assertThat(errorSpan.get("traceId")).isEqualTo(serverSpan.get("traceId"));

        // The failing observation is still recorded once by the timer.
        await().atMost(5, SECONDS).untilAsserted(() -> assertThat(getTimerCount("error.operation.observation"))
                .isGreaterThanOrEqualTo(1));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> waitForSpans(int minCount) {
        await().atMost(5, SECONDS).untilAsserted(() -> {
            List<Map<String, Object>> result = getSpans();
            assertThat(result).hasSizeGreaterThanOrEqualTo(minCount);
        });
        return getSpans();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getSpans() {
        return get("/export").body().as(new TypeRef<>() {
        });
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

    private Map<String, Object> getSpanByKindAndParentId(List<Map<String, Object>> spans,
            SpanKind kind, Object parentSpanId) {
        return spans.stream()
                .filter(s -> kind.toString().equals(s.get("kind")))
                .filter(s -> parentSpanId.equals(s.get("parentSpanId")))
                .findFirst()
                .orElse(null);
    }

    private List<Map<String, Object>> getSpansByKindAndParentId(List<Map<String, Object>> spans,
            SpanKind kind, Object parentSpanId) {
        return spans.stream()
                .filter(s -> kind.toString().equals(s.get("kind")))
                .filter(s -> parentSpanId.equals(s.get("parentSpanId")))
                .toList();
    }
}
