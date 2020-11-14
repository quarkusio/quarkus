package io.quarkus.cache.test.deployment;

import static org.junit.jupiter.api.Assertions.fail;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.DeploymentException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CacheResult;
import io.quarkus.test.QuarkusUnitTest;

public class UnknownMethodParameterCacheNameTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(CachedService.class, TestBean.class))
            .setExpectedException(DeploymentException.class);

    @Test
    public void shouldNotBeInvoked() {
        fail("This method should not be invoked");
    }

    @ApplicationScoped
    static class CachedService {

        @CacheResult(cacheName = "test-cache")
        public Object getObject(Object key) {
            return new Object();
        }
    }

    @Dependent
    static class TestBean {

        public void unknownCacheName(@CacheName("unknown-cache") Cache cache) {
        }
    }
}
