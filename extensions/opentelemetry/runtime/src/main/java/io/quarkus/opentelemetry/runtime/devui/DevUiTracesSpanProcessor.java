package io.quarkus.opentelemetry.runtime.devui;

import jakarta.inject.Inject;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.quarkus.devui.observability.store.TelemetryStore;

/**
 * Dev-mode-only, standalone SpanProcessor that captures finished spans into the
 * in-memory ring buffer for the Dev UI. Independent of the SDK's built-in
 * batch/simple processors. Registered as a CDI bean only in dev mode (via the
 * dev-only build step's {@code AdditionalBeanBuildItem}, which supplies the scope)
 * and picked up by the SDK via the {@code @All List<SpanProcessor>} collection.
 *
 * NOTE: NO class-level scope annotation on purpose — see the note on
 * {@link DevUiTracesStoreProducer}: a scope here would make this an auto-discovered
 * bean in prod/native and break the dev-only guard.
 *
 * onEnd runs on the app's span-completion path (often the request thread), so it must
 * not block: the store's buffer add is O(1) and the broadcast is best-effort / drops.
 */
public class DevUiTracesSpanProcessor implements SpanProcessor {

    private final TelemetryStore<SpanRecord> store;

    @Inject
    public DevUiTracesSpanProcessor(TelemetryStore<SpanRecord> store) {
        this.store = store;
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        // no-op: capture happens on end
    }

    @Override
    public boolean isStartRequired() {
        return false;
    }

    @Override
    public void onEnd(ReadableSpan span) {
        store.record(SpanRecord.from(span.toSpanData()));
    }

    @Override
    public boolean isEndRequired() {
        return true;
    }
}
