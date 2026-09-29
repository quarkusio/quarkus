package io.quarkus.dev.telemetry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Builds a {@link TelemetrySignals#SPAN span} event: one finished span of a trace. Obtained from
 * {@link TelemetryEvent#span(String)}. The event's timestamp is the span's end.
 * <p>
 * Everything the builder puts in the event is already immutable and JSON compatible, so the event takes it without
 * copying or checking it again. The span's attributes and events are copied when they are set.
 */
public final class SpanEventBuilder {

    private final String name;
    private final Map<String, Object> attributes = new LinkedHashMap<>();
    private long endEpochNanos;

    SpanEventBuilder(String name) {
        this.name = name;
    }

    public SpanEventBuilder traceId(String traceId) {
        attributes.put(TelemetryAttributes.TRACE_ID, traceId);
        return this;
    }

    public SpanEventBuilder spanId(String spanId) {
        attributes.put(TelemetryAttributes.SPAN_ID, spanId);
        return this;
    }

    public SpanEventBuilder parentSpanId(String parentSpanId) {
        attributes.put(TelemetryAttributes.PARENT_SPAN_ID, parentSpanId);
        return this;
    }

    /** E.g. {@code SERVER}, {@code CLIENT} or {@code INTERNAL}. */
    public SpanEventBuilder kind(String kind) {
        attributes.put(TelemetryAttributes.KIND, kind);
        return this;
    }

    public SpanEventBuilder time(long startEpochNanos, long endEpochNanos) {
        attributes.put(TelemetryAttributes.START_EPOCH_NANOS, startEpochNanos);
        attributes.put(TelemetryAttributes.END_EPOCH_NANOS, endEpochNanos);
        this.endEpochNanos = endEpochNanos;
        return this;
    }

    /** E.g. {@code OK}, {@code ERROR} or {@code UNSET}, with an optional description. */
    public SpanEventBuilder status(String code, String description) {
        attributes.put(TelemetryAttributes.STATUS_CODE, code);
        attributes.put(TelemetryAttributes.STATUS_DESCRIPTION, description);
        return this;
    }

    /** The name of the instrumentation scope that created the span. */
    public SpanEventBuilder scope(String scope) {
        attributes.put(TelemetryAttributes.SCOPE, scope);
        return this;
    }

    public SpanEventBuilder serviceName(String serviceName) {
        attributes.put(TelemetryAttributes.SERVICE_NAME, serviceName);
        return this;
    }

    /** The span's own attributes, as strings. */
    public SpanEventBuilder spanAttributes(Map<String, String> spanAttributes) {
        if (spanAttributes != null && !spanAttributes.isEmpty()) {
            attributes.put(TelemetryAttributes.SPAN_ATTRIBUTES, CheckedAttributes.strings(spanAttributes));
        }
        return this;
    }

    /** The events recorded on the span, described as strings. */
    public SpanEventBuilder events(List<String> events) {
        if (events != null && !events.isEmpty()) {
            attributes.put(TelemetryAttributes.EVENTS, Collections.unmodifiableList(new ArrayList<>(events)));
        }
        return this;
    }

    public TelemetryEvent build() {
        long timestamp = endEpochNanos > 0 ? TimeUnit.NANOSECONDS.toMillis(endEpochNanos) : System.currentTimeMillis();
        // A copy, so that the builder can go on without changing the event it built.
        return new TelemetryEvent(TelemetrySignals.SPAN, name, timestamp,
                new CheckedAttributes(new LinkedHashMap<>(attributes)));
    }
}
