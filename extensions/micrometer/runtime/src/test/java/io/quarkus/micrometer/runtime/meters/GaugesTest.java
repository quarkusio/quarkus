package io.quarkus.micrometer.runtime.meters;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class GaugesTest {

    private final List<LogRecord> records = new CopyOnWriteArrayList<>();
    private final Logger logger = Logger.getLogger(MeterRegistry.class.getName());
    private Level originalLevel;
    private final Handler handler = new Handler() {
        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };

    @BeforeEach
    void captureRegistrationLogs() {
        originalLevel = logger.getLevel();
        logger.setLevel(Level.ALL);
        logger.addHandler(handler);
    }

    @AfterEach
    void restoreRegistrationLogger() {
        logger.removeHandler(handler);
        logger.setLevel(originalLevel);
    }

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

    @Test
    void concurrentRegistrationsDoNotLogDuplicates() throws Exception {
        Gauges<LongAdder> gauges = Gauges.longAdder();
        MeterRegistry registry = new SimpleMeterRegistry();
        try (var executor = Executors.newFixedThreadPool(16)) {
            CyclicBarrier barrier = new CyclicBarrier(16);
            List<Future<LongAdder>> results = new ArrayList<>();
            for (int i = 0; i < 16; i++) {
                results.add(executor.submit(() -> {
                    barrier.await(10, TimeUnit.SECONDS);
                    LongAdder value = gauges.builder("http.server.active.connections", LongAdder::doubleValue)
                            .tag("server.port", "18085")
                            .register(registry);
                    value.increment();
                    return value;
                }));
            }
            LongAdder first = results.get(0).get(10, TimeUnit.SECONDS);
            for (Future<LongAdder> result : results) {
                assertSame(first, result.get(10, TimeUnit.SECONDS));
            }
            // Also exercise the non-racing path, regardless of how the workers were scheduled.
            assertSame(first, gauges.builder("http.server.active.connections", LongAdder::doubleValue)
                    .tag("server.port", "18085").register(registry));
            assertEquals(16.0, registry.get("http.server.active.connections").gauge().value());
            assertTrue(records.stream().noneMatch(record -> record.getMessage().contains("already registered")),
                    () -> records.stream().map(LogRecord::getMessage).toList().toString());
        } finally {
            registry.close();

        }
    }

    @Test
    void repeatedExternalValueRegistrationKeepsOriginalValue() {
        Gauges<AtomicInteger> gauges = Gauges.atomicInteger();
        MeterRegistry registry = new SimpleMeterRegistry();
        try {
            AtomicInteger first = new AtomicInteger(7);
            assertSame(first, gauges.builder("test.external", AtomicInteger::doubleValue).register(registry, first));
            assertSame(first, gauges.builder("test.external", AtomicInteger::doubleValue)
                    .register(registry, new AtomicInteger(99)));
            assertEquals(7.0, registry.get("test.external").gauge().value());
        } finally {
            registry.close();
        }
    }

    @Test
    void registersGaugeInEachRegistryWithMappedId() {
        Gauges<LongAdder> gauges = Gauges.longAdder();
        MeterRegistry first = new SimpleMeterRegistry();
        MeterRegistry second = new SimpleMeterRegistry();
        try {
            first.config().meterFilter(MeterFilter.commonTags(List.of(Tag.of("common", "value"))));
            second.config().meterFilter(MeterFilter.commonTags(List.of(Tag.of("common", "value"))));
            LongAdder value = gauges.builder("test.mapped", LongAdder::doubleValue).register(first);
            assertSame(value, gauges.builder("test.mapped", LongAdder::doubleValue).register(second));
            value.add(5);
            assertEquals(5.0, first.get("test.mapped").tag("common", "value").gauge().value());
            assertEquals(5.0, second.get("test.mapped").tag("common", "value").gauge().value());
            first.clear();
            assertSame(value, gauges.builder("test.mapped", LongAdder::doubleValue).register(second));
            assertSame(value, gauges.builder("test.mapped", LongAdder::doubleValue).register(first));
            assertEquals(5.0, first.get("test.mapped").tag("common", "value").gauge().value());
            assertTrue(records.stream().noneMatch(record -> record.getMessage().contains("already registered")));
        } finally {
            first.close();
            second.close();
        }
    }

    @Test
    void registersGaugeAgainAfterRemoval() {
        Gauges<LongAdder> gauges = Gauges.longAdder();
        MeterRegistry registry = new SimpleMeterRegistry();
        try {
            LongAdder value = gauges.builder("test.removed", LongAdder::doubleValue).register(registry);
            registry.remove(registry.get("test.removed").gauge());
            assertSame(value, gauges.builder("test.removed", LongAdder::doubleValue).register(registry));
            value.add(3);
            assertEquals(3.0, registry.get("test.removed").gauge().value());
        } finally {
            registry.close();
        }
    }

    @Test
    void removingOneGaugeKeepsOtherRegistrationsCached() {
        Gauges<LongAdder> gauges = Gauges.longAdder();
        MeterRegistry registry = new SimpleMeterRegistry();
        try {
            gauges.builder("test.removed", LongAdder::doubleValue).register(registry);
            LongAdder retained = gauges.builder("test.retained", LongAdder::doubleValue).register(registry);
            registry.remove(registry.get("test.removed").gauge());
            assertSame(retained, gauges.builder("test.retained", LongAdder::doubleValue).register(registry));
            assertTrue(records.stream().noneMatch(record -> record.getMessage().contains("already registered")));
        } finally {
            registry.close();
        }
    }

    @Test
    void clearingRegistryAllowsAllGaugesToBeRegisteredAgain() {
        Gauges<LongAdder> gauges = Gauges.longAdder();
        MeterRegistry registry = new SimpleMeterRegistry();
        try {
            LongAdder first = gauges.builder("test.first", LongAdder::doubleValue).register(registry);
            LongAdder second = gauges.builder("test.second", LongAdder::doubleValue).register(registry);
            registry.clear();
            assertSame(first, gauges.builder("test.first", LongAdder::doubleValue).register(registry));
            assertSame(second, gauges.builder("test.second", LongAdder::doubleValue).register(registry));
            assertEquals(2, registry.getMeters().size());
        } finally {
            registry.close();
        }
    }
}
