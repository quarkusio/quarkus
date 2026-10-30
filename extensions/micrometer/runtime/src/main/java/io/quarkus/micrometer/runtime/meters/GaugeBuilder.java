package io.quarkus.micrometer.runtime.meters;

import java.util.function.ToDoubleFunction;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

public class GaugeBuilder<T> {

    private final String name;
    private final Gauges<T> gauges;
    private final ToDoubleFunction<T> function;
    private Tags tags = Tags.empty();
    private String description;

    GaugeBuilder(String name, Gauges<T> gauges, ToDoubleFunction<T> function) {
        this.name = name;
        this.gauges = gauges;
        this.function = function;
    }

    public GaugeBuilder<T> description(String description) {
        this.description = description;
        return this;
    }

    public GaugeBuilder<T> tags(Iterable<Tag> tags) {
        this.tags = this.tags.and(tags);
        return this;
    }

    public GaugeBuilder<T> tag(String key, String value) {
        tags = tags.and(key, value);
        return this;
    }

    public T register(MeterRegistry registry) {
        return gauges.register(registry, new Meter.Id(name, tags, null, description, Meter.Type.GAUGE), function, null);
    }

    public T register(MeterRegistry registry, T externalValue) {
        return gauges.register(registry, new Meter.Id(name, tags, null, description, Meter.Type.GAUGE), function,
                () -> externalValue);
    }
}
