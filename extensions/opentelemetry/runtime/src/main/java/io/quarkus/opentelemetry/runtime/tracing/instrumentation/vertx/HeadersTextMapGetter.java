package io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.opentelemetry.context.propagation.TextMapGetter;

/**
 * Reads propagation headers from the entries Vert.x hands to a tracer on the receiving side, so a parent
 * carried by an incoming message can be found before deciding whether to start a span.
 */
enum HeadersTextMapGetter implements TextMapGetter<Iterable<Map.Entry<String, String>>> {
    INSTANCE;

    @Override
    public Iterable<String> keys(final Iterable<Map.Entry<String, String>> headers) {
        if (headers == null) {
            return List.of();
        }
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, String> header : headers) {
            keys.add(header.getKey());
        }
        return keys;
    }

    @Override
    public String get(final Iterable<Map.Entry<String, String>> headers, final String key) {
        if (headers == null) {
            return null;
        }
        for (Map.Entry<String, String> header : headers) {
            if (header.getKey().equalsIgnoreCase(key)) {
                return header.getValue();
            }
        }
        return null;
    }
}
