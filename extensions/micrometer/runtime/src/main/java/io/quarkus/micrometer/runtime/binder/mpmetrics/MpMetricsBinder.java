package io.quarkus.micrometer.runtime.binder.mpmetrics;

import java.util.Iterator;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

@Singleton
class MpMetricsBinder implements MeterBinder {

    final Instance<AnnotatedGaugeAdapter> allGaugeAdapters;

    // Micrometer application meter registry
    final MetricRegistryAdapter registry;

    MpMetricsBinder(MetricRegistryAdapter registry,
            Instance<AnnotatedGaugeAdapter> allGaugeAdapters) {
        this.registry = registry;
        this.allGaugeAdapters = allGaugeAdapters;
    }

    @Override
    public void bindTo(MeterRegistry r) {
        // register all annotation-declared gauges
        // this needs to wait until associated/monitored objects can be created.
        for (Iterator<AnnotatedGaugeAdapter> gauges = allGaugeAdapters.iterator(); gauges.hasNext();) {
            registry.bindAnnotatedGauge(gauges.next());
        }
    }
}
