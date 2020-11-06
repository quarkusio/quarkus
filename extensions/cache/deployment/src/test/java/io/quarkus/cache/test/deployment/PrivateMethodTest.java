package io.quarkus.cache.test.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import javax.enterprise.inject.spi.DeploymentException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.deployment.exception.IllegalModifierException;
import io.quarkus.test.QuarkusUnitTest;

public class PrivateMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(TestResource.class)).assertException(e -> {
                assertEquals(DeploymentException.class, e.getClass());
                assertEquals(IllegalModifierException.class, e.getCause().getClass());
            });

    @Test
    public void shouldNotBeInvoked() {
        fail("This method should not be invoked");
    }

    @Path("/test")
    static class TestResource {

        @GET
        @CacheInvalidateAll(cacheName = "test-cache")
        private void shouldThrowDeploymentException() {
        }
    }
}
