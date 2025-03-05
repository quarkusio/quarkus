package io.quarkus.resteasy.reactive.server.test.injection;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.assertj.core.api.Assertions;
import org.jboss.resteasy.reactive.RestPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class PathFieldApplicationScopeTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(Resource.class))
            .assertException(t -> {
                org.junit.jupiter.api.Assertions.assertEquals(DeploymentException.class, t.getClass());
            });

    @Test
    public void test() {
        Assertions.fail("should never have run");
    }

    @Path("/test")
    @Singleton
    public static class Resource {

        @RestPath
        String id;

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        @Path("/{id}")
        public String hello() {
            return "id: " + id;
        }
    }
}
