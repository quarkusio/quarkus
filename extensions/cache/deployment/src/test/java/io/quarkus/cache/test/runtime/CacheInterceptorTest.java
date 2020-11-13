package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.cache.runtime.CacheInterceptor;
import io.quarkus.cache.runtime.CompositeCacheKey;
import io.quarkus.cache.runtime.DefaultCacheKey;
import io.quarkus.cache.runtime.caffeine.CaffeineCache;
import io.quarkus.cache.runtime.caffeine.CaffeineCacheInfo;

public class CacheInterceptorTest {

    private static final TestCacheInterceptor TEST_CACHE_INTERCEPTOR = new TestCacheInterceptor();

    @Test
    public void testDefaultKey() {
        // We need a CaffeineCache instance to test the default key logic.
        CaffeineCacheInfo cacheInfo = new CaffeineCacheInfo();
        cacheInfo.name = "test-cache";
        CaffeineCache cache = new CaffeineCache(cacheInfo);

        DefaultCacheKey expectedKey = new DefaultCacheKey(cache.getName());
        Object actualKey = getCacheKey(cache, new short[] {}, new Object[] {});
        assertEquals(expectedKey, actualKey);
    }

    @Test
    public void testExplicitSimpleKey() {
        Object expectedKey = new Object();
        Object actualKey = getCacheKey(new short[] { 1 }, new Object[] { new Object(), expectedKey });
        // A cache key with one element should be the element itself (same object reference).
        assertEquals(expectedKey, actualKey);
    }

    @Test
    public void testExplicitCompositeKey() {
        Object keyElement1 = new Object();
        Object keyElement2 = new Object();
        Object expectedKey = new CompositeCacheKey(keyElement1, keyElement2);
        Object actualKey = getCacheKey(new short[] { 0, 2 }, new Object[] { keyElement1, new Object(), keyElement2 });
        assertEquals(expectedKey, actualKey);
    }

    @Test
    public void testImplicitSimpleKey() {
        Object expectedKey = new Object();
        Object actualKey = getCacheKey(new short[] {}, new Object[] { expectedKey });
        // A cache key with one element should be the element itself (same object reference).
        assertEquals(expectedKey, actualKey);
    }

    @Test
    public void testImplicitCompositeKey() {
        Object keyElement1 = new Object();
        Object keyElement2 = new Object();
        Object expectedKey = new CompositeCacheKey(keyElement1, keyElement2);
        Object actualKey = getCacheKey(new short[] {}, new Object[] { keyElement1, keyElement2 });
        assertEquals(expectedKey, actualKey);
    }

    private Object getCacheKey(CaffeineCache cache, short[] cacheKeyParameterPositions, Object[] methodParameterValues) {
        return TEST_CACHE_INTERCEPTOR.getCacheKey(cache, cacheKeyParameterPositions, methodParameterValues);
    }

    private Object getCacheKey(short[] cacheKeyParameterPositions, Object[] methodParameterValues) {
        return TEST_CACHE_INTERCEPTOR.getCacheKey(null, cacheKeyParameterPositions, methodParameterValues);
    }

    // This inner class changes the CacheInterceptor#getCacheKey method visibility to public.
    private static class TestCacheInterceptor extends CacheInterceptor {
        @Override
        public Object getCacheKey(CaffeineCache cache, short[] cacheKeyParameterPositions, Object[] methodParameterValues) {
            return super.getCacheKey(cache, cacheKeyParameterPositions, methodParameterValues);
        }
    }
}
