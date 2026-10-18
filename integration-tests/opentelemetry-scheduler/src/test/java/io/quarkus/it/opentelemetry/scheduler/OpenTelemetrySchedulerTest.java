package io.quarkus.it.opentelemetry.scheduler;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;

@QuarkusTest
public class OpenTelemetrySchedulerTest {

    private static long DURATION_IN_NANOSECONDS = 100_000_000; // Thread.sleep(100l) for each job

    @Test
    public void schedulerSpanTest() {
        // ensure that scheduled job is called
        assertCounter("/scheduler/count", 1, Duration.ofSeconds(3));
        // assert JobDefinition type scheduler
        assertCounter("/scheduler/count/job-definition", 1, Duration.ofSeconds(3));

        // ------- SPAN ASSERTS -------
        List<Map<String, Object>> spans = getSpans("myCounter", "myJobDefinition");

        assertJobSpan(spans, "myCounter", DURATION_IN_NANOSECONDS); // identity
        assertJobSpan(spans, "myJobDefinition", DURATION_IN_NANOSECONDS); // identity

        // errors
        assertErrorJobSpan(spans, "myFailedBasicScheduler", DURATION_IN_NANOSECONDS,
                "error occurred in myFailedBasicScheduler.");
        assertErrorJobSpan(spans, "myFailedJobDefinition", DURATION_IN_NANOSECONDS,
                "error occurred in myFailedJobDefinition.");

    }

    private void assertCounter(String counterPath, int expectedCount, Duration timeout) {
        await().atMost(timeout)
                .pollInterval(Duration.ofMillis(500))
                .until(() -> {
                    Response response = given().when().get(counterPath);
                    int code = response.statusCode();
                    if (code != 200) {
                        return false;
                    }
                    String body = response.asString();
                    int count = Integer.valueOf(body);
                    return count >= expectedCount;
                });

    }

    private List<Map<String, Object>> getSpans(String... expectedNames) {
        AtomicReference<List<Map<String, Object>>> ret = new AtomicReference<>(Collections.emptyList());
        await().atMost(15, SECONDS).until(() -> {
            List<Map<String, Object>> spans = get("/export").body().as(new TypeRef<>() {
            });
            for (String name : expectedNames) {
                if (spans.stream().filter(map -> map.get("name").equals(name)).findAny().isEmpty()) {
                    return false;
                }
            }
            ret.set(spans);
            return true;
        });
        return ret.get();
    }

    private void assertJobSpan(List<Map<String, Object>> spans, String expectedName, long expectedDuration) {
        assertNotNull(spans);
        assertFalse(spans.isEmpty());
        Map<String, Object> span = spans.stream().filter(map -> map.get("name").equals(expectedName)).findFirst().orElse(null);
        assertNotNull(span, "Span with name '" + expectedName + "' not found.");
        assertEquals(SpanKind.INTERNAL.toString(), span.get("kind"), "Span with name '" + expectedName + "' is not internal.");

        long start = (long) span.get("startEpochNanos");
        long end = (long) span.get("endEpochNanos");
        long delta = (end - start);
        assertTrue(delta >= expectedDuration,
                "Duration of span with name '" + expectedName +
                        "' is not longer than 100ms, actual duration: " + delta + " (ns)");
    }

    @SuppressWarnings("unchecked")
    private void assertErrorJobSpan(List<Map<String, Object>> spans, String expectedName, long expectedDuration,
            String expectedErrorMessage) {
        assertJobSpan(spans, expectedName, expectedDuration);
        Map<String, Object> span = spans.stream().filter(map -> map.get("name").equals(expectedName)).findFirst()
                .orElseThrow(AssertionError::new); // this assert should never be thrown, since we already checked it in `assertJobSpan`

        Map<String, Object> statusAttributes = (Map<String, Object>) span.get("status");
        assertNotNull(statusAttributes, "Span with name '" + expectedName + "' is not an ERROR");
        assertEquals(StatusCode.ERROR.toString(), statusAttributes.get("statusCode"),
                "Span with name '" + expectedName + "' is not an ERROR");
        Map<String, Object> exception = (Map<String, Object>) ((List<Map<String, Object>>) span.get("events")).stream()
                .map(map -> map.get("exception")).findFirst().orElseThrow(AssertionError::new);
        assertTrue(((String) exception.get("message")).contains(expectedErrorMessage),
                "Span with name '" + expectedName + "' has wrong error message");
    }
}
