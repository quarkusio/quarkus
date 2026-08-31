package io.quarkus.opentelemetry.runtime.devui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.ServiceAttributes;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Immutable, serialization-friendly view of a finished span, captured for the Dev UI.
 * Deliberately decoupled from the OTel SDK types so it can be buffered and streamed.
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

    // NOTE: from() runs on the span-completion path (often the request thread), so it
    // deliberately avoids streams/lambdas to keep the capture path lean (both footprint
    // and per-span allocation). Plain loops only here.
    public static SpanRecord from(SpanData data) {
        Map<String, String> attrs = new LinkedHashMap<>();
        for (Map.Entry<AttributeKey<?>, Object> entry : data.getAttributes().asMap().entrySet()) {
            attrs.put(entry.getKey().getKey(), String.valueOf(entry.getValue()));
        }

        List<EventData> spanEvents = data.getEvents();
        List<String> events = new ArrayList<>(spanEvents.size());
        for (EventData event : spanEvents) {
            events.add(describeEvent(event));
        }

        String serviceName = data.getResource().getAttribute(ServiceAttributes.SERVICE_NAME);

        return new SpanRecord(
                data.getTraceId(),
                data.getSpanId(),
                data.getParentSpanId(),
                data.getName(),
                data.getKind().name(),
                data.getStartEpochNanos(),
                data.getEndEpochNanos(),
                data.getEndEpochNanos() - data.getStartEpochNanos(),
                data.getStatus().getStatusCode().name(),
                data.getStatus().getDescription(),
                data.getInstrumentationScopeInfo().getName(),
                serviceName == null ? "" : serviceName,
                attrs,
                events);
    }

    private static String describeEvent(EventData e) {
        return e.getName() + " @" + e.getEpochNanos();
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
