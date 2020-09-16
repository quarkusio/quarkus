package io.quarkus.cache.test.deployment;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheResult;
import io.quarkus.test.QuarkusUnitTest;

public class DisabledCacheTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
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
