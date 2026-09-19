package io.quarkus.opentelemetry.runtime.devui;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkus.devui.observability.store.TelemetryStore;
import io.quarkus.opentelemetry.runtime.config.runtime.TracesDevUiRuntimeConfig;

/**
 * Produces the single dev-mode traces store, sized from runtime config.
 *
 * This class lives in {@code quarkus-opentelemetry-dev}, a conditional dev dependency
 * of the OTel extension, so it is only ever on the application classpath in dev mode.
 *
 * NOTE: it additionally carries NO class-level bean-defining annotation (no
 * {@code @Dependent}/{@code @Singleton}). This jar is a bean archive (it ships a Jandex
 * index), so a scope here would make it an auto-discovered bean whenever the jar is
 * present — including dev runs where the traces capture is turned off. Instead this
 * class (and the processor + JSON-RPC service) is registered as a bean ONLY by the
 * dev-only build step via {@code AdditionalBeanBuildItem} with an explicit default
 * scope. The {@code @Produces} method still carries its own scope.
 */
public class DevUiTracesStoreProducer {

    @Produces
    @Singleton
    public TelemetryStore<SpanRecord> tracesStore(TracesDevUiRuntimeConfig config) {
        // Reuse a store that survives dev-mode live reloads, so captured spans are not
        // lost when the app (and this bean) is recreated. See DevUiTracesStoreHolder.
        return DevUiTracesStoreHolder.getOrCreate(config.capacity());
    }
}
