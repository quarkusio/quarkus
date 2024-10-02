package io.quarkus.cache.redis.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusUnitTest;

public class ComplexTypesRedisCacheTest {
    private static final String KEY_1 = "1";
    private static final String KEY_2 = "2";
    private static final String KEY_3 = "3";

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(ComplexCachedService.class, TestUtil.class));

    @Inject
    ComplexCachedService cachedService;

    @Test
    public void testGeneric() {
        RedisDataSource redisDataSource = Arc.container().select(RedisDataSource.class).get();
        List<String> allKeysAtStart = TestUtil.allRedisKeys(redisDataSource);

        // STEP 1
        // Action: @CacheResult-annotated method call.
        // Expected effect: method invoked and result cached.
        // Verified by: STEP 2.
        List<String> value1 = cachedService.genericReturnType(KEY_1);
        List<String> newKeys = TestUtil.allRedisKeys(redisDataSource);
        assertEquals(allKeysAtStart.size() + 1, newKeys.size());
        assertThat(newKeys).contains(expectedCacheKey(ComplexCachedService.CACHE_NAME_GENERIC, KEY_1));

        // STEP 2
        // Action: same call as STEP 1.
        // Expected effect: method not invoked and result coming from the cache.
        // Verified by: same object reference between STEPS 1 and 2 results.
        List<String> value2 = cachedService.genericReturnType(KEY_1);
        assertEquals(value1, value2);
        assertEquals(allKeysAtStart.size() + 1, TestUtil.allRedisKeys(redisDataSource).size());
    }

    @Test
    public void testArray() {
        RedisDataSource redisDataSource = Arc.container().select(RedisDataSource.class).get();
        List<String> allKeysAtStart = TestUtil.allRedisKeys(redisDataSource);

        // STEP 1
        // Action: @CacheResult-annotated method call.
        // Expected effect: method invoked and result cached.
        // Verified by: STEP 2.
        int[] value1 = cachedService.arrayReturnType(KEY_2);
        List<String> newKeys = TestUtil.allRedisKeys(redisDataSource);
        assertEquals(allKeysAtStart.size() + 1, newKeys.size());
        assertThat(newKeys).contains(expectedCacheKey(ComplexCachedService.CACHE_NAME_ARRAY, KEY_2));

        // STEP 2
        // Action: same call as STEP 1.
        // Expected effect: method not invoked and result coming from the cache.
        // Verified by: same object reference between STEPS 1 and 2 results.
        int[] value2 = cachedService.arrayReturnType(KEY_2);
        assertArrayEquals(value1, value2);
        assertEquals(allKeysAtStart.size() + 1, TestUtil.allRedisKeys(redisDataSource).size());
    }

    @Test
    public void testGenericArray() {
        RedisDataSource redisDataSource = Arc.container().select(RedisDataSource.class).get();
        List<String> allKeysAtStart = TestUtil.allRedisKeys(redisDataSource);

        // STEP 1
        // Action: @CacheResult-annotated method call.
        // Expected effect: method invoked and result cached.
        // Verified by: STEP 2.
        List<? extends CharSequence>[] value1 = cachedService.genericArrayReturnType(KEY_3);
        List<String> newKeys = TestUtil.allRedisKeys(redisDataSource);
        assertEquals(allKeysAtStart.size() + 1, newKeys.size());
        assertThat(newKeys).contains(expectedCacheKey(ComplexCachedService.CACHE_NAME_GENERIC_ARRAY, KEY_3));

        // STEP 2
        // Action: same call as STEP 1.
        // Expected effect: method not invoked and result coming from the cache.
        // Verified by: same object reference between STEPS 1 and 2 results.
        List<? extends CharSequence>[] value2 = cachedService.genericArrayReturnType(KEY_3);
        assertArrayEquals(value1, value2);
        assertEquals(allKeysAtStart.size() + 1, TestUtil.allRedisKeys(redisDataSource).size());
    }

    private static String expectedCacheKey(String cacheName, String key) {
        return "cache:" + cacheName + ":" + key;
    }
}
