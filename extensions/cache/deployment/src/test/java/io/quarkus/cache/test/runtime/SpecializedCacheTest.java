package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.Function;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.CacheResult;
import io.quarkus.cache.CaffeineCache;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class SpecializedCacheTest {

    private static final String CACHE_NAME = "test-cache";

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot(jar -> jar.addClass(CachedService.class));

    @Inject
    CacheManager cacheManager;

    @Test
    public void testSuccessfulAs() {
        cacheManager.getCache(CACHE_NAME).get().as(CaffeineCache.class);
    }

    @Test
    public void testFailedAs() {
        assertThrows(IllegalStateException.class, () -> {
            cacheManager.getCache(CACHE_NAME).get().as(UnknownCacheType.class);
        });
    }

    @Singleton
    static class CachedService {

        @CacheResult(cacheName = CACHE_NAME)
        public Object cachedMethod(Object key) {
            return new Object();
        }
    }

    static class UnknownCacheType implements Cache {

        @Override
        public String getName() {
            throw new UnsupportedOperationException("This method is not tested here");
        }

        @Override
        public Object getDefaultKey() {
            throw new UnsupportedOperationException("This method is not tested here");
        }

        @Override
        public <K, V> Uni<V> get(K key, Function<K, V> valueLoader) {
            throw new UnsupportedOperationException("This method is not tested here");
        }

        @Override
        public Uni<Void> invalidate(Object key) {
            throw new UnsupportedOperationException("This method is not tested here");
        }

        @Override
        public Uni<Void> invalidateAll() {
            throw new UnsupportedOperationException("This method is not tested here");
        }

        @Override
        public <T extends Cache> T as(Class<T> cacheType) {
            throw new UnsupportedOperationException("This method is not tested here");
        }
    }
}
