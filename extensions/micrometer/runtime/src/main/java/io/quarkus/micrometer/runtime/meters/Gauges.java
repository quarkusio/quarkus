package io.quarkus.micrometer.runtime.meters;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

import io.micrometer.core.instrument.Meter;

public class Gauges<T> {

    private final ConcurrentMap<Meter.Id, T> map;
    private final Supplier<T> factory;

    private Gauges(Supplier<T> factory) {
        this.map = new ConcurrentHashMap<>();
        this.factory = factory;
    }

    public static Gauges<LongAdder> longAdder() {
        return new Gauges<>(LongAdder::new);
    }

    public static Gauges<AtomicInteger> atomicInteger() {
        return new Gauges<>(AtomicInteger::new);
    }

    public static <T> Gauges<T> of(Supplier<T> factory) {
        return new Gauges<>(factory);
    }

    public GaugeBuilder<T> builder(String name, ToDoubleFunction<T> func) {
        return new GaugeBuilder<>(name, map, factory, func);
    }
}
