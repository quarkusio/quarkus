package io.quarkus.opentelemetry.deployment.devui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.opentelemetry.runtime.devui.SpanRecord;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

class SpanRecordGroupingTest {

    private static SpanRecord span(String traceId, String spanId, String parentId,
            String name, long start, long end) {
        return new SpanRecord(traceId, spanId, parentId, name, "INTERNAL",
                start, end, end - start, "OK", "", "test-scope", "test-service",
                Map.of(), List.of());
    }

    @Test
    void toJsonExposesCoreFields() {
        JsonObject json = span("t1", "s1", "", "GET /", 10, 40).toJson();
        assertThat(json.getString("traceId")).isEqualTo("t1");
        assertThat(json.getString("spanId")).isEqualTo("s1");
        assertThat(json.getString("name")).isEqualTo("GET /");
        assertThat(json.getLong("durationNanos")).isEqualTo(30L);
    }

    @Test
    void groupBundlesSpansByTraceWithWindow() {
        List<SpanRecord> spans = List.of(
                span("t1", "s1", "", "GET /", 10, 40),
                span("t1", "s2", "s1", "db.query", 15, 25),
                span("t2", "s3", "", "GET /other", 100, 120));

        JsonArray traces = SpanRecord.group(spans);

        assertThat(traces).hasSize(2);
        JsonObject t1 = findTrace(traces, "t1");
        assertThat(t1.getLong("windowStart")).isEqualTo(10L);
        assertThat(t1.getLong("windowEnd")).isEqualTo(40L);
        assertThat(t1.getJsonArray("spans")).hasSize(2);
    }

    private static JsonObject findTrace(JsonArray traces, String traceId) {
        return traces.stream()
                .map(JsonObject.class::cast)
                .filter(t -> traceId.equals(t.getString("traceId")))
                .findFirst().orElseThrow();
    }
}
