package io.quarkus.cache.redis.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.cache.CacheResult;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusUnitTest;

public class ChainedRedisCacheTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(ChainedCachedService.class, TestUtil.class));

    @Inject
    ChainedCachedService chainedCachedService;

    @Test
    public void test() {
        RedisDataSource redisDataSource = Arc.container().select(RedisDataSource.class).get();
        List<String> allKeysAtStart = TestUtil.allRedisKeys(redisDataSource);

        assertEquals("fubar:42", chainedCachedService.cache1("fubar"));

        List<String> allKeysAtEnd = TestUtil.allRedisKeys(redisDataSource);
        assertEquals(allKeysAtStart.size() + 2, allKeysAtEnd.size());
    }

    @ApplicationScoped
    public static class ChainedCachedService {
        @CacheResult(cacheName = "cache1")
        public String cache1(String key) {
            return key + ":" + cache2(42);
        }

        @CacheResult(cacheName = "cache2")
        public int cache2(int value) {
            return value;
        }
    }
}
