package io.quarkus.it.smallrye.graphql;

import static io.quarkus.test.micrometer.PrometheusMetricsAssert.assertMetrics;
import static org.assertj.core.api.Assertions.entry;
import static org.hamcrest.CoreMatchers.containsString;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.it.smallrye.graphql.metricresources.TestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class MicrometerMetricsTest {

    private static final double SLEEP_TIME = TestResource.SLEEP_TIME;

    @AfterEach
    void resetGlobalMeterRegistry() {
        String request = getPayload("mutation { clearMetrics }");
        RestAssured.given().when()
                .accept("application/json")
                .contentType("application/json")
                .body(request)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200);
    }

    // Run a Query and check that its corresponding metric is updated
    @Test
    public void shouldCreateMetricFromQuery() {
        String request = getPayload("{\n" +
                "  superMetricFoo {\n" +
                "    message\n" +
                "  }\n" +
                "}");
        assertResponse(request,
                "{\"data\":{\"superMetricFoo\":[{\"message\":\"bar\"},{\"message\":\"bar\"},{\"message\":\"bar\"}]}}");
        assertMetric("superMetricFoo", false, "QUERY", 1l);
    }

    @Test
    public void shouldCreateMetricsFromQueryAndSourceField() {
        String request = getPayload("{\n" +
                "  superMetricFoo {\n" +
                "    randomNumber {\n" +
                "       value\n" +
                "    }\n" +
                "  }\n" +
                "}");

        assertResponse(request,
                "{\"data\":{\"superMetricFoo\":[{\"randomNumber\":{\"value\":123.0}},{\"randomNumber\":{\"value\":123.0}},{\"randomNumber\":{\"value\":123.0}}]}}");
        assertMetric("superMetricFoo", false, "QUERY", 1l);
        assertMetric("randomNumber", true, "QUERY", 3l);

    }

    @Test
    public void shouldCreateMetricFromAsyncQuery() {
        String request = getPayload("{\n" +
                "  asyncSuperMetricFoo {\n" +
                "    message\n" +
                "  }\n" +
                "}");
        assertResponse(request,
                "{\"data\":{\"asyncSuperMetricFoo\":[{\"message\":\"async1\"},{\"message\":\"async2\"},{\"message\":\"async3\"}]}}");
        assertMetric("asyncSuperMetricFoo", false, "QUERY", 1l);
    }

    @Test
    public void shouldCreateMetricsFromAsyncQueryAndSourceField() {
        String request = getPayload("{\n" +
                "  asyncSuperMetricFoo {\n" +
                "    randomNumber {\n" +
                "       value\n" +
                "    }\n" +
                "  }\n" +
                "}");

        assertResponse(request,
                "{\"data\":{\"asyncSuperMetricFoo\":[{\"randomNumber\":{\"value\":123.0}},{\"randomNumber\":{\"value\":123.0}},{\"randomNumber\":{\"value\":123.0}}]}}");
        assertMetric("asyncSuperMetricFoo", false, "QUERY", 1l);
        assertMetric("randomNumber", true, "QUERY", 3l);
    }

    @Test
    public void shouldCreateMetricsFromQueryAndAsyncSourceField() {
        String request = getPayload("{\n" +
                "  superMetricFoo {\n" +
                "    randomNumberAsync {\n" +
                "       value\n" +
                "    }\n" +
                "  }\n" +
                "}");

        assertResponse(request,
                "{\"data\":{\"superMetricFoo\":[{\"randomNumberAsync\":{\"value\":123.0}},{\"randomNumberAsync\":{\"value\":123.0}},{\"randomNumberAsync\":{\"value\":123.0}}]}}");
        assertMetric("superMetricFoo", false, "QUERY", 1l);
        assertMetric("randomNumberAsync", true, "QUERY", 3l);
    }

    @Test
    public void shouldCreateMetricsFromAsyncQueryAndAsyncSourceField() {
        String request = getPayload("{\n" +
                "  asyncSuperMetricFoo {\n" +
                "    randomNumberAsync {\n" +
                "       value\n" +
                "    }\n" +
                "  }\n" +
                "}");

        assertResponse(request,
                "{\"data\":{\"asyncSuperMetricFoo\":[{\"randomNumberAsync\":{\"value\":123.0}},{\"randomNumberAsync\":{\"value\":123.0}},{\"randomNumberAsync\":{\"value\":123.0}}]}}");
        assertMetric("asyncSuperMetricFoo", false, "QUERY", 1l);
        assertMetric("randomNumberAsync", true, "QUERY", 3l);
    }

    @Test
    public void shouldCreateMetricsFromQueryAndBatch() {
        String request = getPayload("{\n" +
                "  superMetricFoo {\n" +
                "    message\n" +
                "    batchFoo {\n" +
                "       message\n" +
                "    }\n" +
                "  }\n" +
                "}");

        assertResponse(request,
                "{\"data\":{\"superMetricFoo\":[{\"message\":\"bar\",\"batchFoo\":{\"message\":\"bar1\"}},{\"message\":\"bar\",\"batchFoo\":{\"message\":\"bar2\"}},{\"message\":\"bar\",\"batchFoo\":{\"message\":\"bar3\"}}]}}");
        assertMetric("superMetricFoo", false, "QUERY", 1l);
        assertBatchMetric("batchFoo", "QUERY", 3l);
    }

    @Test
    public void shouldCreateMetricsFromAsyncQueryAndBatch() {
        String request = getPayload("{\n" +
                "  asyncSuperMetricFoo {\n" +
                "    message\n" +
                "    batchFoo {\n" +
                "       message\n" +
                "    }\n" +
                "  }\n" +
                "}");

        assertResponse(request,
                "{\"data\":{\"asyncSuperMetricFoo\":[{\"message\":\"async1\",\"batchFoo\":{\"message\":\"bar1\"}},{\"message\":\"async2\",\"batchFoo\":{\"message\":\"bar2\"}},{\"message\":\"async3\",\"batchFoo\":{\"message\":\"bar3\"}}]}}");
        assertMetric("asyncSuperMetricFoo", false, "QUERY", 1l);
        assertBatchMetric("batchFoo", "QUERY", 3l);

    }

    @Test
    public void shouldCreateMetricsFromQueryAndAsyncBatch() {
        String request = getPayload("{\n" +
                "  superMetricFoo {\n" +
                "    message\n" +
                "    asyncBatchFoo {\n" +
                "       message\n" +
                "    }\n" +
                "  }\n" +
                "}");

        assertResponse(request,
                "{\"data\":{\"superMetricFoo\":[{\"message\":\"bar\",\"asyncBatchFoo\":{\"message\":\"abar1\"}},{\"message\":\"bar\",\"asyncBatchFoo\":{\"message\":\"abar2\"}},{\"message\":\"bar\",\"asyncBatchFoo\":{\"message\":\"abar3\"}}]}}");
        assertMetric("superMetricFoo", false, "QUERY", 1l);
        assertBatchMetric("asyncBatchFoo", "QUERY", 3l);
    }

    @Test
    public void shouldCreateMetricsFromAsyncQueryAndAsyncBatch() {
        String request = getPayload("{\n" +
                "  asyncSuperMetricFoo {\n" +
                "    message\n" +
                "    asyncBatchFoo {\n" +
                "       message\n" +
                "    }\n" +
                "  }\n" +
                "}");

        assertResponse(request,
                "{\"data\":{\"asyncSuperMetricFoo\":[{\"message\":\"async1\",\"asyncBatchFoo\":{\"message\":\"abar1\"}},{\"message\":\"async2\",\"asyncBatchFoo\":{\"message\":\"abar2\"}},{\"message\":\"async3\",\"asyncBatchFoo\":{\"message\":\"abar3\"}}]}}");
        assertMetric("asyncSuperMetricFoo", false, "QUERY", 1l);
        assertBatchMetric("asyncBatchFoo", "QUERY", 3l);

    }

    @Test
    void shouldCreateMultipleMetrics() throws ExecutionException, InterruptedException {
        String request = getPayload("{\n" +
                "  asyncSuperMetricFoo {\n" +
                "    message\n" +
                "  }\n" +
                "}");
        ExecutorService executor = Executors.newFixedThreadPool(50);
        int iterations = 200;
        try {
            CompletableFuture<Void>[] futures = new CompletableFuture[iterations];
            for (int i = 0; i < iterations; i++) {
                futures[i] = CompletableFuture.supplyAsync(() -> assertResponse(request,
                        "{\"data\":{\"asyncSuperMetricFoo\":[{\"message\":\"async1\"},{\"message\":\"async2\"},{\"message\":\"async3\"}]}"),
                        executor);
            }
            getTestResult(futures, iterations);
        } finally {
            executor.shutdown();
        }
    }

    private void getTestResult(CompletableFuture<Void>[] futures, long iterations)
            throws InterruptedException, ExecutionException {
        CompletableFuture.allOf(futures).get();
        assertMetric("asyncSuperMetricFoo", false, "QUERY", iterations);
    }

    private String getPayload(String query) {
        JsonObject jsonObject = createRequestBody(query);
        return jsonObject.toString();
    }

    private JsonObject createRequestBody(String graphQL) {
        return createRequestBody(graphQL, null);
    }

    private JsonObject createRequestBody(String graphQL, JsonObject variables) {
        // Create the request
        if (variables == null || variables.isEmpty()) {
            variables = Json.createObjectBuilder().build();
        }
        return Json.createObjectBuilder().add("query", graphQL).add("variables", variables).build();
    }

    private void assertMetric(String name, boolean source, String type, long count) {
        assertMetricWrapper(name, source, type, count, false);
    }

    private void assertBatchMetric(String name, String type, long count) {
        assertMetricWrapper(name, true, type, count, true);
    }

    private void assertMetricWrapper(String name, boolean source, String type, long count, boolean batch) {
        assertMetricExists(name, source, type);
        assertMetricCountValue(name, source, type, count);
        assertMetricMaxValue(name, source, type, SLEEP_TIME);
        assertMetricTotalValue(name, source, type, (batch ? 1 : count) * SLEEP_TIME);
    }

    private void assertMetricCountValue(String name, boolean source, String type, long count) {
        assertMetrics(RestAssured.when().get("/q/metrics").then()
                .extract().asInputStream())
                .hasMetricWithExactLabelsAndValue("mp_graphql_seconds_count", (double) count,
                        entry("name", name),
                        entry("source", String.valueOf(source)),
                        entry("type", type.toUpperCase()));
    }

    private void assertMetricTotalValue(String name, boolean source, String type, double minimumDuration) {
        assertMetrics(RestAssured.when().get("/q/metrics").then()
                .extract().asInputStream())
                .hasMetricWithLabelsAndValueGreaterThanOrEqualTo("mp_graphql_seconds_sum", minimumDuration,
                        entry("name", name),
                        entry("source", String.valueOf(source)),
                        entry("type", type));
    }

    private void assertMetricMaxValue(String name, boolean source, String type, double minimumDuration) {
        assertMetrics(RestAssured.when().get("/q/metrics").then()
                .extract().asInputStream())
                .hasMetricWithLabelsAndValueGreaterThanOrEqualTo("mp_graphql_seconds_max", minimumDuration,
                        entry("name", name),
                        entry("source", String.valueOf(source)),
                        entry("type", type));
    }

    private void assertMetricExists(String name, boolean source, String type) {
        assertMetrics(RestAssured.when().get("/q/metrics").then()
                .extract().asInputStream())
                .hasMetricWithExactLabels("mp_graphql_seconds_count",
                        entry("name", name),
                        entry("source", String.valueOf(source)),
                        entry("type", type.toUpperCase()));
    }

    private Void assertResponse(String request, String response) {
        RestAssured.given().when()
                .accept("application/json")
                .contentType("application/json")
                .body(request)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body(containsString(response));

        return null;
    }

}
