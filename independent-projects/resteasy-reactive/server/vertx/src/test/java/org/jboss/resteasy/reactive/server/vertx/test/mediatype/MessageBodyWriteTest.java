package org.jboss.resteasy.reactive.server.vertx.test.mediatype;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.StringStartsWith.startsWith;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Supplier;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class MessageBodyWriteTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(InvalidContentTypeTest.HelloResource.class);
                }
            });

    @Test
    public void test() {
        given().when().get("/test").then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Type-Copy", startsWith(MediaType.APPLICATION_JSON));
    }

    @Path("test")
    public static class TestResource {

        @GET
        public Response response() {
            return Response.ok(Map.of("key", "value")).build();
        }
    }

    @Provider
    @Produces(MediaType.APPLICATION_JSON)
    public static class GenericJSONSerializer implements MessageBodyWriter<Object> {
        @Override
        public long getSize(final Object value, final Class<?> type, final Type genericType,
                final Annotation[] annotations, final MediaType mediaType) {
            return -1;
        }

        @Override
        public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                final MediaType mediaType) {
            return true;
        }

        @Override
        public void writeTo(final Object value, final Class<?> type, final Type genericType,
                final Annotation[] annotations, final MediaType mediaType,
                final MultivaluedMap<String, Object> httpHeaders, final OutputStream entityStream)
                throws IOException {

            httpHeaders.add("Content-Type-Copy", mediaType.toString());
            entityStream.write("{\"foo\": \"bar\"}".getBytes(StandardCharsets.UTF_8));
        }
    }
}
