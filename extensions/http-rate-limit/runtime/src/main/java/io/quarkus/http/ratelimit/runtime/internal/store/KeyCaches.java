package io.quarkus.http.ratelimit.runtime.internal.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.github.benmanes.caffeine.cache.Ticker;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.github.benmanes.caffeine.cache.stats.StatsCounter;

import io.quarkus.http.ratelimit.RateLimitLocalStoreCustomizer;
import io.quarkus.http.ratelimit.runtime.internal.gcra.RateLimitClock;

/**
 * The per-key Caffeine caches of the in-memory stores: bounded, timed by the rate limit clock, and maintained on the
 * calling thread.
 */
final class KeyCaches {

    private KeyCaches() {
    }

    /**
     * @param customizer applies the {@code RateLimitLocalStoreCustomizer}s to the cache, after its own settings
     */
    static <V> Cache<String, V> build(long maxKeys, Expiry<String, V> expiry,
            RemovalListener<String, V> evictionListener,
            RateLimitClock clock, Consumer<Caffeine<Object, Object>> customizer) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder();
        Caffeine<String, V> typed = builder
                .maximumSize(maxKeys)
                .expireAfter(expiry)
                .ticker(new Ticker() {
                    @Override
                    public long read() {
                        return clock.nanoTime();
                    }
                })
                .executor(new Executor() {
                    @Override
                    public void execute(Runnable command) {
                        command.run();
                    }
                })
                // an eviction listener, not a removal listener, keeps the cache class among those Quarkus registers for native
                .evictionListener(evictionListener);
        // the same builder, before its type was narrowed
        customizer.accept(builder);
        return typed.build();
    }

    /**
     * Applies the customizers to the cache of a rule, in their order. The statistics they register are reported
     * together, through one stats counter, since Caffeine takes only one.
     */
    static void customize(List<RateLimitLocalStoreCustomizer> customizers, String rule, Caffeine<Object, Object> builder) {
        List<RateLimitLocalStoreCustomizer.Statistics> suppliedStats = new ArrayList<>();
        RateLimitLocalStoreCustomizer.Settings settings = new RateLimitLocalStoreCustomizer.Settings() {
            @Override
            public RateLimitLocalStoreCustomizer.Settings statistics(RateLimitLocalStoreCustomizer.Statistics s) {
                suppliedStats.add(Objects.requireNonNull(s, "statistics"));
                return this;
            }
        };
        for (RateLimitLocalStoreCustomizer customizer : customizers) {
            customizer.customize(rule, settings);
        }
        if (!suppliedStats.isEmpty()) {
            List<RateLimitLocalStoreCustomizer.Statistics> effectiveStats = List.copyOf(suppliedStats);
            builder.recordStats(new Supplier<>() {
                @Override
                public StatsCounter get() {
                    return new StatisticsCounter(effectiveStats);
                }
            });
        }
    }

    /**
     * Reports what Caffeine records to the statistics of the customizers: a lookup is a hit or a miss, an atomic update
     * of a key is a load, and a removal by size or by expiry an eviction.
     */
    private record StatisticsCounter(List<RateLimitLocalStoreCustomizer.Statistics> statistics) implements StatsCounter {

        @Override
        public void recordHits(int count) {
            for (RateLimitLocalStoreCustomizer.Statistics s : statistics) {
                s.hits(count);
            }
        }

        @Override
        public void recordMisses(int count) {
            for (RateLimitLocalStoreCustomizer.Statistics s : statistics) {
                s.misses(count);
            }
        }

        @Override
        public void recordLoadSuccess(long loadTime) {
            updated();
        }

        @Override
        public void recordLoadFailure(long loadTime) {
            updated();
        }

        private void updated() {
            for (RateLimitLocalStoreCustomizer.Statistics s : statistics) {
                s.updated();
            }
        }

        @Override
        public void recordEviction(int weight, RemovalCause cause) {
            for (RateLimitLocalStoreCustomizer.Statistics s : statistics) {
                if (cause == RemovalCause.SIZE) {
                    s.evicted();
                } else if (cause == RemovalCause.EXPIRED) {
                    s.expired();
                }
            }
        }

        @Override
        public CacheStats snapshot() {
            return CacheStats.empty();
        }
    }
}
