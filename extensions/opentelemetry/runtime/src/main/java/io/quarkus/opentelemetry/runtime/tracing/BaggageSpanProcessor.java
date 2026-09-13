package io.quarkus.opentelemetry.runtime.tracing;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageEntry;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

/**
 * Copies the baggage entries present in the context in which a span is started to the attributes of that span.
 * <p>
 * Baggage is propagated across services but, per the OpenTelemetry specification, it is not recorded on spans; this
 * processor makes the selected entries searchable in the tracing backend.
 */
public final class BaggageSpanProcessor implements SpanProcessor {

    private final String prefix;
    private final Set<String> keys;

    /**
     * @param prefix the prefix of the attribute names
     * @param keys the baggage keys to copy, or empty to copy every entry
     */
    public BaggageSpanProcessor(String prefix, Optional<List<String>> keys) {
        this.prefix = prefix;
        this.keys = keys.map(Set::copyOf).orElse(null);
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        Map<String, BaggageEntry> entries = Baggage.fromContext(parentContext).asMap();
        if (entries.isEmpty()) {
            return;
        }
        for (Map.Entry<String, BaggageEntry> entry : entries.entrySet()) {
            if (keys == null || keys.contains(entry.getKey())) {
                span.setAttribute(prefix + entry.getKey(), entry.getValue().getValue());
            }
        }
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {
    }

    @Override
    public boolean isEndRequired() {
        return false;
    }
}
