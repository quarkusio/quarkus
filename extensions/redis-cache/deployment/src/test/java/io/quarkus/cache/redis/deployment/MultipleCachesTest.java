package io.quarkus.cache.redis.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.cache.CacheResult;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusUnitTest;

public class MultipleCachesTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(CachedService.class, TestUtil.class))
            .overrideConfigKey("quarkus.cache.redis.cache2.prefix", "dummy");

    @Inject
    CachedService cachedService;

    @Test
    public void test() {
        RedisDataSource redisDataSource = Arc.container().select(RedisDataSource.class).get();
        List<String> allKeysAtStart = TestUtil.allRedisKeys(redisDataSource);

        String key1FromCache1 = cachedService.cache1("1");
        assertEquals(key1FromCache1, cachedService.cache1("1"));
        List<String> newKeys = TestUtil.allRedisKeys(redisDataSource);
        assertEquals(allKeysAtStart.size() + 1, newKeys.size());
        Assertions.assertThat(newKeys).contains(expectedCache1Key("1"));

        String key2FromCache1 = cachedService.cache1("2");
        assertNotEquals(key2FromCache1, key1FromCache1);
        newKeys = TestUtil.allRedisKeys(redisDataSource);
        assertEquals(allKeysAtStart.size() + 2, newKeys.size());
        Assertions.assertThat(newKeys).contains(expectedCache1Key("1"), expectedCache1Key("2"));

        Double key1FromCache2 = cachedService.cache2(1d);
        assertEquals(key1FromCache2, cachedService.cache2(1d));
        newKeys = TestUtil.allRedisKeys(redisDataSource);
        assertEquals(allKeysAtStart.size() + 3, newKeys.size());
        Assertions.assertThat(newKeys).contains(expectedCache1Key("1"), expectedCache1Key("2"),
                expectedCache2Key("1.0"));

        Double key2FromCache2 = cachedService.cache2(2d);
        assertNotEquals(key2FromCache2, key1FromCache2);
        newKeys = TestUtil.allRedisKeys(redisDataSource);
        assertEquals(allKeysAtStart.size() + 4, newKeys.size());
        Assertions.assertThat(newKeys).contains(expectedCache1Key("1"), expectedCache1Key("2"),
                expectedCache2Key("1.0"), expectedCache2Key("2.0"));
    }

    private static String expectedCache1Key(String key) {
        return "cache:" + CachedService.CACHE1 + ":" + key;
    }

    private static String expectedCache2Key(String key) {
        return "dummy:" + key;
    }

    @ApplicationScoped
    public static class CachedService {

        static final String CACHE1 = "cache1";
        static final String CACHE2 = "cache2";

        @CacheResult(cacheName = CACHE1)
        public String cache1(String key) {
            return UUID.randomUUID().toString();
        }

        @CacheResult(cacheName = CACHE2)
        public double cache2(double in) {
            return ThreadLocalRandom.current().nextDouble();
        }
    }
}
