package io.quarkus.cache.test.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheResult;
import io.quarkus.cache.runtime.CacheRepository;
import io.quarkus.cache.runtime.caffeine.CaffeineCache;
import io.quarkus.test.QuarkusUnitTest;

public class CacheConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class).addClass(TestResource.class).addAsResource(
                            "cache-config-test.properties", "application.properties"));

    private static final String CACHE_NAME = "test-cache";

    @Inject
    CacheRepository cacheRepository;

    @Test
    public void testConfig() {
        CaffeineCache cache = (CaffeineCache) cacheRepository.getCache(CACHE_NAME);
        assertEquals(10, cache.getInitialCapacity());
        assertEquals(100L, cache.getMaximumSize());
        assertEquals(Duration.ofSeconds(30L), cache.getExpireAfterWrite());
        assertEquals(Duration.ofDays(2L), cache.getExpireAfterAccess());
    }

    @Path("/test")
    public static class TestResource {

        @GET
        @CacheResult(cacheName = CACHE_NAME)
        public String foo(String key) {
            return "bar";
        }
    }
}
