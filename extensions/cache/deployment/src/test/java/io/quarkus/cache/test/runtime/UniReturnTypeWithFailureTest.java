package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheResult;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;
import io.vertx.core.impl.NoStackTraceException;

public class UniReturnTypeWithFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot((jar) -> jar.addClass(CachedService.class));

    @Inject
    CachedService cachedService;

    @Test
    void testCacheResult() {
        assertThrows(NoStackTraceException.class, () -> cachedService.cacheResult("k1").await().indefinitely());
        assertEquals(1, cachedService.getCacheResultInvocations());
        assertEquals("", cachedService.cacheResult("k1").await().indefinitely());
        assertEquals(2, cachedService.getCacheResultInvocations());
        assertEquals("", cachedService.cacheResult("k1").await().indefinitely());
        assertEquals(2, cachedService.getCacheResultInvocations());
    }

    @ApplicationScoped
    static class CachedService {

        private volatile int cacheResultInvocations;

        @CacheResult(cacheName = "test-cache")
        public Uni<String> cacheResult(String key) {
            cacheResultInvocations++;
            if (cacheResultInvocations == 1) {
                return Uni.createFrom().failure(new NoStackTraceException("dummy"));
            }
            return Uni.createFrom().item(() -> new String());
        }

        public int getCacheResultInvocations() {
            return cacheResultInvocations;
        }
    }
}
