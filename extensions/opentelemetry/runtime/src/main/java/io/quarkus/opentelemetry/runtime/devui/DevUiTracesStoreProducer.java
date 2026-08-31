package io.quarkus.opentelemetry.runtime.devui;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkus.devui.observability.store.TelemetryStore;
import io.quarkus.opentelemetry.runtime.config.runtime.TracesDevUiRuntimeConfig;

/**
 * Produces the single dev-mode traces store, sized from runtime config.
 *
 * NOTE: this class deliberately carries NO class-level bean-defining annotation
 * (no {@code @Dependent}/{@code @Singleton}). The OTel runtime jar is a bean
 * archive (it ships a Jandex index), so any class annotated with a scope here
 * would be auto-discovered as a bean in ALL modes — defeating the dev-only gate.
 * Instead this class (and the processor + JSON-RPC service) is registered as a
 * bean ONLY by the dev-only build step via {@code AdditionalBeanBuildItem} with an
 * explicit default scope. The {@code @Produces} method still carries its own scope.
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
