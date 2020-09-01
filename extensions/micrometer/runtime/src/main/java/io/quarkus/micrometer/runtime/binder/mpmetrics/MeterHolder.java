package io.quarkus.micrometer.runtime.binder.mpmetrics;

import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricType;

import io.micrometer.core.instrument.Meter;

interface MeterHolder extends Metric {
    Meter getMeter();

    MetricType getType();
}
