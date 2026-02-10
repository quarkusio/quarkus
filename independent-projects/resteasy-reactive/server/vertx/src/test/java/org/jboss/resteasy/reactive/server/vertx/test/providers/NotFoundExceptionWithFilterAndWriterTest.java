package org.jboss.resteasy.reactive.server.vertx.test.providers;

import static io.restassured.RestAssured.get;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class NotFoundExceptionWithFilterAndWriterTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(FooResource.class, Foo.class));

    @Test
    public void ok() {
        get("/test")
                .then()
                .statusCode(200)
                .body(equalTo("foo"));
    }

    @Test
    public void notFound() {
        get("/test2")
                .then()
                .statusCode(404)
                .body(equalTo("foo"));
    }

    @Path("/test")
    public static class FooResource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public Foo hello() {
            return new Foo();
        }
    }

    @Provider
    public static class NotFoundMapper implements ExceptionMapper<Exception> {

        @Override
        public Response toResponse(Exception exception) {
            if (exception instanceof NotFoundException nfe) {
                return Response.status(Response.Status.NOT_FOUND)
                        .type(MediaType.TEXT_PLAIN)
                        .entity(new Foo()).build();
            }
            throw new RuntimeException(exception);
        }
    }

    @Provider
    public static class NoOpFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            // do nothing
        }
    }

    @Produces(MediaType.TEXT_PLAIN)
    @Provider
    public static class FooWriter implements MessageBodyWriter<Foo> {

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return true;
        }

        @Override
        public void writeTo(Foo foo, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
                throws IOException, WebApplicationException {
            assertNotNull(annotations);
            entityStream.write("foo".getBytes());
        }
    }

    public static class Foo {

    }
}
