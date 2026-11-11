package io.quarkus.micrometer.runtime.binder;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.noop.NoopMeter;

/**
 * A size-bounded cache of the meters returned by a {@link Meter.MeterProvider}, keyed by the values of their tags.
 * <p>
 * On a hit, it avoids building the {@link Tags} and looking the meter up in the registry, which
 * {@link Meter.MeterProvider#withTags(Iterable)} does on every call. The tags are only built on a miss.
 * <p>
 * Rules:
 * <ul>
 * <li>A key must identify exactly one set of tags: two keys are equal if and only if their {@link Key#tags()} are
 * equal. A record holding the tags satisfies this. The cache can therefore never hold more
 * entries than there are meters.</li>
 * <li>The cache holds at most {@code maxSize} entries. When it is full, new meters are still resolved and returned,
 * just not cached. No-op meters (meters denied by a {@link io.micrometer.core.instrument.config.MeterFilter}) are never
 * cached.</li>
 * <li>All the caches are invalidated by {@link #invalidateAll()}, which is called when a meter is removed from the
 * global registry and when Quarkus has (re)configured the meter filters of the registries.</li>
 * </ul>
 *
 * @param <M> the meter type
 */
public final class MeterCache<M extends Meter> {

    /**
     * The key of a cached meter. Its {@code equals} and {@code hashCode} must be based on the tag values.
     */
    public interface Key {
        /**
         * @return the tags of the meter identified by this key
         */
        Tags tags();
    }

    /**
     * Default maximum number of cached meters per cache. An entry costs a few hundred bytes (map node, key and the tags
     * it references; the meter itself is held by the registry anyway), so a full cache uses well under 1 MB.
     */
    public static final int DEFAULT_MAX_SIZE = 1024;

    private static final AtomicLong GENERATION = new AtomicLong();
    private static final AtomicBoolean REMOVAL_LISTENER_REGISTERED = new AtomicBoolean();

    private final ConcurrentHashMap<Key, M> meters = new ConcurrentHashMap<>();
    private final Meter.MeterProvider<M> provider;
    private final int maxSize;
    private volatile long generation;

    public MeterCache(Meter.MeterProvider<M> provider) {
        this(provider, DEFAULT_MAX_SIZE);
    }

    public MeterCache(Meter.MeterProvider<M> provider, int maxSize) {
        this.provider = provider;
        this.maxSize = maxSize;
        this.generation = GENERATION.get();
        // Registered once, on the first use at runtime: the global registry outlives the application in dev mode
        if (REMOVAL_LISTENER_REGISTERED.compareAndSet(false, true)) {
            Metrics.globalRegistry.config().onMeterRemoved(new InvalidateAllOnRemoval());
        }
    }

    /**
     * Invalidates all the caches, so the next lookups go to the registry again.
     */
    public static void invalidateAll() {
        GENERATION.incrementAndGet();
    }

    /**
     * @param key the key of the meter
     * @return the meter, never null
     */
    public M get(Key key) {
        long currentGeneration = GENERATION.get();
        if (generation != currentGeneration) {
            meters.clear();
            generation = currentGeneration;
        }
        M meter = meters.get(key);
        if (meter == null) {
            meter = provider.withTags(key.tags());
            if (!(meter instanceof NoopMeter) && meters.size() < maxSize) {
                meters.put(key, meter);
                if (GENERATION.get() != currentGeneration) {
                    // Invalidated while the meter was resolved: it may already be removed from the registry
                    meters.remove(key, meter);
                }
            }
        }
        return meter;
    }

    // Visible for testing
    int size() {
        return meters.size();
    }

    private static final class InvalidateAllOnRemoval implements Consumer<Meter> {
        @Override
        public void accept(Meter meter) {
            invalidateAll();
        }
    }
}
