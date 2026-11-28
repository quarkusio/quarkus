package io.quarkus.opentelemetry.runtime.exporter.otlp.metrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.logging.Logger;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;

/**
 * A {@link MetricExporter} that fans out to several delegates, so the built-in OTLP exporter can
 * coexist with additional exporters (CDI beans or Quarkiverse extension exporters).
 * <p>
 * Each delegate is invoked independently: a delegate that throws does not prevent the others from
 * running, and its failure is folded into the aggregated result.
 */
public final class CompositeMetricExporter implements MetricExporter {

    private static final Logger log = Logger.getLogger(CompositeMetricExporter.class);

    private final List<MetricExporter> delegates;

    private CompositeMetricExporter(List<MetricExporter> delegates) {
        this.delegates = delegates;
    }

    /**
     * Collapses the given exporters into a single {@link MetricExporter}: an empty collection returns
     * the Noop exporter, a single element is returned as-is, and multiple elements are wrapped in a
     * {@link CompositeMetricExporter}.
     */
    public static MetricExporter of(Collection<MetricExporter> exporters) {
        List<MetricExporter> delegates = new ArrayList<>(exporters);
        if (delegates.isEmpty()) {
            return NoopMetricExporter.INSTANCE;
        }
        if (delegates.size() == 1) {
            return delegates.get(0);
        }
        return new CompositeMetricExporter(delegates);
    }

    @Override
    public CompletableResultCode export(Collection<MetricData> metrics) {
        List<CompletableResultCode> results = new ArrayList<>(delegates.size());
        for (MetricExporter delegate : delegates) {
            try {
                results.add(delegate.export(metrics));
            } catch (RuntimeException e) {
                log.debugf(e, "Exception thrown by metric exporter %s", delegate.getClass().getName());
                results.add(CompletableResultCode.ofFailure());
            }
        }
        return CompletableResultCode.ofAll(results);
    }

    @Override
    public CompletableResultCode flush() {
        List<CompletableResultCode> results = new ArrayList<>(delegates.size());
        for (MetricExporter delegate : delegates) {
            try {
                results.add(delegate.flush());
            } catch (RuntimeException e) {
                log.debugf(e, "Exception thrown while flushing metric exporter %s", delegate.getClass().getName());
                results.add(CompletableResultCode.ofFailure());
            }
        }
        return CompletableResultCode.ofAll(results);
    }

    @Override
    public CompletableResultCode shutdown() {
        List<CompletableResultCode> results = new ArrayList<>(delegates.size());
        for (MetricExporter delegate : delegates) {
            try {
                results.add(delegate.shutdown());
            } catch (RuntimeException e) {
                log.debugf(e, "Exception thrown while shutting down metric exporter %s", delegate.getClass().getName());
                results.add(CompletableResultCode.ofFailure());
            }
        }
        return CompletableResultCode.ofAll(results);
    }

    @Override
    public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
        // A MeterProvider registers a single temporality per exporter, so we honor the first
        // delegate's preference. Mixing exporters that disagree on temporality is not supported.
        return delegates.get(0).getAggregationTemporality(instrumentType);
    }
}
