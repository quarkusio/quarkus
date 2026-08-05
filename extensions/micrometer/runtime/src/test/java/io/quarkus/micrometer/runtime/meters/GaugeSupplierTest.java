package io.quarkus.micrometer.runtime.meters;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tags;

class GaugeSupplierTest {

    @Test
    void returnsNullWhenIdNotSet() {
        ConcurrentMap<Meter.Id, LongAdder> map = new ConcurrentHashMap<>();
        GaugeSupplier<LongAdder> supplier = new GaugeSupplier<>(map, LongAdder::doubleValue);
        assertNull(supplier.get());
    }

    @Test
    void returnsNullWhenKeyNotInMap() {
        ConcurrentMap<Meter.Id, LongAdder> map = new ConcurrentHashMap<>();
        GaugeSupplier<LongAdder> supplier = new GaugeSupplier<>(map, LongAdder::doubleValue);
        Meter.Id id = new Meter.Id("test", Tags.empty(), null, null, Meter.Type.GAUGE);
        supplier.setId(id);
        assertNull(supplier.get());
    }

    @Test
    void returnsValueFromMapWhenPresent() {
        ConcurrentMap<Meter.Id, LongAdder> map = new ConcurrentHashMap<>();
        Meter.Id id = new Meter.Id("test", Tags.empty(), null, null, Meter.Type.GAUGE);
        LongAdder adder = new LongAdder();
        adder.add(42);
        map.put(id, adder);

        GaugeSupplier<LongAdder> supplier = new GaugeSupplier<>(map, LongAdder::doubleValue);
        supplier.setId(id);

        assertEquals(42.0, supplier.get().doubleValue());
    }

    @Test
    void appliesCustomFunction() {
        ConcurrentMap<Meter.Id, LongAdder> map = new ConcurrentHashMap<>();
        Meter.Id id = new Meter.Id("test", Tags.empty(), null, null, Meter.Type.GAUGE);
        LongAdder adder = new LongAdder();
        adder.add(10);
        map.put(id, adder);

        GaugeSupplier<LongAdder> supplier = new GaugeSupplier<>(map, a -> a.doubleValue() * 2);
        supplier.setId(id);

        assertEquals(20.0, supplier.get().doubleValue());
    }
}
