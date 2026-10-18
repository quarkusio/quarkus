package io.quarkus.micrometer.runtime.meters;

import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

import io.micrometer.core.instrument.Meter;

class GaugeSupplier<T> implements Supplier<Number> {

    private final ConcurrentMap<Meter.Id, T> map;
    private final ToDoubleFunction<T> func;
    private volatile Meter.Id id;

    GaugeSupplier(ConcurrentMap<Meter.Id, T> map, ToDoubleFunction<T> func) {
        this.map = map;
        this.func = func;
    }

    void setId(Meter.Id id) {
        this.id = id;
    }

    @Override
    public Number get() {
        Meter.Id key = id;
        if (key != null) {
            T value = map.get(key);
            if (value != null) {
                return func.applyAsDouble(value);
            }
        }
        return null;
    }
}
