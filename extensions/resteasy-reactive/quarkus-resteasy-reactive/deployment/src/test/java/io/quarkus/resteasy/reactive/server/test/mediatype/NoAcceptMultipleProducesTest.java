package io.quarkus.resteasy.reactive.server.test.mediatype;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.http.ContentType;

public class NoAcceptMultipleProducesTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(DummyResource.class, DummyJsonWriter.class);
                }
            });

    @Test
    public void test() {
        when().get("/dummy")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body(is("{\"foo\": \"bar\"}"));
    }

    @Path("dummy")
    public static class DummyResource {

        @Produces({ "text/plain; qs=0", "application/json; qs=1" })
        @GET
        public Map<String, String> dummy() {
            return Collections.emptyMap(); // the return values doesn't matter as the json writer will write whatever it likes
        }

    }

    @Provider
    @Produces("application/json")
    public static class DummyJsonWriter extends ServerMessageBodyWriter.AllWriteableMessageBodyWriter {

        @Override
        public void writeResponse(Object o, Type genericType, ServerRequestContext context)
                throws WebApplicationException, IOException {
            doDummyWrite(context.getOrCreateOutputStream());
        }

        @Override
        public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
                throws IOException, WebApplicationException {
            doDummyWrite(entityStream);
        }

        private void doDummyWrite(OutputStream outputStream) throws IOException {
            outputStream.write("{\"foo\": \"bar\"}".getBytes(StandardCharsets.UTF_8));
        }
    }
}
