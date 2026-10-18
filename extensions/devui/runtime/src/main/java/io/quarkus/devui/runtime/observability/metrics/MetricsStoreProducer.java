package io.quarkus.devui.runtime.observability.metrics;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkus.devui.observability.store.metrics.MetricsStoreHolder;
import io.quarkus.devui.observability.store.metrics.MetricsTimeSeriesStore;
import io.quarkus.devui.runtime.observability.metrics.config.MetricsDevUiRuntimeConfig;

/**
 * Produces the single dev-mode metrics store, sized from runtime config and reused across
 * live reloads via {@link MetricsStoreHolder}. Directly mirrors {@code DevUiTracesStoreProducer}.
 *
 * NOTE: NO class-level bean-defining annotation on purpose. {@code quarkus-devui} runtime is
 * a jandex bean archive, so a scoped class here would be auto-discovered in ALL modes and leak
 * the dev-only store into prod/native. Instead this class is registered as a bean ONLY by the
 * dev-only build step (Task A9) via {@code AdditionalBeanBuildItem} with an explicit default
 * scope. The {@code @Produces} method still carries its own {@code @Singleton}.
 */
public class MetricsStoreProducer {

    @Produces
    @Singleton
    public MetricsTimeSeriesStore metricsStore(MetricsDevUiRuntimeConfig config) {
        MetricsTimeSeriesStore store = MetricsStoreHolder.getOrCreate(
                config.retention().toMillis(),
                config.maxPointsPerSeries(),
                config.sampleInterval().toMillis());
        // The store survives live reloads via the holder, so its catalog would otherwise accumulate
        // stale meter names for the whole dev session. This producer runs once per (re)start, so
        // reset the catalog here to reflect only the meters present after the reload; the selection
        // and captured history are deliberately preserved (so charts keep filling across a reload).
        store.resetCatalog();
        return store;
    }
}
