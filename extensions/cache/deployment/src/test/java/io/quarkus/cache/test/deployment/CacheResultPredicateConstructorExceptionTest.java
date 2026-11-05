package io.quarkus.cache.test.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.DeploymentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheResult;
import io.quarkus.cache.CacheResultPredicate;
import io.quarkus.cache.deployment.exception.CacheResultPredicateConstructorException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A cache result predicate that is neither a CDI bean nor has a default constructor is rejected at build time.
 */
public class CacheResultPredicateConstructorExceptionTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(CachedService.class, NoDefaultConstructorPredicate.class))
            .assertException(t -> {
                assertEquals(DeploymentException.class, t.getClass());
                assertTrue(Stream.concat(Stream.of(t.getCause()), Arrays.stream(t.getSuppressed()))
                        .anyMatch(s -> s instanceof CacheResultPredicateConstructorException e
                                && e.getClassInfo().name().toString().equals(NoDefaultConstructorPredicate.class.getName())),
                        t.getCause() + " " + Arrays.toString(t.getSuppressed()));
            });

    @Test
    public void shouldNotBeInvoked() {
        fail("This method should not be invoked");
    }

    @ApplicationScoped
    static class CachedService {

        @CacheResult(cacheName = "predicate-without-constructor", unless = NoDefaultConstructorPredicate.class)
        public String get(String key) {
            return key;
        }
    }

    static class NoDefaultConstructorPredicate implements CacheResultPredicate {

        NoDefaultConstructorPredicate(String unused) {
        }

        @Override
        public boolean test(Object result) {
            return false;
        }
    }
}
