package io.quarkus.dev.telemetry;

/**
 * The attribute keys of the signals in {@link TelemetrySignals}, so that senders and receivers agree on them. The
 * builders ({@link TelemetryEvent#metric(String)}, {@link TelemetryEvent#span(String)}) write them for you.
 */
public final class TelemetryAttributes {

    // ---- metric

    /** The primary statistic of a meter, a {@link Number}: the reading, or the recording count of a distribution. */
    public static final String VALUE = "value";

    /** The kind of meter, e.g. {@code GAUGE}, {@code COUNTER}, {@code TIMER}; it decides how the meter is charted. */
    public static final String TYPE = "type";

    /** Whether the value is a running total, so that the dashboard charts how fast it grows. */
    public static final String CUMULATIVE = "cumulative";

    /** The unit of the value (or, for a distribution, of the amounts recorded), e.g. {@code s} or {@code bytes}. */
    public static final String UNIT = "unit";

    /** The dimensions of a meter, a map of strings. Each combination is a series of its own. */
    public static final String TAGS = "tags";

    /** Who captured the meter, e.g. {@code micrometer} or {@code otel}. */
    public static final String SOURCE = "source";

    /** The sum of everything a distribution recorded. */
    public static final String TOTAL = "total";

    /** The largest amount a distribution recorded; absent when the meter does not track one. */
    public static final String MAX = "max";

    /** The percentiles of a distribution, a list of maps with {@code rank} (0 to 1) and {@code value}. */
    public static final String PERCENTILES = "percentiles";

    /** The upper bounds of a distribution's histogram buckets, a list of numbers. */
    public static final String BUCKET_BOUNDARIES = "bucketBoundaries";

    /** The recordings in each histogram bucket, one more than the boundaries: the last is above them all. */
    public static final String BUCKET_COUNTS = "bucketCounts";

    // ---- span

    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";
    public static final String PARENT_SPAN_ID = "parentSpanId";
    public static final String KIND = "kind";
    public static final String START_EPOCH_NANOS = "startEpochNanos";
    public static final String END_EPOCH_NANOS = "endEpochNanos";
    public static final String STATUS_CODE = "statusCode";
    public static final String STATUS_DESCRIPTION = "statusDescription";
    /** The name of the instrumentation scope that created the span. */
    public static final String SCOPE = "scope";
    public static final String SERVICE_NAME = "serviceName";
    /** The span's own attributes, a map of strings. */
    public static final String SPAN_ATTRIBUTES = "spanAttributes";
    /** The events recorded on the span, a list of strings. */
    public static final String EVENTS = "events";

    private TelemetryAttributes() {
    }
}
