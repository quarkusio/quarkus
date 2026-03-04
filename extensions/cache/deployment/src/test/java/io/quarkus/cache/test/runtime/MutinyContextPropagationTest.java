package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheResult;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Uni;

public class MutinyContextPropagationTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(CachingService.class));

    @Inject
    CachingService cachingService;

    @Test
    void testMutinyContextPropagatedOnCacheMiss() throws Exception {
        Uni<String> uni = cachingService.cachedUniWithContext();

        String value = uni.awaitUsing(Context.of("hello", "world")).atMost(Duration.ofSeconds(5));
        assertNotNull(value);
        assertTrue(value.contains("hello=world"));
    }

    @Test
    void testMutinyContextPropagatedOnCacheHit() throws Exception {
        Uni<String> uni = cachingService.cachedUniWithContext();

        // First subscription: cache miss
        String value1 = uni.awaitUsing(Context.of("hello", "world")).atMost(Duration.ofSeconds(5));
        assertTrue(value1.contains("hello=world"));

        // Second call: cache hit
        String value2 = uni.awaitUsing(Context.of("different", "context")).atMost(Duration.ofSeconds(5));
        assertTrue(value2.contains("hello=world"));
    }

    @ApplicationScoped
    public static class CachingService {

        // See https://github.com/quarkusio/quarkus/issues/40274
        @CacheResult(cacheName = "mutiny-context-cache")
        public Uni<String> cachedUniWithContext() {
            return Uni.createFrom().context(ctx -> Uni.createFrom().item("Context: " + ctx));
        }
    }
}
