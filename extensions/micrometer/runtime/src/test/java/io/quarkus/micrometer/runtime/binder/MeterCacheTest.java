package io.quarkus.micrometer.runtime.binder;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.noop.NoopTimer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class MeterCacheTest {

    private static final String NAME = "test.requests";

    private SimpleMeterRegistry globalChild;

    record Key(String uri, String status) implements MeterCache.Key {
        @Override
        public Tags tags() {
            return Tags.of(Tag.of("uri", uri), Tag.of("status", status));
        }
    }

    /**
     * A provider counting how many times the cache resolves a meter from the registry.
     */
    static final class CountingProvider implements Meter.MeterProvider<Timer> {
        final Meter.MeterProvider<Timer> delegate;
        final AtomicInteger calls = new AtomicInteger();

        CountingProvider(MeterRegistry registry) {
            this.delegate = Timer.builder(NAME).withRegistry(registry);
        }

        @Override
        public Timer withTags(Iterable<? extends Tag> tags) {
            calls.incrementAndGet();
            return delegate.withTags(tags);
        }
    }

    @BeforeEach
    void addGlobalChild() {
        globalChild = new SimpleMeterRegistry();
        Metrics.addRegistry(globalChild);
    }

    @AfterEach
    void cleanGlobalRegistry() {
        for (Meter meter : Metrics.globalRegistry.find(NAME).meters()) {
            Metrics.globalRegistry.remove(meter);
        }
        Metrics.removeRegistry(globalChild);
    }

    @Test
    void hitReturnsTheSameMeterWithoutLookingItUpAgain() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CountingProvider provider = new CountingProvider(registry);
        MeterCache<Timer> cache = new MeterCache<>(provider);

        Timer first = cache.get(new Key("/a", "200"));
        Timer second = cache.get(new Key("/a", "200"));

        assertThat(second).isSameAs(first);
        assertThat(provider.calls).hasValue(1);
        assertThat(cache.size()).isEqualTo(1);
    }

    @Test
    void returnsTheMeterOfTheRegistryWithExactlyTheKeyTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MeterCache<Timer> cache = new MeterCache<>(Timer.builder(NAME).withRegistry(registry));

        Timer timer = cache.get(new Key("/a", "200"));

        assertThat(timer).isSameAs(registry.find(NAME).tags("uri", "/a", "status", "200").timer());
        assertThat(timer.getId().getTags()).containsExactlyElementsOf(new Key("/a", "200").tags());
    }

    @Test
    void differentKeysGetDifferentMeters() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MeterCache<Timer> cache = new MeterCache<>(Timer.builder(NAME).withRegistry(registry));

        Timer a200 = cache.get(new Key("/a", "200"));
        Timer a500 = cache.get(new Key("/a", "500"));
        Timer b200 = cache.get(new Key("/b", "200"));

        assertThat(List.of(a200, a500, b200)).doesNotHaveDuplicates();
        assertThat(registry.find(NAME).timers()).hasSize(3);
        assertThat(cache.size()).isEqualTo(3);
    }

    @Test
    void recordingsGoToTheRegistry() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MeterCache<Timer> cache = new MeterCache<>(Timer.builder(NAME).withRegistry(registry));

        for (int i = 0; i < 5; i++) {
            cache.get(new Key("/a", "200")).record(1, TimeUnit.MILLISECONDS);
        }

        assertThat(registry.find(NAME).tags("uri", "/a").timer().count()).isEqualTo(5);
    }

    @Test
    void deniedMetersAreNotCached() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        // Same kind of filter as the max-uri-tags configuration
        registry.config().meterFilter(MeterFilter.maximumAllowableTags(NAME, "uri", 2, MeterFilter.deny()));
        CountingProvider provider = new CountingProvider(registry);
        MeterCache<Timer> cache = new MeterCache<>(provider);

        cache.get(new Key("/a", "200"));
        cache.get(new Key("/b", "200"));
        Timer denied = cache.get(new Key("/c", "200"));
        Timer deniedAgain = cache.get(new Key("/c", "200"));

        assertThat(denied).isInstanceOf(NoopTimer.class);
        assertThat(deniedAgain).isInstanceOf(NoopTimer.class);
        assertThat(cache.size()).isEqualTo(2);
        // the denied meter is resolved again every time, the others only once
        assertThat(provider.calls).hasValue(4);
    }

    @Test
    void cacheSizeIsLimited() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CountingProvider provider = new CountingProvider(registry);
        MeterCache<Timer> cache = new MeterCache<>(provider, 2);

        Timer a = cache.get(new Key("/a", "200"));
        Timer b = cache.get(new Key("/b", "200"));
        Timer c = cache.get(new Key("/c", "200"));

        assertThat(cache.size()).isEqualTo(2);
        // a meter that does not fit is still the right, registered, meter
        assertThat(c).isSameAs(registry.find(NAME).tags("uri", "/c").timer());
        assertThat(cache.get(new Key("/c", "200"))).isSameAs(c);
        // the cached meters are still hits
        assertThat(cache.get(new Key("/a", "200"))).isSameAs(a);
        assertThat(cache.get(new Key("/b", "200"))).isSameAs(b);
        assertThat(provider.calls).hasValue(4);
        assertThat(cache.size()).isEqualTo(2);
    }

    @Test
    void defaultSizeIsLimited() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MeterCache<Timer> cache = new MeterCache<>(Timer.builder(NAME).withRegistry(registry));

        for (int i = 0; i < MeterCache.DEFAULT_MAX_SIZE + 100; i++) {
            cache.get(new Key("/" + i, "200"));
        }

        assertThat(cache.size()).isEqualTo(MeterCache.DEFAULT_MAX_SIZE);
    }

    @Test
    void removingAMeterFromTheGlobalRegistryInvalidatesTheCache() {
        MeterCache<Timer> cache = new MeterCache<>(Timer.builder(NAME).withRegistry(Metrics.globalRegistry));
        Timer removed = cache.get(new Key("/a", "200"));

        Metrics.globalRegistry.remove(removed);
        Timer timer = cache.get(new Key("/a", "200"));
        timer.record(1, TimeUnit.MILLISECONDS);

        assertThat(timer).isNotSameAs(removed);
        assertThat(Metrics.globalRegistry.find(NAME).tags("uri", "/a").timer()).isSameAs(timer);
        assertThat(globalChild.find(NAME).tags("uri", "/a").timer().count()).isEqualTo(1);
    }

    @Test
    void removingAnyMeterFromTheGlobalRegistryInvalidatesAllTheCaches() {
        MeterCache<Timer> first = new MeterCache<>(Timer.builder(NAME).withRegistry(Metrics.globalRegistry));
        MeterCache<Timer> second = new MeterCache<>(Timer.builder(NAME).withRegistry(Metrics.globalRegistry));
        first.get(new Key("/a", "200"));
        second.get(new Key("/b", "200"));

        Metrics.globalRegistry.remove(Metrics.globalRegistry.counter("test.unrelated"));

        // the caches are cleared on their next use
        first.get(new Key("/b", "200"));
        second.get(new Key("/c", "200"));
        assertThat(first.size()).isEqualTo(1);
        assertThat(second.size()).isEqualTo(1);
    }

    @Test
    void invalidateAllPicksUpLateMeterFilters() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MeterCache<Timer> cache = new MeterCache<>(Timer.builder(NAME).withRegistry(registry));
        Timer beforeFilter = cache.get(new Key("/a", "200"));

        // e.g. the filters re-applied by the dev mode restart, while the HTTP server kept running
        registry.config().meterFilter(MeterFilter.commonTags(Tags.of("env", "dev")));
        assertThat(cache.get(new Key("/a", "200"))).isSameAs(beforeFilter);

        MeterCache.invalidateAll();
        Timer afterFilter = cache.get(new Key("/a", "200"));

        assertThat(afterFilter).isNotSameAs(beforeFilter);
        assertThat(afterFilter.getId().getTag("env")).isEqualTo("dev");
    }

    @Test
    void meterRemovedWhileBeingResolvedIsNotKept() {
        Meter.MeterProvider<Timer> delegate = Timer.builder(NAME).withRegistry(Metrics.globalRegistry);
        AtomicBoolean removeOnce = new AtomicBoolean(true);
        List<MeterCache<Timer>> cacheHolder = new ArrayList<>();
        // Deterministic version of the race: after the registry returned the meter, but before it is cached,
        // the meter is removed and another thread uses the cache (here: a nested call), which clears it
        Meter.MeterProvider<Timer> removingProvider = tags -> {
            Timer timer = delegate.withTags(tags);
            if (removeOnce.compareAndSet(true, false)) {
                Metrics.globalRegistry.remove(timer);
                cacheHolder.get(0).get(new Key("/other", "200"));
            }
            return timer;
        };
        MeterCache<Timer> cache = new MeterCache<>(removingProvider);
        cacheHolder.add(cache);

        Timer removed = cache.get(new Key("/a", "200"));
        Timer timer = cache.get(new Key("/a", "200"));

        assertThat(timer).isNotSameAs(removed);
        assertThat(Metrics.globalRegistry.find(NAME).tags("uri", "/a").timer()).isSameAs(timer);
    }

    @Test
    void concurrentGetsAndRemovalsNeverKeepARemovedMeter() throws Exception {
        MeterCache<Timer> cache = new MeterCache<>(Timer.builder(NAME).withRegistry(Metrics.globalRegistry));
        int keys = 20;
        int workers = 8;
        AtomicBoolean removing = new AtomicBoolean(true);
        AtomicBoolean running = new AtomicBoolean(true);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(workers + 1);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int w = 0; w < workers; w++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    while (running.get()) {
                        int k = ThreadLocalRandom.current().nextInt(keys);
                        cache.get(new Key("/" + k, "200")).record(1, TimeUnit.MILLISECONDS);
                    }
                    return null;
                }));
            }
            futures.add(executor.submit(() -> {
                start.await();
                while (removing.get()) {
                    for (Meter meter : Metrics.globalRegistry.find(NAME).meters()) {
                        Metrics.globalRegistry.remove(meter);
                    }
                    Thread.yield();
                }
                return null;
            }));

            start.countDown();
            Thread.sleep(500);
            // stop removing first, then let the workers run a bit with no more removal
            removing.set(false);
            Thread.sleep(100);
            running.set(false);
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        // Every meter the cache returns must be the one currently registered: a cached removed meter would lose data
        for (int k = 0; k < keys; k++) {
            Timer timer = cache.get(new Key("/" + k, "200"));
            assertThat(timer).isSameAs(Metrics.globalRegistry.find(NAME).tags("uri", "/" + k).timer());
        }
    }
}
