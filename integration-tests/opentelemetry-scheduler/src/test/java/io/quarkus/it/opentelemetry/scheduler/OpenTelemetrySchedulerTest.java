package io.quarkus.it.opentelemetry.scheduler;

import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_MESSAGE;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.ExceptionEventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.opentelemetry.SpanDataList;
import io.quarkus.test.opentelemetry.SpanExporterClient;
import io.restassured.response.Response;

@QuarkusTest
public class OpenTelemetrySchedulerTest {

    @TestHTTPResource
    URI applicationUri;

    SpanExporterClient spanExporter() {
        return SpanExporterClient.at(applicationUri);
    }

    private static long DURATION_IN_NANOSECONDS = 100_000_000; // Thread.sleep(100l) for each job

    @Test
    public void schedulerSpanTest() {
        // ensure that scheduled job is called
        assertCounter("/scheduler/count", 1, Duration.ofSeconds(3));
        // assert JobDefinition type scheduler
        assertCounter("/scheduler/count/job-definition", 1, Duration.ofSeconds(3));

        // ------- SPAN ASSERTS -------
        SpanDataList spans = getSpans("myCounter", "myJobDefinition");

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

    private SpanDataList getSpans(String... expectedNames) {
        return scheduledSpans(spanExporter().awaitUntil("spans named " + Arrays.toString(expectedNames), Duration.ofSeconds(15),
                fetched -> {
                    SpanDataList spans = scheduledSpans(fetched);
                    for (String name : expectedNames) {
                        if (spans.named(name).isEmpty()) {
                            return false;
                        }
                    }
                    return true;
                }));
    }

    // only keep scheduled spans
    private static SpanDataList scheduledSpans(SpanDataList spans) {
        return spans.filter(span -> !span.getName().contains("export") && !span.getName().contains("GET"));
    }

    private void assertJobSpan(SpanDataList spans, String expectedName, long expectedDuration) {
        assertNotNull(spans);
        assertFalse(spans.isEmpty());
        SpanData span = spans.named(expectedName).stream().findFirst().orElse(null);
        assertNotNull(span, "SpanData with name '" + expectedName + "' not found.");
        assertEquals(SpanKind.INTERNAL, span.getKind(), "SpanData with name '" + expectedName + "' is not internal.");

        long delta = span.getEndEpochNanos() - span.getStartEpochNanos();
        assertTrue(delta >= expectedDuration,
                "Duration of span with name '" + expectedName +
                        "' is not longer than 100ms, actual duration: " + delta + " (ns)");
    }

    private void assertErrorJobSpan(SpanDataList spans, String expectedName, long expectedDuration,
            String expectedErrorMessage) {
        assertJobSpan(spans, expectedName, expectedDuration);
        SpanData span = spans.named(expectedName).stream().findFirst()
                .orElseThrow(AssertionError::new); // this assert should never be thrown, since we already checked it in `assertJobSpan`

        assertEquals(StatusCode.ERROR, span.getStatus().getStatusCode(),
                "SpanData with name '" + expectedName + "' is not an ERROR");
        EventData exception = span.getEvents().getFirst();
        ExceptionEventData recorded = assertInstanceOf(ExceptionEventData.class, exception);
        assertTrue(recorded.getException().getMessage().contains(expectedErrorMessage));
        assertEquals("exception", exception.getName());
        assertTrue(exception.getAttributes().get(EXCEPTION_MESSAGE).contains(expectedErrorMessage),
                "SpanData with name '" + expectedName + "' has wrong error message");
    }
}
