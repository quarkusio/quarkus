package io.quarkus.cache.test.deployment;

import static java.util.Arrays.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.stream.Stream;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.deployment.exception.PrivateMethodTargetException;
import io.quarkus.test.QuarkusUnitTest;

/**
 * This class tests {@link DeploymentException} causes related to caching annotations on private methods.
 */
public class PrivateMethodExceptionsTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(TestResource.class)
                    // These checks are only useful if Arc's more generic check is disabled.
                    .addAsResource(new StringAsset("quarkus.arc.fail-on-intercepted-private-method=false"),
                            "application.properties"))
            .assertException(t -> {
                assertEquals(DeploymentException.class, t.getClass());
                assertEquals(3, t.getSuppressed().length);
                assertPrivateMethodTargetException(t, "shouldThrowPrivateMethodTargetException", 1);
                assertPrivateMethodTargetException(t, "shouldAlsoThrowPrivateMethodTargetException", 2);
            });

    private static void assertPrivateMethodTargetException(Throwable t, String expectedMethodName, long expectedCount) {
        assertEquals(expectedCount, filterSuppressed(t, PrivateMethodTargetException.class)
                .filter(s -> expectedMethodName.equals(s.getMethodInfo().name())).count());
    }

    private static <T extends RuntimeException> Stream<T> filterSuppressed(Throwable t, Class<T> filterClass) {
        return stream(t.getSuppressed()).filter(filterClass::isInstance).map(filterClass::cast);
    }

    @Test
    public void shouldNotBeInvoked() {
        fail("This method should not be invoked");
    }

    @Path("/test")
    static class TestResource {

        @GET
        // Single annotation test.
        @CacheInvalidateAll(cacheName = "should-throw-private-method-target-exception")
        private void shouldThrowPrivateMethodTargetException() {
        }

        @GET
        // Repeated annotations test.
        @CacheInvalidate(cacheName = "should-throw-private-method-target-exception")
        @CacheInvalidate(cacheName = "should-throw-private-method-target-exception")
        private void shouldAlsoThrowPrivateMethodTargetException() {
        }

    }

}
