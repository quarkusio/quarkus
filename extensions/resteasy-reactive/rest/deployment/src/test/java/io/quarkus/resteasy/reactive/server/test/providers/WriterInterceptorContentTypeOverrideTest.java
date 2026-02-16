package io.quarkus.resteasy.reactive.server.test.providers;

import java.io.IOException;

import jakarta.annotation.Priority;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.WriterInterceptorContext;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class WriterInterceptorContentTypeOverrideTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot(
            jar -> jar.addClasses(MediaTypeResponseInterceptor.class, TestResource.class));

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
            return Response.ok(greeting).type(MediaType.TEXT_PLAIN).build();
        }
    }

    public record Greeting(String message) {
    }

    @Priority(5000)
    @Provider
    public static class MediaTypeResponseInterceptor implements jakarta.ws.rs.ext.WriterInterceptor {

        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
            if (!context.getMediaType().isCompatible(MediaType.TEXT_PLAIN_TYPE)) {
                throw new IllegalStateException("MediaType was not overridden by Response, got: " + context.getMediaType()
                        + " instead of expected: " + MediaType.TEXT_PLAIN);
            }
            context.proceed();
        }
    }
}
