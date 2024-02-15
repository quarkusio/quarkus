package io.quarkus.cache.redis.deployment;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.redis.runtime.RedisCache;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusUnitTest;

public class RedisCacheWithOptimisticLockingTest {

    private static final String KEY_1 = "1";
    private static final String KEY_2 = "2";

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(SimpleCachedService.class, TestUtil.class))
            .overrideRuntimeConfigKey("quarkus.cache.redis.use-optimistic-locking", "true");

    @Inject
    SimpleCachedService simpleCachedService;

    @Test
    public void testTypes() {
        CacheManager cacheManager = Arc.container().select(CacheManager.class).get();
        assertNotNull(cacheManager);

        Optional<Cache> cacheOpt = cacheManager.getCache(SimpleCachedService.CACHE_NAME);
        assertTrue(cacheOpt.isPresent());

        Cache cache = cacheOpt.get();
        assertTrue(cache instanceof RedisCache);
    }

    @Test
    public void testAllCacheAnnotations() {
        RedisDataSource redisDataSource = Arc.container().select(RedisDataSource.class).get();
        List<String> allKeysAtStart = TestUtil.allRedisKeys(redisDataSource);

        // STEP 1
        // Action: @CacheResult-annotated method call.
        // Expected effect: method invoked and result cached.
        // Verified by: STEP 2.
        String value1 = simpleCachedService.cachedMethod(KEY_1);
        List<String> newKeys = TestUtil.allRedisKeys(redisDataSource);
        assertEquals(allKeysAtStart.size() + 1, newKeys.size());
        Assertions.assertThat(newKeys).contains(expectedCacheKey(KEY_1));

        // STEP 2
        // Action: same call as STEP 1.
        // Expected effect: method not invoked and result coming from the cache.
        // Verified by: same object reference between STEPS 1 and 2 results.
        String value2 = simpleCachedService.cachedMethod(KEY_1);
        assertEquals(value1, value2);
        assertEquals(allKeysAtStart.size() + 1,
                TestUtil.allRedisKeys(redisDataSource).size());

        // STEP 3
        // Action: same call as STEP 2 with a new key.
        // Expected effect: method invoked and result cached.
        // Verified by: different objects references between STEPS 2 and 3 results.
        String value3 = simpleCachedService.cachedMethod(KEY_2);
        assertNotEquals(value2, value3);
        newKeys = TestUtil.allRedisKeys(redisDataSource);
        assertEquals(allKeysAtStart.size() + 2, newKeys.size());
        Assertions.assertThat(newKeys).contains(expectedCacheKey(KEY_1), expectedCacheKey(KEY_2));

        // STEP 4
        // Action: cache entry invalidation.
        // Expected effect: STEP 2 cache entry removed.
        // Verified by: STEP 5.
        simpleCachedService.invalidate(KEY_1);
        newKeys = TestUtil.allRedisKeys(redisDataSource);
        assertEquals(allKeysAtStart.size() + 1, newKeys.size());
        Assertions.assertThat(newKeys).contains(expectedCacheKey(KEY_2)).doesNotContain(expectedCacheKey(KEY_1));

        // STEP 5
        // Action: same call as STEP 2.
        // Expected effect: method invoked because of STEP 4 and result cached.
        // Verified by: different objects references between STEPS 2 and 5 results.
        String value5 = simpleCachedService.cachedMethod(KEY_1);
        assertNotEquals(value2, value5);
        newKeys = TestUtil.allRedisKeys(redisDataSource);
        assertEquals(allKeysAtStart.size() + 2, newKeys.size());
        Assertions.assertThat(newKeys).contains(expectedCacheKey(KEY_1), expectedCacheKey(KEY_2));

        // STEP 6
        // Action: same call as STEP 3.
        // Expected effect: method not invoked and result coming from the cache.
        // Verified by: same object reference between STEPS 3 and 6 results.
        String value6 = simpleCachedService.cachedMethod(KEY_2);
        assertEquals(value3, value6);
        assertEquals(allKeysAtStart.size() + 2,
                TestUtil.allRedisKeys(redisDataSource).size());

        // STEP 7
        // Action: full cache invalidation.
        // Expected effect: empty cache.
        // Verified by: STEPS 8 and 9.
        simpleCachedService.invalidateAll();
        newKeys = TestUtil.allRedisKeys(redisDataSource);
        assertEquals(allKeysAtStart.size(), newKeys.size());
        Assertions.assertThat(newKeys).doesNotContain(expectedCacheKey(KEY_1), expectedCacheKey(KEY_2));

        // STEP 8
        // Action: same call as STEP 5.
        // Expected effect: method invoked because of STEP 7 and result cached.
        // Verified by: different objects references between STEPS 5 and 8 results.
        String value8 = simpleCachedService.cachedMethod(KEY_1);
        assertNotEquals(value5, value8);

        // STEP 9
        // Action: same call as STEP 6.
        // Expected effect: method invoked because of STEP 7 and result cached.
        // Verified by: different objects references between STEPS 6 and 9 results.
        String value9 = simpleCachedService.cachedMethod(KEY_2);
        assertNotEquals(value6, value9);
    }

    private static String expectedCacheKey(String key) {
        return "cache:" + SimpleCachedService.CACHE_NAME + ":" + key;
    }

}
