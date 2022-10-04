package org.jboss.resteasy.reactive.server.vertx.test.simple;

import static io.restassured.RestAssured.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.IOException;
import java.lang.annotation.Retention;

import javax.ws.rs.GET;
import javax.ws.rs.NameBinding;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.smallrye.mutiny.Uni;

public class NameBindingWithInterfaceTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BlockingHelloResource.class, ReactiveHelloResource.class, BlockingHelloApi.class,
                            ReactiveHelloApi.class, AddTestHeaderContainerRequestFilter.class));

    @Test
    public void blockingHello() {
        get("/blocking-hello")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("hello"))
                .header("test", "some-value");
    }

    @Test
    public void reactiveHello() {
        get("/reactive-hello")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("hello"))
                .header("test", "some-value");
    }

    @SomeFilter
    public static class BlockingHelloResource implements BlockingHelloApi {

        @Override
        public String sayHello() {
            return "hello";
        }
    }

    @SomeFilter
    public static class ReactiveHelloResource implements ReactiveHelloApi {

        @Override
        public Uni<String> sayHello() {
            return Uni.createFrom().item("hello");
        }
    }

    @Path("blocking-hello")
    public interface BlockingHelloApi {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        String sayHello();
    }

    @Path("reactive-hello")
    public interface ReactiveHelloApi {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        Uni<String> sayHello();
    }

    @NameBinding
    @Retention(RUNTIME)
    @interface SomeFilter {
    }

    @Provider
    @SomeFilter
    public static class AddTestHeaderContainerRequestFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
                throws IOException {
            responseContext.getHeaders().putSingle("test", "some-value");

        }
    }
}
