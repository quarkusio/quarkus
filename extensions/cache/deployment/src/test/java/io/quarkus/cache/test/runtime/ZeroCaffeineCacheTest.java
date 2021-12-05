package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheManager;
import io.quarkus.test.QuarkusUnitTest;

/**
 * This test checks that no exception is thrown when a {@link CacheManager} method is called while the extension is enabled and
 * no cache is declared in the application.
 */
public class ZeroCaffeineCacheTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withEmptyApplication();

    @Inject
    CacheManager cacheManager;

    @Test
    public void testNoExceptionThrown() {
        assertTrue(cacheManager.getCacheNames().isEmpty());
        assertFalse(cacheManager.getCache("unknown-cache").isPresent());
    }
}
