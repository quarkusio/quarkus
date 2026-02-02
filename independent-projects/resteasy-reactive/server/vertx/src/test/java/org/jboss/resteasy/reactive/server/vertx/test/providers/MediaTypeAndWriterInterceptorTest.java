package org.jboss.resteasy.reactive.server.vertx.test.providers;

import static io.restassured.RestAssured.get;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.WriterInterceptorContext;

import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class MediaTypeAndWriterInterceptorTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Resource.class, Interceptor.class, FooMessageBodyWriter.class));

    @Test
    void testSmallRequestForNonBlocking() {
        get("/test")
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON);
    }

    @Path("test")
    public static class Resource {

        @Produces(MediaType.APPLICATION_JSON)
        @GET
        public Response sayCheese() {
            return Response.ok(new Foo()).build();
        }

    }

    @Provider
    public static class Interceptor implements jakarta.ws.rs.ext.WriterInterceptor {

        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
            context.proceed();
        }
    }

    public static class FooMessageBodyWriter implements jakarta.ws.rs.ext.MessageBodyWriter<Foo> {

        @Override
        public boolean isWriteable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type.equals(Foo.class);
        }

        @Override
        public void writeTo(Foo o, Class type, Type genericType, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap httpHeaders, OutputStream entityStream)
                throws IOException, WebApplicationException {
            if (!mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
                throw new IllegalArgumentException("Wrong media type: " + mediaType);
            }
            entityStream.write("{}".getBytes(StandardCharsets.UTF_8));
        }
    }

    public static class Foo {

    }
}
