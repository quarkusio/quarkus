package io.quarkus.micrometer.runtime.meters;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class GaugeBuilderTest {

    @Test
    void registerCreatesNewBackingObject() {
        ConcurrentMap<Meter.Id, LongAdder> map = new ConcurrentHashMap<>();
        MeterRegistry registry = new SimpleMeterRegistry();

        GaugeBuilder<LongAdder> builder = new GaugeBuilder<>(
                "test.gauge", map, LongAdder::new, LongAdder::doubleValue);
        LongAdder result = builder.register(registry);

        assertNotNull(result);
        assertEquals(1, map.size());
    }

    @Test
    void registerReturnsSameObjectForSameMeterId() {
        ConcurrentMap<Meter.Id, LongAdder> map = new ConcurrentHashMap<>();
        MeterRegistry registry = new SimpleMeterRegistry();

        GaugeBuilder<LongAdder> builder1 = new GaugeBuilder<>(
                "test.gauge", map, LongAdder::new, LongAdder::doubleValue);
        LongAdder first = builder1.register(registry);

        GaugeBuilder<LongAdder> builder2 = new GaugeBuilder<>(
                "test.gauge", map, LongAdder::new, LongAdder::doubleValue);
        LongAdder second = builder2.register(registry);

        assertSame(first, second);
        assertEquals(1, map.size());
    }

    @Test
    void registerWithTagsCreatesDifferentEntries() {
        ConcurrentMap<Meter.Id, LongAdder> map = new ConcurrentHashMap<>();
        MeterRegistry registry = new SimpleMeterRegistry();

        GaugeBuilder<LongAdder> builder1 = new GaugeBuilder<>(
                "test.gauge", map, LongAdder::new, LongAdder::doubleValue);
        builder1.tag("env", "prod");
        LongAdder first = builder1.register(registry);

        GaugeBuilder<LongAdder> builder2 = new GaugeBuilder<>(
                "test.gauge", map, LongAdder::new, LongAdder::doubleValue);
        builder2.tag("env", "dev");
        LongAdder second = builder2.register(registry);

        assertNotSame(first, second);
        assertEquals(2, map.size());
    }

    @Test
    void registerWithExternalValueUsesProvidedInstance() {
        ConcurrentMap<Meter.Id, AtomicInteger> map = new ConcurrentHashMap<>();
        MeterRegistry registry = new SimpleMeterRegistry();
        AtomicInteger external = new AtomicInteger(99);

        GaugeBuilder<AtomicInteger> builder = new GaugeBuilder<>(
                "test.gauge", map, AtomicInteger::new, AtomicInteger::doubleValue);
        AtomicInteger result = builder.register(registry, external);

        assertSame(external, result);
        assertEquals(99, map.values().iterator().next().get());
    }

    @Test
    void gaugeReportsValueFromBackingObject() {
        ConcurrentMap<Meter.Id, LongAdder> map = new ConcurrentHashMap<>();
        MeterRegistry registry = new SimpleMeterRegistry();

        GaugeBuilder<LongAdder> builder = new GaugeBuilder<>(
                "test.gauge", map, LongAdder::new, LongAdder::doubleValue);
        LongAdder adder = builder.register(registry);
        adder.add(42);

        double value = registry.get("test.gauge").gauge().value();
        assertEquals(42.0, value);
    }

    @Test
    void descriptionAndTagsAreApplied() {
        ConcurrentMap<Meter.Id, LongAdder> map = new ConcurrentHashMap<>();
        MeterRegistry registry = new SimpleMeterRegistry();

        GaugeBuilder<LongAdder> builder = new GaugeBuilder<>(
                "test.gauge", map, LongAdder::new, LongAdder::doubleValue);
        builder.description("A test gauge")
                .tags(Tags.of("key", "val"));
        builder.register(registry);

        io.micrometer.core.instrument.Gauge gauge = registry.get("test.gauge")
                .tag("key", "val").gauge();
        assertNotNull(gauge);
        assertEquals("A test gauge", gauge.getId().getDescription());
    }
}
