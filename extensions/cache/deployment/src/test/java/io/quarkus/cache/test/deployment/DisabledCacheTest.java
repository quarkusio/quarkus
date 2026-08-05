package io.quarkus.cache.test.deployment;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheResult;
import io.quarkus.test.QuarkusExtensionTest;

public class DisabledCacheTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.cache.enabled=false"), "application.properties")
                    .addClass(CachedService.class));

    private static final String KEY = "key";

    @Inject
    CachedService cachedService;

    @Test
    public void testEnabledFlagProperty() {
        assertNotEquals(cachedService.cachedMethod(KEY), cachedService.cachedMethod(KEY));
    }

    @Dependent
    static class CachedService {

        @CacheResult(cacheName = "test-cache")
        public Object cachedMethod(String key) {
            return new Object();
        }
    }
}
