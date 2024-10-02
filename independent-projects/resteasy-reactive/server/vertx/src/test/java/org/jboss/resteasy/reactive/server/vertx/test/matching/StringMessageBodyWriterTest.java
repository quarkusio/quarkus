package org.jboss.resteasy.reactive.server.vertx.test.matching;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.restassured.http.Header;

public class StringMessageBodyWriterTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class);
                }
            });

    @Test
    void testHelloEndpoint() {
        given()
                .when().get("/hello")
                .then()
                .statusCode(200)
                .body(is("Greeting response: Hello from Quarkus REST"));
    }

    @Test
    void testWithNoAcceptHeader() {
        // Prevent RestAssured from setting any Accept header
        final var header = new Header("Accept", null);

        given()
                .when()
                .header(header)
                .get("/hello")
                .then()
                .statusCode(200)
                .body(is("Greeting response: Hello from Quarkus REST"));
    }

    @Path("/hello")
    public static class GreetingResource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public Response hello() {
            return Response.ok("Hello from Quarkus REST").build();
        }
    }

    @Provider
    public static class GreetingMessageBodyWriter implements MessageBodyWriter<String> {

        @Override
        public boolean isWriteable(final Class<?> aClass, final Type type, final Annotation[] annotations,
                final MediaType mediaType) {
            return String.class.isAssignableFrom(aClass) && MediaType.TEXT_PLAIN_TYPE.isCompatible(mediaType);
        }

        @Override
        public void writeTo(final String s, final Class<?> aClass, final Type type, final Annotation[] annotations,
                final MediaType mediaType,
                final MultivaluedMap<String, Object> multivaluedMap, final OutputStream outputStream)
                throws IOException, WebApplicationException {

            final var content = "Greeting response: " + s;
            outputStream.write(content.getBytes());
        }
    }
}
