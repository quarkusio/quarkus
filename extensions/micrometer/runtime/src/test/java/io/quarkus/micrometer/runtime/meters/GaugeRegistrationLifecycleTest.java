package io.quarkus.micrometer.runtime.meters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.ToDoubleFunction;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class GaugeRegistrationLifecycleTest {

    @Test
    void registrationAndSequentialRemovalDoNotScanRegistry() {
        try (var registry = new CloseableRegistry() {
            @Override
            public List<Meter> getMeters() {
                throw new AssertionError("Registration without concurrent removal must not scan the registry");
            }
        }) {
            Gauges<LongAdder> gauges = Gauges.longAdder();
            LongAdder value = gauges.builder("connections", LongAdder::doubleValue).register(registry);
            assertThat(gauges.builder("connections", LongAdder::doubleValue).register(registry)).isSameAs(value);
            Meter.Id id = new Meter.Id("connections", Tags.empty(), null, null, Meter.Type.GAUGE);
            assertThat(registry.remove(id)).isNotNull();
            assertThat(gauges.builder("connections", LongAdder::doubleValue).register(registry)).isSameAs(value);
            assertThat(registry.remove(id)).isNotNull();
        }
    }

    @Test
    void removalBeforeRegistrationIsCachedAllowsReregistration() throws Exception {
        CountDownLatch registered = new CountDownLatch(1);
        CountDownLatch removed = new CountDownLatch(1);
        AtomicBoolean pause = new AtomicBoolean(true);
        try (var registry = new CloseableRegistry() {
            @Override
            protected <T> Gauge newGauge(Meter.Id id, T object, ToDoubleFunction<T> function) {
                Gauge delegate = super.newGauge(id, object, function);
                return new Gauge() {
                    @Override
                    public double value() {
                        return delegate.value();
                    }

                    @Override
                    public Meter.Id getId() {
                        // Pause the helper's ID lookup after Micrometer has published the gauge,
                        // but before the helper can cache its registration.
                        if (pause.compareAndSet(true, false)) {
                            registered.countDown();
                            try {
                                assertThat(removed.await(10, TimeUnit.SECONDS)).isTrue();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException(e);
                            }
                        }
                        return id;
                    }
                };
            }
        }) {
            Gauges<LongAdder> gauges = Gauges.longAdder();
            try (var executor = Executors.newSingleThreadExecutor()) {
                try {
                    var registration = executor.submit(() -> gauges.builder("connections", LongAdder::doubleValue)
                            .register(registry));
                    assertThat(registered.await(10, TimeUnit.SECONDS)).isTrue();
                    assertThat(registry.remove(new Meter.Id("connections", Tags.empty(), null, null, Meter.Type.GAUGE)))
                            .isNotNull();
                    removed.countDown();
                    LongAdder value = registration.get(10, TimeUnit.SECONDS);
                    value.increment();

                    assertThat(registry.getMeters()).isEmpty();
                    assertThat(gauges.builder("connections", LongAdder::doubleValue).register(registry)).isSameAs(value);
                    assertThat(registry.get("connections").gauge().value()).isEqualTo(1);
                } finally {
                    removed.countDown();
                    executor.shutdownNow();
                    assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
                }
            }
        }
    }

    @Test
    void removalInvalidatesAllFilteredAliases() {
        Gauges<LongAdder> gauges = Gauges.longAdder();
        try (var registry = new CloseableRegistry()) {
            registry.config().commonTags("application", "test").meterFilter(MeterFilter.ignoreTags("server.port"));
            LongAdder first = gauges.builder("connections", LongAdder::doubleValue)
                    .tag("server.port", "8080").register(registry);
            assertThat(gauges.builder("connections", LongAdder::doubleValue)
                    .tag("server.port", "8081").register(registry)).isSameAs(first);
            first.increment();
            registry.remove(registry.get("connections").gauge());
            assertThat(gauges.builder("connections", LongAdder::doubleValue).tag("server.port", "8081")
                    .register(registry)).isSameAs(first);
            registry.remove(registry.get("connections").gauge());
            assertThat(gauges.builder("connections", LongAdder::doubleValue).tag("server.port", "8080")
                    .register(registry)).isSameAs(first);
            assertThat(registry.get("connections").tag("application", "test").gauge().value()).isEqualTo(1);
            assertThat(registry.getMeters()).hasSize(1);
        }
    }

    @Test
    void deniedGaugeDoesNotAppearInRegistry() {
        Gauges<LongAdder> gauges = Gauges.longAdder();
        try (var registry = new CloseableRegistry()) {
            registry.config().meterFilter(MeterFilter.deny());
            LongAdder first = gauges.builder("connections", LongAdder::doubleValue).register(registry);
            assertThat(gauges.builder("connections", LongAdder::doubleValue).register(registry)).isSameAs(first);
            assertThat(registry.getMeters()).isEmpty();
        }
    }

    @Test
    void compositeRegistrySupportsChildrenAddedAfterRegistration() {
        Gauges<LongAdder> gauges = Gauges.longAdder();
        CompositeMeterRegistry composite = new CompositeMeterRegistry();
        try (var child = new CloseableRegistry()) {
            LongAdder value = gauges.builder("connections", LongAdder::doubleValue).register(composite);
            composite.add(child);
            assertThat(gauges.builder("connections", LongAdder::doubleValue).register(composite)).isSameAs(value);
            value.increment();
            assertThat(child.get("connections").gauge().value()).isEqualTo(1);
            composite.remove(composite.get("connections").gauge());
            assertThat(child.getMeters()).isEmpty();
            assertThat(gauges.builder("connections", LongAdder::doubleValue).register(composite)).isSameAs(value);
            assertThat(child.get("connections").gauge().value()).isEqualTo(1);
        } finally {
            composite.close();
        }
    }

    private static class CloseableRegistry extends SimpleMeterRegistry implements AutoCloseable {
    }
}
