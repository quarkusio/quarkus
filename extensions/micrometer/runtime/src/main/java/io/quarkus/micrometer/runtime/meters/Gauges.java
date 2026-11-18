package io.quarkus.micrometer.runtime.meters;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;

public class Gauges<T> {

    private final ConcurrentMap<Meter.Id, T> map;
    private final Supplier<T> factory;
    private final ConcurrentMap<MeterRegistry, Registrations<T>> registrations = new ConcurrentHashMap<>();

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
        return new GaugeBuilder<>(name, this, func);
    }

    T register(MeterRegistry registry, Meter.Id originalId, ToDoubleFunction<T> function, Supplier<T> externalValue) {
        Registrations<T> registryRegistrations = registrations.computeIfAbsent(registry, Registrations::new);
        // Fast-path avoiding acquisition of lock when a mapping already exists
        Registration<T> existing = registryRegistrations.originalIds.get(originalId);
        if (existing != null && !existing.removed) {
            return existing.value;
        }
        // Slow-path: Gauge not registered yet.
        return registryRegistrations.originalIds.compute(originalId, (id, previous) -> {
            // Check again since another thread might have registered the Gauge since the above check
            if (previous != null && !previous.removed) {
                return previous;
            }
            // Get the removal version before registering the Gauge.
            // A Gauge can be registered and removed multiple times.
            long removalVersion = registryRegistrations.removalVersion.get();
            GaugeSupplier<T> supplier = new GaugeSupplier<>(map, function);
            Meter.Id mappedId = Gauge.builder(id.getName(), supplier)
                    .tags(id.getTagsAsIterable())
                    .description(id.getDescription())
                    .register(registry).getId();
            supplier.setId(mappedId);
            // Keep sharing the backing value when filters merge different original IDs.
            T value = map.computeIfAbsent(mappedId, ignored -> (externalValue == null ? factory : externalValue).get());
            Registration<T> registration = new Registration<>(value);
            Registration<T> shared = registryRegistrations.mappedIds.compute(mappedId, (key, current) -> {
                if (current != null) {
                    return current;
                }
                // A removal before publication cannot find this registration to invalidate it.
                // Reconcile only when a removal overlapped registration. This map lock also
                // serializes publication with invalidation; getMeters() does not take the
                // registry lock, so it does not invert Micrometer's lock order.
                if (removalVersion != registryRegistrations.removalVersion.get()
                        && !contains(registry, mappedId)) {
                    registration.removed = true;
                    return null;
                }
                return registration;
            });
            return shared == null ? registration : shared;
        }).value;
    }

    private static boolean contains(MeterRegistry registry, Meter.Id id) {
        for (Meter meter : registry.getMeters()) {
            if (meter.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static final class Registrations<T> {
        final AtomicLong removalVersion = new AtomicLong();
        // A map from original IDs to registrations. This enables avoiding double registrations.
        final ConcurrentMap<Meter.Id, Registration<T>> originalIds = new ConcurrentHashMap<>();
        // A map from mapped IDs (after filtering) to registrations. This enables efficient Gauge removal.
        final ConcurrentMap<Meter.Id, Registration<T>> mappedIds = new ConcurrentHashMap<>();

        Registrations(MeterRegistry registry) {
            // The registry can outlive the binder that owns this cache.
            WeakReference<Registrations<T>> reference = new WeakReference<>(this);
            registry.config().onMeterRemoved(meter -> {
                Registrations<T> cache = reference.get();
                if (cache == null || meter.getId().getType() != Meter.Type.GAUGE) {
                    return;
                }
                // Invalidate all aliases in constant time, without taking the original-ID
                // registration lock while Micrometer holds its own registry lock.
                cache.removalVersion.incrementAndGet();
                cache.mappedIds.computeIfPresent(meter.getId(), (id, registration) -> {
                    registration.removed = true;
                    return null;
                });
            });
        }
    }

    private static final class Registration<T> {
        final T value;
        volatile boolean removed;

        Registration(T value) {
            this.value = value;
        }
    }
}
