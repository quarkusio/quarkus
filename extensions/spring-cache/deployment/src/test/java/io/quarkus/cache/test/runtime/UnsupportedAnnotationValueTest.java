package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.fail;

import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.cache.annotation.Cacheable;

import io.quarkus.test.QuarkusUnitTest;

public class UnsupportedAnnotationValueTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class).addClass(CachedService.class))
            .setExpectedException(IllegalArgumentException.class);

    @Test
    public void testApplicationShouldNotStart() {
        fail("Application should not start when an unsupported annotation value is set");
    }

    @Singleton
    static class CachedService {

        private static final String CACHE_NAME = "test-cache";

        @Cacheable(cacheNames = CachedService.CACHE_NAME, unless = "#result != null")
        public String someMethod(String key) {
            return new String();
        }
    }
}
