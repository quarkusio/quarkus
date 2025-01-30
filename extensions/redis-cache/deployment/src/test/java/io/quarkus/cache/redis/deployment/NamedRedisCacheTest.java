package io.quarkus.cache.redis.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusUnitTest;

public class NamedRedisCacheTest {

    private static final String KEY_1 = "1";
    private static final String KEY_2 = "2";

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(SimpleCachedService.class, TestUtil.class))
            .overrideConfigKey("quarkus.redis.test.hosts", "${quarkus.redis.hosts}/1")
            .overrideConfigKey("quarkus.cache.redis.client-name", "test");

    @Inject
    SimpleCachedService simpleCachedService;

    @Test
    public void testAllCacheAnnotations() {
        RedisDataSource redisDataSource = Arc.container().select(RedisDataSource.class,
                RedisClientName.Literal.of("test")).get();
        List<String> allKeysAtStart = TestUtil.allRedisKeys(redisDataSource);

        // STEP 1
        // Action: @CacheResult-annotated method call.
        // Expected effect: method invoked and result cached.
        // Verified by: STEP 2.
        String value1 = simpleCachedService.cachedMethod(KEY_1);
        assertEquals(allKeysAtStart.size() + 1, TestUtil.allRedisKeys(redisDataSource).size());

        // STEP 2
        // Action: same call as STEP 1.
        // Expected effect: method not invoked and result coming from the cache.
        // Verified by: same object reference between STEPS 1 and 2 results.
        String value2 = simpleCachedService.cachedMethod(KEY_1);
        assertEquals(value1, value2);
        assertEquals(allKeysAtStart.size() + 1, TestUtil.allRedisKeys(redisDataSource).size());

        // STEP 3
        // Action: same call as STEP 2 with a new key.
        // Expected effect: method invoked and result cached.
        // Verified by: different objects references between STEPS 2 and 3 results.
        String value3 = simpleCachedService.cachedMethod(KEY_2);
        assertNotEquals(value2, value3);
        assertEquals(allKeysAtStart.size() + 2, TestUtil.allRedisKeys(redisDataSource).size());

        // STEP 4
        // Action: cache entry invalidation.
        // Expected effect: STEP 2 cache entry removed.
        // Verified by: STEP 5.
        simpleCachedService.invalidate(KEY_1);
        assertEquals(allKeysAtStart.size() + 1, TestUtil.allRedisKeys(redisDataSource).size());

        // STEP 5
        // Action: same call as STEP 2.
        // Expected effect: method invoked because of STEP 4 and result cached.
        // Verified by: different objects references between STEPS 2 and 5 results.
        String value5 = simpleCachedService.cachedMethod(KEY_1);
        assertNotEquals(value2, value5);
        assertEquals(allKeysAtStart.size() + 2, TestUtil.allRedisKeys(redisDataSource).size());

        // STEP 6
        // Action: same call as STEP 3.
        // Expected effect: method not invoked and result coming from the cache.
        // Verified by: same object reference between STEPS 3 and 6 results.
        String value6 = simpleCachedService.cachedMethod(KEY_2);
        assertEquals(value3, value6);
        assertEquals(allKeysAtStart.size() + 2, TestUtil.allRedisKeys(redisDataSource).size());

        // STEP 7
        // Action: add 100 cached keys, to make sure the SCAN command in next step requires multiple iterations
        // Expected effect: + 100 keys in Redis
        // Verified by: comparison with previous number of keys
        for (int i = 0; i < 100; i++) {
            simpleCachedService.cachedMethod("extra-" + i);
        }
        assertEquals(allKeysAtStart.size() + 102, TestUtil.allRedisKeys(redisDataSource).size());

        // STEP 8
        // Action: full cache invalidation.
        // Expected effect: empty cache.
        // Verified by: comparison with previous number of keys, STEPS 9 and 10.
        simpleCachedService.invalidateAll();
        assertEquals(allKeysAtStart.size(), TestUtil.allRedisKeys(redisDataSource).size());

        // STEP 9
        // Action: same call as STEP 5.
        // Expected effect: method invoked because of STEP 8 and result cached.
        // Verified by: different objects references between STEPS 5 and 9 results.
        String value9 = simpleCachedService.cachedMethod(KEY_1);
        assertNotEquals(value5, value9);

        // STEP 10
        // Action: same call as STEP 6.
        // Expected effect: method invoked because of STEP 8 and result cached.
        // Verified by: different objects references between STEPS 6 and 10 results.
        String value10 = simpleCachedService.cachedMethod(KEY_2);
        assertNotEquals(value6, value10);
    }

}
