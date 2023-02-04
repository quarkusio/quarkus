package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.fail;

import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.cache.annotation.Cacheable;

import io.quarkus.test.QuarkusUnitTest;

public class AnnotationOnClassTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class).addClass(CachedService.class))
            .setExpectedException(IllegalArgumentException.class);

    @Test
    public void testApplicationShouldNotStart() {
        fail("Application should not start when Spring caching annotations are used on a class instead of a method");
    }

    @Singleton
    @Cacheable(cacheNames = CachedService.CACHE_NAME)
    static class CachedService {

        private static final String CACHE_NAME = "test-cache";

        public String someMethod() {
            return new String();
        }
    }
}
