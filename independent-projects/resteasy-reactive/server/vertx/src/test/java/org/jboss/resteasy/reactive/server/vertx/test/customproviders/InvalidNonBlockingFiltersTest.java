package org.jboss.resteasy.reactive.server.vertx.test.customproviders;

import static org.junit.jupiter.api.Assertions.fail;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import jakarta.annotation.Priority;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.function.Supplier;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class InvalidNonBlockingFiltersTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(StandardBlockingRequestFilter.class, StandardNonBlockingRequestFilter.class,
                                    DummyResource.class);
                }
            }).assertException(t -> Assertions.assertTrue(t.getMessage().contains("StandardNonBlockingRequestFilter")));

    @Test
    public void test() {
        fail("Should never have been called");
    }

    @Path("dummy")
    public static class DummyResource {

        @Blocking
        @Path("blocking")
        @GET
        public Response blocking() {
            return Response.ok().build();
        }
    }

    @Provider
    public static class StandardBlockingRequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {

        }
    }

    @NonBlocking
    @Provider
    @Priority(Priorities.USER + 1)
    public static class StandardNonBlockingRequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {

        }
    }
}
