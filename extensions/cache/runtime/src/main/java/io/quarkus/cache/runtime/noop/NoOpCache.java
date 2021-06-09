package io.quarkus.cache.runtime.noop;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import io.quarkus.cache.runtime.AbstractCache;
import io.smallrye.mutiny.Uni;

/**
 * This class is an internal Quarkus cache implementation. Do not use it explicitly from your Quarkus application. The public
 * methods signatures may change without prior notice.
 */
public class NoOpCache extends AbstractCache {

    private static final String NAME = NoOpCache.class.getName();

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public CompletableFuture<Object> get(Object key, Function<Object, Object> valueLoader) {
        CompletableFuture<Object> cacheValue = new CompletableFuture<Object>();
        try {
            Object value = valueLoader.apply(key);
            cacheValue.complete(value);
        } catch (Throwable t) {
            cacheValue.completeExceptionally(t);
        }
        return cacheValue;
    }

    @Override
    public void invalidate(Object key) {
    }

    @Override
    public void invalidateAll() {
    }

    @Override
    public Uni<Void> replaceUniValue(Object key, Object emittedValue) {
        return Uni.createFrom().voidItem();
    }
}
