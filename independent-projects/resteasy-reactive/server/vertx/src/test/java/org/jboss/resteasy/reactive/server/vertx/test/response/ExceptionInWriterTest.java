package org.jboss.resteasy.reactive.server.vertx.test.response;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ExceptionInWriterTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest runner = new ResteasyReactiveUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Greeting.class, GreetingResource.class,
                            GreetingWriter.class, GreetingException.class, GreetingExceptionMapper.class));

    @Test
    void nullHeaderTest() {
        when()
                .get("/greeting")
                .then().statusCode(200)
                .body(is("fallback"));
    }

    @Path("/greeting")
    public static class GreetingResource {

        @GET
        public Greeting ok() {
            return new Greeting("hello");
        }
    }

    public static class Greeting {

        private final String message;

        public Greeting(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    @Provider
    @Produces(MediaType.TEXT_PLAIN)
    public static class GreetingWriter implements ServerMessageBodyWriter<Greeting> {

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return true;
        }

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target,
                MediaType mediaType) {
            return true;
        }

        @Override
        public void writeTo(Greeting greeting, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
                throws IOException, WebApplicationException {

            doWrite(greeting, entityStream);
        }

        @Override
        public void writeResponse(Greeting greeting, Type genericType, ServerRequestContext context)
                throws WebApplicationException, IOException {
            doWrite(greeting, context.getOrCreateOutputStream());
        }

        private void doWrite(Greeting greeting, OutputStream entityStream) throws IOException {
            if ("hello".equals(greeting.getMessage())) { // when the greeting comes from the resource method
                entityStream.write("should not exist in final output".getBytes(StandardCharsets.UTF_8));
                GreetingException ioe = new GreetingException("dummy exception");
                ioe.setStackTrace(new StackTraceElement[0]);
                throw ioe;
            } else {
                entityStream.write(greeting.message.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    public static class GreetingException extends IOException {

        public GreetingException(String message) {
            super(message);
        }
    }

    @Provider
    public static class GreetingExceptionMapper implements ExceptionMapper<GreetingException> {

        @Override
        public Response toResponse(GreetingException exception) {
            return Response.status(200).entity(new Greeting("fallback")).build();
        }
    }
}
