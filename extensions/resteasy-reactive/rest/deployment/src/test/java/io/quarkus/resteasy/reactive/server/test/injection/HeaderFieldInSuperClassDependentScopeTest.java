package io.quarkus.resteasy.reactive.server.test.injection;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.assertj.core.api.Assertions;
import org.jboss.resteasy.reactive.RestHeader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class HeaderFieldInSuperClassDependentScopeTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(AbstractResource.class, Resource.class))
            .assertException(t -> {
                org.junit.jupiter.api.Assertions.assertEquals(DeploymentException.class, t.getClass());
            });

    @Test
    public void test() {
        Assertions.fail("should never have run");
    }

    @Path("/test")
    @Dependent
    public static class Resource extends AbstractResource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String hello() {
            return "foo: " + foo + ", bar: " + bar;
        }
    }

    public static class AbstractResource {
        @HeaderParam("foo")
        String foo;

        @RestHeader
        String bar;
    }
}
