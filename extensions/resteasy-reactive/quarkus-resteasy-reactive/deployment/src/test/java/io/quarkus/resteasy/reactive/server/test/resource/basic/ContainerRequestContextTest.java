package io.quarkus.resteasy.reactive.server.test.resource.basic;

import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ContainerRequestContextTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClass(HelloResource.class);
                }
            });

    @Test
    public void helloWorldTest() {
        RestAssured.get("/hello")
                .then()
                .body(equalTo("hello foo"));
    }

    @Path("/hello")
    public static class HelloResource {

        private final ContainerRequestContext containerRequestContext;

        public HelloResource(ContainerRequestContext containerRequestContext) {
            this.containerRequestContext = containerRequestContext;
        }

        @GET
        public String hello() {
            return "hello " + containerRequestContext.getProperty("name");
        }

    }

    @Provider
    public static class TestFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext context) throws IOException {
            context.setProperty("name", "foo");
        }
    }
}
