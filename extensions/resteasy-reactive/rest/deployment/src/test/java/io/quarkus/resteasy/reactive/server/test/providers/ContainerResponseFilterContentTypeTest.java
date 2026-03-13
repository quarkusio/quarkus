package io.quarkus.resteasy.reactive.server.test.providers;

import java.io.IOException;

import jakarta.annotation.Priority;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ContainerResponseFilterContentTypeTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot(
            jar -> jar.addClasses(MediaTypeContainerResponseFilter.class, TestResource.class));

    @Test
    public void producesMediaTypePresentInWriterInterceptor() {
        RestAssured.when().get("/test").then().body(Matchers.containsString("Hello"));
    }

    @Path("/test")
    public static class TestResource {

        @GET
        @Produces(MediaType.TEXT_XML)
        public Response hello() {
            Greeting greeting = new Greeting("Hello");
            return Response.ok(greeting).build();
        }
    }

    public record Greeting(String message) {
    }

    @Priority(5000)
    @Provider
    public static class MediaTypeContainerResponseFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
                throws IOException {
            if (!responseContext.getMediaType().isCompatible(MediaType.TEXT_XML_TYPE)) {
                throw new IllegalStateException("MediaType was not provided, got: " + responseContext.getMediaType()
                        + " instead of expected: " + MediaType.TEXT_XML);
            }
        }
    }
}
