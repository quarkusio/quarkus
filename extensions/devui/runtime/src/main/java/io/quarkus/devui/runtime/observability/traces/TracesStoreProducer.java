package io.quarkus.devui.runtime.observability.traces;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkus.devui.observability.store.CountBoundingStrategy;
import io.quarkus.devui.observability.store.TelemetryStore;
import io.quarkus.devui.runtime.observability.traces.config.TracesDevUiRuntimeConfig;

/**
 * The store behind the traces view. Whichever tracer the application uses fires its finished spans as events; the
 * router records them here.
 * <p>
 * The store is kept in a static field so the spans a developer is looking at survive a live reload - unless the
 * configured capacity changed, which needs a store of the new size.
 * <p>
 * NOTE: NO class-level scope annotation on purpose: registered only by the dev-only build step.
 */
public class TracesStoreProducer {

    private static TelemetryStore<SpanRecord> instance;
    private static int capacity;

    @Produces
    @Singleton
    public TelemetryStore<SpanRecord> tracesStore(TracesDevUiRuntimeConfig config) {
        return getOrCreate(config.capacity());
    }

    static synchronized TelemetryStore<SpanRecord> getOrCreate(int capacity) {
        if (instance == null || TracesStoreProducer.capacity != capacity) {
            instance = new TelemetryStore<>(new CountBoundingStrategy(capacity));
            TracesStoreProducer.capacity = capacity;
        }
        return instance;
    }
}
