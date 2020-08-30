package io.quarkus.cache.runtime.caffeine;

import static io.quarkus.cache.runtime.NullValueConverter.fromCacheValue;
import static io.quarkus.cache.runtime.NullValueConverter.toCacheValue;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.quarkus.cache.runtime.CacheException;
import io.quarkus.cache.runtime.NullValueConverter;

public class CaffeineCache {

    private AsyncCache<Object, Object> cache;

    private String name;

    private Integer initialCapacity;

    private Long maximumSize;

    private Duration expireAfterWrite;

    private Duration expireAfterAccess;

    public CaffeineCache(CaffeineCacheInfo cacheInfo, Executor executor) {
        this.name = cacheInfo.name;
        Caffeine<Object, Object> builder = Caffeine.newBuilder();
        if (executor != null) {
            builder.executor(executor);
        }
        if (cacheInfo.initialCapacity != null) {
            this.initialCapacity = cacheInfo.initialCapacity;
            builder.initialCapacity(cacheInfo.initialCapacity);
        }
        if (cacheInfo.maximumSize != null) {
            this.maximumSize = cacheInfo.maximumSize;
            builder.maximumSize(cacheInfo.maximumSize);
        }
        if (cacheInfo.expireAfterWrite != null) {
            this.expireAfterWrite = cacheInfo.expireAfterWrite;
            builder.expireAfterWrite(cacheInfo.expireAfterWrite);
        }
        if (cacheInfo.expireAfterAccess != null) {
            this.expireAfterAccess = cacheInfo.expireAfterAccess;
            builder.expireAfterAccess(cacheInfo.expireAfterAccess);
        }
        cache = builder.buildAsync();
    }

    public Object get(Object key, Callable<Object> valueLoader, long lockTimeout) throws Exception {
        if (lockTimeout <= 0) {
            CompletableFuture<Object> cacheValue = cache.get(key, (k, executor) -> {
                return preventCaffeineWarning(CompletableFuture.supplyAsync(new MappingSupplier(valueLoader), executor));
            });
            try {
                return rethrowCacheComputationException(key, cacheValue).get();
            } catch (ExecutionException e) {
                throw getExceptionToThrow(e);
            }
        }

        // The lock timeout logic starts here.

        /*
         * If the current key is not already associated with a value in the Caffeine cache, there's no way to know if the
         * current thread or another one started the missing value computation. The following variable will be used to
         * determine whether or not a timeout should be triggered during the computation depending on which thread started it.
         */
        boolean[] isCurrentThreadComputation = { false };

        CompletableFuture<Object> cacheValue = cache.get(key, (k, executor) -> {
            isCurrentThreadComputation[0] = true;
            return preventCaffeineWarning(CompletableFuture.supplyAsync(new MappingSupplier(valueLoader), executor));
        });

        if (isCurrentThreadComputation[0]) {
            // The value is missing and its computation was started from the current thread.
            // We'll wait for the result no matter how long it takes.
            try {
                return rethrowCacheComputationException(key, cacheValue).get();
            } catch (ExecutionException e) {
                throw getExceptionToThrow(e);
            }
        } else {
            // The value is either already present in the cache or missing and its computation was started from another thread.
            // We want to retrieve it from the cache within the lock timeout delay.
            try {
                return rethrowCacheComputationException(key, cacheValue).get(lockTimeout, TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                throw getExceptionToThrow(e);
            } catch (TimeoutException e) {
                // Timeout triggered! We don't want to wait any longer for the value computation and we'll simply invoke the
                // cached method and return its result without caching it.
                // TODO: Add statistics here to monitor the timeout.
                return valueLoader.call();
            }
        }
    }

    public void invalidate(Object key) {
        cache.synchronous().invalidate(key);
    }

    public void invalidateAll() {
        cache.synchronous().invalidateAll();
    }

    public String getName() {
        return name;
    }

    // For testing purposes only.
    public Integer getInitialCapacity() {
        return initialCapacity;
    }

    // For testing purposes only.
    public Long getMaximumSize() {
        return maximumSize;
    }

    // For testing purposes only.
    public Duration getExpireAfterWrite() {
        return expireAfterWrite;
    }

    // For testing purposes only.
    public Duration getExpireAfterAccess() {
        return expireAfterAccess;
    }

    private CompletableFuture<Object> preventCaffeineWarning(CompletableFuture<Object> cacheValue) {
        return cacheValue.exceptionally(new Function<Throwable, Object>() {
            @Override
            public Object apply(Throwable cause) {
                // This is required to prevent Caffeine from logging unwanted warnings.
                return new CaffeineComputationThrowable(cause);
            }
        });
    }

    private CompletableFuture<Object> rethrowCacheComputationException(Object key, CompletableFuture<Object> cacheValue) {
        return cacheValue.thenApply(new Function<Object, Object>() {
            @Override
            @SuppressWarnings("finally")
            public Object apply(Object value) {
                // If there's a throwable encapsulated into a CaffeineComputationThrowable, it must be rethrown.
                if (value instanceof CaffeineComputationThrowable) {
                    try {
                        // The cache entry needs to be removed from Caffeine explicitly (this would usually happen automatically).
                        cache.asMap().remove(key, cacheValue);
                    } finally {
                        Throwable cause = ((CaffeineComputationThrowable) value).getCause();
                        if (cause instanceof RuntimeException) {
                            throw (RuntimeException) cause;
                        } else {
                            throw new CacheException(cause);
                        }
                    }
                } else {
                    return NullValueConverter.fromCacheValue(value);
                }
            }
        });
    }

    private Exception getExceptionToThrow(ExecutionException e) {
        if (e.getCause() instanceof CacheException && e.getCause().getCause() instanceof Exception) {
            return (Exception) e.getCause().getCause();
        } else {
            /*
             * If:
             * - the cause is not a CacheException
             * - the cause is a CacheException which doesn't have a cause itself
             * - the cause is a CacheException which was caused itself by an Error
             * ... then we'll throw the original ExecutionException.
             */
            return e;
        }
    }

    private static class MappingSupplier implements Supplier<Object> {

        private final Callable<?> valueLoader;

        public MappingSupplier(Callable<?> valueLoader) {
            this.valueLoader = valueLoader;
        }

        @Override
        public Object get() {
            try {
                return toCacheValue(valueLoader.call());
            } catch (Exception e) {
                throw new CacheException(e);
            }
        }
    }
}
