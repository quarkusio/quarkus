package io.quarkus.micrometer.runtime.meters;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class GaugesTest {

    @Test
    void longAdderFactoryCreatesWorkingGauge() {
        Gauges<LongAdder> gauges = Gauges.longAdder();
        MeterRegistry registry = new SimpleMeterRegistry();

        LongAdder adder = gauges.builder("test.longadder", LongAdder::doubleValue)
                .register(registry);
        adder.add(5);

        assertEquals(5.0, registry.get("test.longadder").gauge().value());
    }

    @Test
    void atomicIntegerFactoryCreatesWorkingGauge() {
        Gauges<AtomicInteger> gauges = Gauges.atomicInteger();
        MeterRegistry registry = new SimpleMeterRegistry();

        AtomicInteger value = gauges.builder("test.atomicint", AtomicInteger::doubleValue)
                .register(registry);
        value.set(7);

        assertEquals(7.0, registry.get("test.atomicint").gauge().value());
    }

    @Test
    void customFactoryCreatesWorkingGauge() {
        Gauges<double[]> gauges = Gauges.of(() -> new double[] { 0.0 });
        MeterRegistry registry = new SimpleMeterRegistry();

        double[] arr = gauges.builder("test.custom", a -> a[0])
                .register(registry);
        arr[0] = 3.14;

        assertEquals(3.14, registry.get("test.custom").gauge().value(), 0.001);
    }
}
