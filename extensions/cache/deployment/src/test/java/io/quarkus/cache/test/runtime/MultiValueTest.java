package io.quarkus.cache.test.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheResult;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Multi;

/**
 * Tests the {@link CacheResult} annotation on methods returning {@link Multi}.
 */
public class MultiValueTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(CachedService.class));

    @Inject
    CachedService cachedService;

    @Test
    public void test() {

        /*
         * io.smallrye.mutiny.Multi values returned by methods annotated with @CacheResult should never be cached.
         * Let's check that the cached method from this test is executed on each call.
         */
        Multi<String> multi1 = cachedService.cachedMethod();
        assertEquals(1, cachedService.getInvocations());
        Multi<String> multi2 = cachedService.cachedMethod();
        assertEquals(2, cachedService.getInvocations());

        /*
         * io.smallrye.mutiny.Uni emitted items are cached by a callback when the Unis are resolved.
         * We need to make sure this isn't the case for Multi values.
         */
        multi1.collect().asList().await().indefinitely();
        cachedService.cachedMethod();
        assertEquals(3, cachedService.getInvocations());
    }

    @ApplicationScoped
    static class CachedService {

        private int invocations;

        @CacheResult(cacheName = "test-cache")
        public Multi<String> cachedMethod() {
            invocations++;
            return Multi.createFrom().items("We", "are", "not", "cached!");
        }

        public int getInvocations() {
            return invocations;
        }
    }
}
