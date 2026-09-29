package io.quarkus.dev.telemetry;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

/**
 * A piece of telemetry for the dev mode tooling, fired as a CDI event:
 *
 * <pre>
 * &#064;Inject
 * Event&lt;TelemetryEvent&gt; telemetry;
 *
 * void sample() {
 *     if (TelemetryEvents.isEnabled()) {
 *         telemetry.fire(TelemetryEvent.metric("queue.depth").value(queue.size()).build());
 *     }
 * }
 * </pre>
 *
 * The envelope is deliberately generic: a {@link #signal() signal} saying what kind of telemetry it is, a
 * {@link #name() name}, a timestamp, and attributes. Nothing about how the telemetry is stored or shown is in it, so
 * a sender depends on this class only - which every Quarkus application already has - and a receiver routes by the
 * signal. The Dev UI observability dashboard receives {@link TelemetrySignals#METRIC metrics} and
 * {@link TelemetrySignals#SPAN spans}; see {@link #metric(String)} and {@link #span(String)} for building those.
 * <p>
 * Attribute values must be JSON compatible - {@code null}, a {@link String}, a {@link Number}, a {@link Boolean}, or a
 * {@link List} or {@link Map} (with {@link String} keys) of those - so that a receiver can handle any event without
 * knowing the sender's classes. Anything else is rejected when the event is created, where the mistake is made. The
 * attributes are copied, so the event is immutable. That includes the numbers: a mutable {@link Number}, such as an
 * {@link AtomicLong} or a {@link LongAdder}, is replaced by the value it holds when the event is created, and a number
 * that is not finite ({@code NaN} or an infinity) becomes {@code null}, since JSON cannot represent it.
 *
 * @param signal what kind of telemetry this is, e.g. {@link TelemetrySignals#METRIC}; never {@code null} or blank
 * @param name what happened or what was measured, e.g. a meter or span name; never {@code null} or blank
 * @param timestamp when it happened, in milliseconds since the epoch
 * @param attributes the details; see {@link TelemetryAttributes} for the keys receivers give a meaning to
 */
public record TelemetryEvent(String signal, String name, long timestamp, Map<String, Object> attributes) {

    public TelemetryEvent {
        requireText(signal, "signal");
        requireText(name, "name");
        if (!(attributes instanceof CheckedAttributes)) {
            // Already checked and immutable when it is: it came from a builder, or from another event.
            attributes = attributes == null || attributes.isEmpty() ? Map.of() : copy(attributes, "attributes");
        }
    }

    /**
     * Starts a {@link TelemetrySignals#METRIC metric} event: one reading of a meter.
     */
    public static MetricEventBuilder metric(String name) {
        return new MetricEventBuilder(name);
    }

    /**
     * Starts a {@link TelemetrySignals#SPAN span} event: one finished span of a trace.
     */
    public static SpanEventBuilder span(String name) {
        return new SpanEventBuilder(name);
    }

    private static void requireText(String value, String what) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("A telemetry event needs a " + what);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object copyValue(Object value, String path) {
        if (value == null || value instanceof String || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Number number) {
            return copyNumber(number);
        }
        if (value instanceof Map<?, ?> map) {
            for (Object key : map.keySet()) {
                if (!(key instanceof String)) {
                    throw new IllegalArgumentException("The keys of " + path + " must be strings, found " + key);
                }
            }
            return copy((Map<String, Object>) map, path);
        }
        if (value instanceof List<?> list) {
            Object[] copied = new Object[list.size()];
            for (int i = 0; i < copied.length; i++) {
                copied[i] = copyValue(list.get(i), path + "[" + i + "]");
            }
            // Not List.of: that rejects null elements, which JSON allows.
            return Collections.unmodifiableList(Arrays.asList(copied));
        }
        throw new IllegalArgumentException(path + " must be JSON compatible (a string, number, boolean, list, map or"
                + " null), found " + value.getClass().getName());
    }

    /**
     * An immutable copy of a number, or {@code null} for one that is not finite.
     */
    private static Number copyNumber(Number number) {
        if (number instanceof Double d) {
            return Double.isFinite(d) ? d : null;
        }
        if (number instanceof Float f) {
            return Float.isFinite(f) ? f : null;
        }
        if (number instanceof Long || number instanceof Integer || number instanceof Short || number instanceof Byte
                || number instanceof BigInteger || number instanceof BigDecimal) {
            return number;
        }
        // Anything else - an AtomicLong, a LongAdder, a Number of the sender's own - may still change: keep what it
        // holds now.
        if (number instanceof AtomicLong || number instanceof AtomicInteger || number instanceof LongAdder
                || number instanceof LongAccumulator) {
            return number.longValue();
        }
        double value = number.doubleValue();
        return Double.isFinite(value) ? value : null;
    }

    private static Map<String, Object> copy(Map<String, Object> map, String path) {
        // Not Map.copyOf: that rejects null values, which JSON allows, and loses the sender's order.
        Map<String, Object> copied = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            copied.put(Objects.requireNonNull(entry.getKey(), path + " cannot have a null key"),
                    copyValue(entry.getValue(), path + "." + entry.getKey()));
        }
        return new CheckedAttributes(copied);
    }
}
