package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheKeyGenerator;
import io.quarkus.cache.CaffeineCache;
import io.quarkus.cache.CompositeCacheKey;
import io.quarkus.cache.DefaultCacheKey;
import io.quarkus.cache.runtime.CacheInterceptor;
import io.quarkus.cache.runtime.UndefinedCacheKeyGenerator;
import io.quarkus.cache.runtime.caffeine.CaffeineCacheImpl;
import io.quarkus.cache.runtime.caffeine.CaffeineCacheInfo;

public class CacheInterceptorTest {

    private static final TestCacheInterceptor TEST_CACHE_INTERCEPTOR = new TestCacheInterceptor();

    @Test
    public void testDefaultKey() {
        // We need a CaffeineCache instance to test the default key logic.
        CaffeineCacheInfo cacheInfo = new CaffeineCacheInfo();
        cacheInfo.name = "test-cache";
        CaffeineCache cache = new CaffeineCacheImpl(cacheInfo, false);

        DefaultCacheKey expectedKey = new DefaultCacheKey(cacheInfo.name);
        Object actualKey = getCacheKey(cache, Collections.emptyList(), new Object[] {});
        assertEquals(expectedKey, actualKey);
    }

    @Test
    public void testExplicitSimpleKey() {
        Object expectedKey = new Object();
        Object actualKey = getCacheKey(Arrays.asList((short) 1), new Object[] { new Object(), expectedKey });
        // A cache key with one element should be the element itself (same object reference).
        assertEquals(expectedKey, actualKey);
    }

    @Test
    public void testExplicitCompositeKey() {
        Object keyElement1 = new Object();
        Object keyElement2 = new Object();
        Object expectedKey = new CompositeCacheKey(keyElement1, keyElement2);
        Object actualKey = getCacheKey(Arrays.asList((short) 0, (short) 2),
                new Object[] { keyElement1, new Object(), keyElement2 });
        assertEquals(expectedKey, actualKey);
    }

    @Test
    public void testImplicitSimpleKey() {
        Object expectedKey = new Object();
        Object actualKey = getCacheKey(Collections.emptyList(), new Object[] { expectedKey });
        // A cache key with one element should be the element itself (same object reference).
        assertEquals(expectedKey, actualKey);
    }

    @Test
    public void testImplicitCompositeKey() {
        Object keyElement1 = new Object();
        Object keyElement2 = new Object();
        Object expectedKey = new CompositeCacheKey(keyElement1, keyElement2);
        Object actualKey = getCacheKey(Collections.emptyList(), new Object[] { keyElement1, keyElement2 });
        assertEquals(expectedKey, actualKey);
    }

    private Object getCacheKey(Cache cache, List<Short> cacheKeyParameterPositions, Object[] methodParameterValues) {
        return TEST_CACHE_INTERCEPTOR.getCacheKey(cache, UndefinedCacheKeyGenerator.class, cacheKeyParameterPositions,
                null, methodParameterValues);
    }

    private Object getCacheKey(List<Short> cacheKeyParameterPositions, Object[] methodParameterValues) {
        return TEST_CACHE_INTERCEPTOR.getCacheKey(null, UndefinedCacheKeyGenerator.class, cacheKeyParameterPositions,
                null, methodParameterValues);
    }

    // This inner class changes the CacheInterceptor#getCacheKey method visibility to public.
    private static class TestCacheInterceptor extends CacheInterceptor {
        @Override
        public Object getCacheKey(Cache cache, Class<? extends CacheKeyGenerator> keyGeneratorClass,
                List<Short> cacheKeyParameterPositions, Method method, Object[] methodParameterValues) {
            return super.getCacheKey(cache, keyGeneratorClass, cacheKeyParameterPositions, method,
                    methodParameterValues);
        }
    }
}
