package io.quarkus.micrometer.runtime.meters;

import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

public class GaugeBuilder<T> {

    private final GaugeSupplier<T> supplier;
    private final Gauge.Builder<Supplier<Number>> builder;
    private final ConcurrentMap<Meter.Id, T> map;
    private final Supplier<T> factory;

    GaugeBuilder(String name, ConcurrentMap<Meter.Id, T> map,
            Supplier<T> factory, ToDoubleFunction<T> func) {
        this.supplier = new GaugeSupplier<>(map, func);
        this.builder = Gauge.builder(name, supplier);
        this.map = map;
        this.factory = factory;
    }

    public GaugeBuilder<T> description(String description) {
        builder.description(description);
        return this;
    }

    public GaugeBuilder<T> tags(Iterable<Tag> tags) {
        builder.tags(tags);
        return this;
    }

    public GaugeBuilder<T> tag(String key, String value) {
        builder.tag(key, value);
        return this;
    }

    public T register(MeterRegistry registry) {
        Meter.Id meterId = builder.register(registry).getId();
        supplier.setId(meterId);
        return map.computeIfAbsent(meterId, id -> factory.get());
    }

    public T register(MeterRegistry registry, T externalValue) {
        Meter.Id meterId = builder.register(registry).getId();
        supplier.setId(meterId);
        return map.computeIfAbsent(meterId, id -> externalValue);
    }
}
