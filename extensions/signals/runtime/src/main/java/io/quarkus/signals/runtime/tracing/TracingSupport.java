package io.quarkus.signals.runtime.tracing;

import java.util.Collections;
import java.util.Map;

import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;

/**
 * Shared constants and W3C trace context carrier accessors used by the OpenTelemetry integration.
 * <p>
 * The propagated trace context is stored in the signal metadata under a single {@link #METADATA_KEY} entry whose value
 * is an immutable map of W3C Trace Context headers (e.g. {@code traceparent}). This keeps the OpenTelemetry-specific
 * keys out of the user-facing metadata namespace.
 */
final class TracingSupport {

    /**
     * The instrumentation name used for the {@link io.opentelemetry.api.trace.Tracer}.
     */
    static final String TRACER_NAME = "io.quarkus.signals";

    /**
     * The signal metadata key under which the W3C trace context map is stored.
     */
    static final String METADATA_KEY = "quarkus.tracing";

    static final TextMapSetter<Map<String, String>> SETTER = new TextMapSetter<Map<String, String>>() {
        @Override
        public void set(Map<String, String> carrier, String key, String value) {
            if (carrier != null) {
                carrier.put(key, value);
            }
        }
    };

    static final TextMapGetter<Map<String, String>> GETTER = new TextMapGetter<Map<String, String>>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier == null ? Collections.emptyList() : carrier.keySet();
        }

        @Override
        public String get(Map<String, String> carrier, String key) {
            return carrier == null ? null : carrier.get(key);
        }
    };

    private TracingSupport() {
    }
}
