package io.quarkus.cache.infinispan;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CacheResult;
import io.quarkus.cache.CaffeineCache;
import io.quarkus.cache.infinispan.runtime.InfinispanCacheImpl;
import io.quarkus.test.QuarkusExtensionTest;

class MixedCacheBackendsTest {

    private static final String CAFFEINE_CACHE_NAME = "caffeine-cache";
    private static final String INFINISPAN_CACHE_NAME = "infinispan-cache";

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(CachedService.class))
            .overrideConfigKey("quarkus.cache.type", "caffeine")
            .overrideConfigKey("quarkus.cache.infinispan-cache.type", "infinispan");

    @Inject
    @CacheName(CAFFEINE_CACHE_NAME)
    Cache caffeineCache;

    @Inject
    @CacheName(INFINISPAN_CACHE_NAME)
    Cache infinispanCache;

    @Test
    void usesConfiguredBackendForEachCache() {
        assertThat(caffeineCache).isInstanceOf(CaffeineCache.class);
        assertThat(infinispanCache).isInstanceOf(InfinispanCacheImpl.class);
    }

    @ApplicationScoped
    static class CachedService {

        @CacheResult(cacheName = INFINISPAN_CACHE_NAME)
        String cached(String key) {
            return key;
        }
    }
}
