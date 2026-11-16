package io.quarkus.resteasy.test.providers;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import jakarta.annotation.Priority;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class ApplicationWriterPrecedenceTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class, PrefixingWriter.class));

    @Test
    public void applicationWriterIsPreferredOverBuiltinWriter() {
        when().get("/greeting").then().statusCode(200).body(is("Prefixed: Hello"));
    }

    @Path("/greeting")
    public static class GreetingResource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String hello() {
            return "Hello";
        }
    }

    // ties with the built-in StringTextStar on media type; a lower priority than the default must not let the
    // built-in win, as application providers are preferred over built-in ones
    @Provider
    @Priority(Priorities.USER + 100)
    public static class PrefixingWriter implements MessageBodyWriter<String> {

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return String.class.isAssignableFrom(type) && MediaType.TEXT_PLAIN_TYPE.isCompatible(mediaType);
        }

        @Override
        public void writeTo(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException {
            entityStream.write(("Prefixed: " + s).getBytes(StandardCharsets.UTF_8));
        }
    }
}
