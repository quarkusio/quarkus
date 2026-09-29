package io.quarkus.devui.runtime.observability.telemetry;

import java.util.Optional;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.dev.telemetry.TelemetryEvent;
import io.quarkus.dev.telemetry.TelemetryEvents;
import io.quarkus.dev.telemetry.TelemetrySignals;
import io.quarkus.devui.observability.store.TelemetryStore;
import io.quarkus.devui.observability.store.metrics.MetricSample;
import io.quarkus.devui.observability.store.metrics.MetricsTimeSeriesStore;
import io.quarkus.devui.runtime.observability.traces.SpanRecord;
import io.quarkus.runtime.ShutdownEvent;

/**
 * Receives every {@link TelemetryEvent} fired in the application and hands it to the part of the observability
 * dashboard that shows its signal: a {@code metric} to the metrics store, a {@code span} to the traces store. This is
 * what lets the metrics and tracing extensions send telemetry without depending on the stores, or on Dev UI at all.
 * <p>
 * Observed synchronously, on purpose: the metrics store computes rates from each series' samples in the order they
 * arrive, and asynchronous delivery would not keep that order.
 * <p>
 * NOTE: NO class-level scope annotation on purpose, like the other observability beans: it is registered only by the
 * dev-only build step, which gives it its scope.
 */
public class TelemetryEventRouter {

    private static final Logger LOG = Logger.getLogger(TelemetryEventRouter.class);

    /** Only there when the application has a metrics backend (Micrometer or OpenTelemetry metrics). */
    @Inject
    Instance<MetricsTimeSeriesStore> metrics;

    /** Only there when the application has a tracer that sends spans (OpenTelemetry). */
    @Inject
    Instance<TelemetryStore<SpanRecord>> traces;

    // Resolved on the first event of their signal rather than on every one. Both stores are singletons, and whether
    // they exist is decided at build time, so the answer never changes for this application. A race only resolves
    // the same store twice.
    private volatile Optional<MetricsTimeSeriesStore> metricsStore;
    private volatile Optional<TelemetryStore<SpanRecord>> tracesStore;

    void stop(@Observes ShutdownEvent shutdown) {
        // Senders stop building events once nothing receives them. A dev mode restart stops the old application
        // before starting the new one, which switches them back on as it starts (see TelemetryEventsRecorder).
        TelemetryEvents.setEnabled(false);
    }

    void route(@Observes TelemetryEvent event) {
        switch (event.signal()) {
            case TelemetrySignals.METRIC -> {
                MetricsTimeSeriesStore store = metricsStore();
                MetricSample sample = store == null ? null : MetricEvents.toSample(event);
                if (sample != null) {
                    store.observe(sample);
                }
            }
            case TelemetrySignals.SPAN -> {
                TelemetryStore<SpanRecord> store = tracesStore();
                if (store != null) {
                    store.record(SpanRecord.from(event));
                }
            }
            // Nothing on the dashboard shows this signal (yet).
            default -> LOG.debugf("No Dev UI view for telemetry signal '%s', dropping %s", event.signal(), event.name());
        }
    }

    private MetricsTimeSeriesStore metricsStore() {
        Optional<MetricsTimeSeriesStore> store = metricsStore;
        if (store == null) {
            store = metrics.isResolvable() ? Optional.of(metrics.get()) : Optional.empty();
            metricsStore = store;
        }
        return store.orElse(null);
    }

    private TelemetryStore<SpanRecord> tracesStore() {
        Optional<TelemetryStore<SpanRecord>> store = tracesStore;
        if (store == null) {
            store = traces.isResolvable() ? Optional.of(traces.get()) : Optional.empty();
            tracesStore = store;
        }
        return store.orElse(null);
    }
}
