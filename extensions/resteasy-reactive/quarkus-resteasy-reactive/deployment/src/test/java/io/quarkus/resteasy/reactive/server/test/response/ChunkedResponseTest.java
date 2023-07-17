package io.quarkus.resteasy.reactive.server.test.response;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import jakarta.annotation.Priority;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ChunkedResponseTest {

    private static final String LARGE_HELLO_STRING = "h" + "e".repeat(256) + "llo";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloResource.class)
                    .addAsResource(new StringAsset("quarkus.resteasy-reactive.output-buffer-size = 256"),
                            "application.properties"));

    @Test
    void chunked() {
        when()
                .get("/hello/big")
                .then().statusCode(200)
                .body(equalTo(LARGE_HELLO_STRING))
                .header("Transfer-encoding", "chunked");
    }

    @Test
    void notChunked() {
        when()
                .get("/hello/small")
                .then().statusCode(200)
                .body(equalTo("hello"))
                .header("Transfer-encoding", nullValue());
    }

    @Path("hello")
    public static final class HelloResource {

        @GET
        @Path("big")
        public String helloBig() {
            return LARGE_HELLO_STRING;
        }

        @GET
        @Path("small")
        public String helloSmall() {
            return "hello";
        }
    }

    @Provider
    public static class CustomStringMessageBodyWriter implements ServerMessageBodyWriter<String> {

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target, MediaType mediaType) {
            return true;
        }

        @Override
        public void writeResponse(String o, Type genericType, ServerRequestContext context) throws WebApplicationException {
            context.serverResponse().end(o);
        }

        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return true;
        }

        public void writeTo(String o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
                throws IOException, WebApplicationException {
            entityStream.write(o.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Provider
    @Priority(Priorities.USER + 1) // the spec says that when it comes to writers, higher number means higher priority...
    public static final class CustomStringMessageBodyWriter2 extends CustomStringMessageBodyWriter {

        @Override
        public void writeResponse(String o, Type genericType, ServerRequestContext context)
                throws WebApplicationException {

            try (OutputStream stream = context.getOrCreateOutputStream()) {
                stream.write(o.getBytes());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
