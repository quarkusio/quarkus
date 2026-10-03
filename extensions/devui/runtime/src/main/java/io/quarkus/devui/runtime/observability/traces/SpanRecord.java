package io.quarkus.devui.runtime.observability.traces;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.dev.telemetry.TelemetryAttributes;
import io.quarkus.dev.telemetry.TelemetryEvent;
import io.quarkus.devui.runtime.observability.telemetry.StringValues;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Immutable, serialization-friendly view of a finished span, captured for the Dev UI.
 * Built from a span {@link TelemetryEvent}, so it knows nothing of the tracer that produced the span.
 */
public record SpanRecord(
        String traceId,
        String spanId,
        String parentSpanId,
        String name,
        String kind,
        long startEpochNanos,
        long endEpochNanos,
        long durationNanos,
        String statusCode,
        String statusDescription,
        String scopeName,
        String resourceServiceName,
        Map<String, String> attributes,
        List<String> events) {

    /**
     * Reads a span back out of the event its tracer fired. The event's attributes are JSON by contract, so each one
     * is read defensively: a sender that left one out gets an empty value rather than a failure.
     */
    public static SpanRecord from(TelemetryEvent event) {
        Map<String, Object> a = event.attributes();
        long start = number(a.get(TelemetryAttributes.START_EPOCH_NANOS));
        long end = number(a.get(TelemetryAttributes.END_EPOCH_NANOS));
        return new SpanRecord(
                text(a.get(TelemetryAttributes.TRACE_ID)),
                text(a.get(TelemetryAttributes.SPAN_ID)),
                text(a.get(TelemetryAttributes.PARENT_SPAN_ID)),
                event.name(),
                text(a.get(TelemetryAttributes.KIND)),
                start,
                end,
                end - start,
                text(a.get(TelemetryAttributes.STATUS_CODE)),
                text(a.get(TelemetryAttributes.STATUS_DESCRIPTION)),
                text(a.get(TelemetryAttributes.SCOPE)),
                text(a.get(TelemetryAttributes.SERVICE_NAME)),
                StringValues.map(a.get(TelemetryAttributes.SPAN_ATTRIBUTES)),
                StringValues.list(a.get(TelemetryAttributes.EVENTS)));
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private static long number(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    public JsonObject toJson() {
        JsonObject attrsJson = new JsonObject();
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            attrsJson.put(entry.getKey(), entry.getValue());
        }
        return new JsonObject()
                .put("traceId", traceId)
                .put("spanId", spanId)
                .put("parentSpanId", parentSpanId)
                .put("name", name)
                .put("kind", kind)
                .put("startEpochNanos", startEpochNanos)
                .put("endEpochNanos", endEpochNanos)
                .put("durationNanos", durationNanos)
                .put("statusCode", statusCode)
                .put("statusDescription", statusDescription)
                .put("scopeName", scopeName)
                .put("resourceServiceName", resourceServiceName)
                .put("attributes", attrsJson)
                .put("events", new JsonArray(events));
    }

    /**
     * Group spans by traceId, newest trace first, each with its span list and the
     * [windowStart, windowEnd] time window used to lay out the waterfall.
     */
    public static JsonArray group(List<SpanRecord> spans) {
        // Preserve insertion order per trace; order traces by earliest start descending.
        Map<String, JsonObject> byTrace = new LinkedHashMap<>();
        for (SpanRecord s : spans) {
            JsonObject trace = byTrace.get(s.traceId());
            if (trace == null) {
                trace = new JsonObject()
                        .put("traceId", s.traceId())
                        .put("windowStart", Long.MAX_VALUE)
                        .put("windowEnd", Long.MIN_VALUE)
                        .put("spans", new JsonArray());
                byTrace.put(s.traceId(), trace);
            }
            trace.put("windowStart", Math.min(trace.getLong("windowStart"), s.startEpochNanos()));
            trace.put("windowEnd", Math.max(trace.getLong("windowEnd"), s.endEpochNanos()));
            trace.getJsonArray("spans").add(s.toJson());
        }
        List<JsonObject> traces = new ArrayList<>(byTrace.values());
        traces.sort((a, b) -> Long.compare(b.getLong("windowStart"), a.getLong("windowStart")));
        return new JsonArray(traces);
    }
}
