package io.quarkus.opentelemetry.runtime.devui;

import io.quarkus.devui.observability.store.CountBoundingStrategy;
import io.quarkus.devui.observability.store.TelemetryStore;

/**
 * Dev-mode holder that keeps the captured traces store alive across live reloads.
 *
 * On a dev-mode live reload Quarkus recreates the application's runtime classloader
 * (and with it the ArC container and all beans), but the OpenTelemetry runtime jar is
 * an immutable dependency loaded by the base runtime classloader, which is reused
 * across app-code reloads. Keeping the store in a static field here therefore lets the
 * captured spans survive a reload, even though {@link DevUiTracesStoreProducer} runs
 * again and produces a fresh bean each time. The state is discarded only on a full dev
 * restart (e.g. a dependency or build-time config change) or JVM exit.
 *
 * This class is only ever touched in dev mode (the producer that calls it is registered
 * exclusively by the dev-only build step), so it adds no production footprint.
 */
public final class DevUiTracesStoreHolder {

    private static TelemetryStore<SpanRecord> instance;
    private static int capacity;

    private DevUiTracesStoreHolder() {
    }

    /**
     * Returns the surviving store, creating it on first use. If the configured capacity
     * changed since the store was created, a new (empty) store is built to honor the new
     * size — otherwise the existing store (and its captured spans) is reused across the
     * reload.
     */
    public static synchronized TelemetryStore<SpanRecord> getOrCreate(int capacity) {
        if (instance == null || DevUiTracesStoreHolder.capacity != capacity) {
            instance = new TelemetryStore<>(new CountBoundingStrategy(capacity));
            DevUiTracesStoreHolder.capacity = capacity;
        }
        return instance;
    }
}
