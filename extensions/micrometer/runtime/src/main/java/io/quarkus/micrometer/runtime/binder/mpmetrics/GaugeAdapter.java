package io.quarkus.micrometer.runtime.binder.mpmetrics;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricType;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;

interface GaugeAdapter<T> extends Gauge<T>, MeterHolder {

    GaugeAdapter<T> register(MpMetadata metadata, MetricDescriptor metricInfo, MeterRegistry registry);

    static class DoubleFunctionGauge<S> implements GaugeAdapter<Double> {
        io.micrometer.core.instrument.Gauge gauge;

        final S obj;
        final ToDoubleFunction<S> f;

        DoubleFunctionGauge(S obj, ToDoubleFunction<S> f) {
            this.obj = obj;
            this.f = f;
        }

        public GaugeAdapter<Double> register(MpMetadata metadata, MetricDescriptor metricInfo, MeterRegistry registry) {
            gauge = io.micrometer.core.instrument.Gauge.builder(metricInfo.name(), obj, f)
                    .description(metadata.getDescription())
                    .tags(metricInfo.tags())
                    .baseUnit(metadata.getUnit())
                    .strongReference(true)
                    .register(registry);
            return this;
        }

        @Override
        public Meter getMeter() {
            return gauge;
        }

        @Override
        public Double getValue() {
            return gauge.value();
        }

        @Override
        public MetricType getType() {
            return MetricType.GAUGE;
        }
    }

    static class FunctionGauge<S, R extends Number> implements GaugeAdapter<R> {
        io.micrometer.core.instrument.Gauge gauge;

        final S obj;
        final Function<S, R> f;

        FunctionGauge(S obj, Function<S, R> f) {
            this.obj = obj;
            this.f = f;
        }

        public GaugeAdapter<R> register(MpMetadata metadata, MetricDescriptor metricInfo, MeterRegistry registry) {
            gauge = io.micrometer.core.instrument.Gauge.builder(metricInfo.name(), obj, obj -> f.apply(obj).doubleValue())
                    .description(metadata.getDescription())
                    .tags(metricInfo.tags())
                    .baseUnit(metadata.getUnit())
                    .strongReference(true)
                    .register(registry);
            return this;
        }

        @Override
        public Meter getMeter() {
            return gauge;
        }

        @Override
        public R getValue() {
            return (R) (Double) gauge.value();
        }

        @Override
        public MetricType getType() {
            return MetricType.GAUGE;
        }
    }

    static class NumberSupplierGauge<T extends Number> implements GaugeAdapter<T> {
        io.micrometer.core.instrument.Gauge gauge;
        final Supplier<T> supplier;

        NumberSupplierGauge(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        @Override
        public GaugeAdapter<T> register(MpMetadata metadata, MetricDescriptor metricInfo, MeterRegistry registry) {
            if (gauge == null || metadata.cleanDirtyMetadata()) {
                gauge = io.micrometer.core.instrument.Gauge.builder(metricInfo.name(), (Supplier<Number>) supplier)
                        .description(metadata.getDescription())
                        .tags(metricInfo.tags())
                        .baseUnit(metadata.getUnit())
                        .strongReference(true).register(registry);
            }

            return this;
        }

        @Override
        public Meter getMeter() {
            return gauge;
        }

        @Override
        public T getValue() {
            return supplier.get();
        }

        @Override
        public MetricType getType() {
            return MetricType.GAUGE;
        }
    }
}
