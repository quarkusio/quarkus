package io.quarkus.it.smallrye.graphql;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
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
        RestAssured.when().get("/q/metrics").then()
                .body(containsString(
                        String.format("mp_graphql_seconds_count{name=\"%s\",source=\"%b\",type=\"%S\"} %d", name, source, type,
                                count)));
    }

    private void assertMetricTotalValue(String name, boolean source, String type, double minimumDuration) {
        String endpoint = "/q/metrics";
        String failureMessage = String.format(
                "Expected metric with name '%s', source '%b', type '%s', and total minimum of %f to be present in the response body of endpoint '%s'",
                name, source, type, minimumDuration, endpoint);
        assertThat(failureMessage,
                RestAssured.when().get(endpoint).asString(),
                new TotalMetricMatcher(name, source, type, minimumDuration));
    }

    private void assertMetricMaxValue(String name, boolean source, String type, double minimumDuration) {
        // mean would be better, but it is harder to get...
        String endpoint = "/q/metrics";
        String failureMessage = String.format(
                "Expected metric with name '%s', source '%b', type '%s', and at least or equal to maximum duration of %f to be present in the response body of endpoint '%s'",
                name, source, type, minimumDuration, endpoint);
        assertThat(failureMessage,
                RestAssured.when().get(endpoint).asString(),
                new MaxMetricMatcher(name, source, type, minimumDuration));
    }

    private void assertMetricExists(String name, boolean source, String type) {
        RestAssured.when().get("/q/metrics").then()
                .body(containsString(
                        String.format("mp_graphql_seconds_count{name=\"%s\",source=\"%b\",type=\"%S\"}", name, source, type)));
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

    abstract static class MetricMatcher extends TypeSafeMatcher<String> {
        protected final String name;
        protected final boolean source;
        protected final String type;
        protected final double value;

        protected MetricMatcher(String name, boolean source, String type, double value) {
            this.name = name;
            this.source = source;
            this.type = type;
            this.value = value;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("a metric with name ").appendValue(name)
                    .appendText(", source ").appendValue(source)
                    .appendText(", type ").appendValue(type)
                    .appendText(", and ").appendText(getValueDescription())
                    .appendValue(value);
        }

        protected abstract String getValueDescription();
    }

    static class TotalMetricMatcher extends MetricMatcher {
        public TotalMetricMatcher(String name, boolean source, String type, double total) {
            super(name, source, type, total);
        }

        @Override
        public boolean matchesSafely(String item) {
            String pattern = String.format("mp_graphql_seconds_sum\\{name=\"%s\",source=\"%b\",type=\"%s\"\\} \\d+\\.\\d+",
                    name, source, type);
            Matcher matcher = Pattern.compile(pattern).matcher(item);
            return matcher.find() && Double.parseDouble(matcher.group().split(" ")[1]) >= value;
        }

        @Override
        protected String getValueDescription() {
            return "total minimum of ";
        }
    }

    static class MaxMetricMatcher extends MetricMatcher {
        public MaxMetricMatcher(String name, boolean source, String type, double maxDuration) {
            super(name, source, type, maxDuration);
        }

        @Override
        public boolean matchesSafely(String item) {
            String pattern = String.format("mp_graphql_seconds_max\\{name=\"%s\",source=\"%b\",type=\"%s\"\\} \\d+\\.\\d+",
                    name, source, type);
            Matcher matcher = Pattern.compile(pattern).matcher(item);
            return matcher.find() && Double.parseDouble(matcher.group().split(" ")[1]) >= value;
        }

        @Override
        protected String getValueDescription() {
            return "maximum duration of ";
        }
    }

}
