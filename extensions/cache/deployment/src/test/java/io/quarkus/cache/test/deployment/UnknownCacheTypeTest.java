package io.quarkus.cache.test.deployment;

import static org.junit.jupiter.api.Assertions.fail;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.DeploymentException;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheResult;
import io.quarkus.test.QuarkusUnitTest;

public class UnknownCacheTypeTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.cache.type=i_am_an_unknown_cache_type"), "application.properties")
                    .addClass(CachedService.class))
            .setExpectedException(DeploymentException.class);

    @Test
    public void shouldNotBeInvoked() {
        fail("This method should not be invoked");
    }

    @ApplicationScoped
    static class CachedService {

        @CacheResult(cacheName = "test-cache")
        public Object cachedMethod(String key) {
            return new Object();
        }
    }
}
