package io.quarkus.resteasy.reactive.server.test.resource.basic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class MediaTypesWithSuffixHandlingTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(() -> {
                JavaArchive archive = ShrinkWrap.create(JavaArchive.class);
                archive.addClasses(TestResource.class, NoSuffixMessageBodyWriter.class, SuffixMessageBodyWriter.class);
                return archive;
            });

    @Test
    public void testWriterWithoutSuffix() {
        RestAssured.get("/test/writer/with-no-suffix")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("result - no suffix writer"));
    }

    @Test
    public void testReaderWithoutSuffix() {
        RestAssured.get("/test/reader/with-no-suffix")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("from reader - result"));
    }

    @Test
    public void testWriterWithSuffix() {
        RestAssured.get("/test/writer/with-suffix")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("result - suffix writer"));
    }

    @Test
    public void testReaderWithSuffix() {
        RestAssured.get("/test/reader/with-suffix")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("from reader suffix - result"));
    }

    @Path("/test")
    public static class TestResource {

        @GET
        @Path("/writer/with-no-suffix")
        @Produces("text/test")
        public String writerSimple() {
            return "result";
        }

        @GET
        @Path("/writer/with-suffix")
        @Produces("text/test+suffix")
        public String writerSuffix() {
            return "result";
        }

        @GET
        @Path("/reader/with-no-suffix")
        @Consumes("text/test")
        public String readerSimple(Object fromReader) {
            return fromReader + " - result";
        }

        @GET
        @Path("/reader/with-suffix")
        @Consumes("text/test+suffix")
        public String readerSuffix(Object fromReader) {
            return fromReader + " - result";
        }
    }

    @Provider
    @Consumes("text/test")
    @Produces("text/test")
    public static class NoSuffixMessageBodyWriter implements ServerMessageBodyWriter<Object>, ServerMessageBodyReader<Object> {

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target, MediaType mediaType) {
            return true;
        }

        @Override
        public void writeResponse(Object o, Type genericType, ServerRequestContext context)
                throws WebApplicationException, IOException {
            String response = (String) o;
            response += " - no suffix writer";
            context.getOrCreateOutputStream().write(response.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            throw new IllegalStateException("should never have been called");
        }

        @Override
        public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
                throws IOException, WebApplicationException {
            throw new IllegalStateException("should never have been called");
        }

        @Override
        public boolean isReadable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo lazyMethod,
                MediaType mediaType) {
            return true;
        }

        @Override
        public Object readFrom(Class<Object> type, Type genericType, MediaType mediaType,
                ServerRequestContext context) throws WebApplicationException {
            return "from reader";
        }

        @Override
        public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
            throw new IllegalStateException("should never have been called");
        }

        @Override
        public Object readFrom(Class<Object> aClass, Type type, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap<String, String> multivaluedMap, InputStream inputStream)
                throws WebApplicationException {
            throw new IllegalStateException("should never have been called");
        }
    }

    @Provider
    @Consumes("text/test+suffix")
    @Produces("text/test+suffix")
    public static class SuffixMessageBodyWriter implements ServerMessageBodyWriter<Object>, ServerMessageBodyReader<Object> {

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target, MediaType mediaType) {
            return true;
        }

        @Override
        public void writeResponse(Object o, Type genericType, ServerRequestContext context)
                throws WebApplicationException, IOException {
            String response = (String) o;
            response += " - suffix writer";
            context.getOrCreateOutputStream().write(response.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            throw new IllegalStateException("should never have been called");
        }

        @Override
        public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
                throws IOException, WebApplicationException {
            throw new IllegalStateException("should never have been called");
        }

        @Override
        public boolean isReadable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo lazyMethod,
                MediaType mediaType) {
            return true;
        }

        @Override
        public Object readFrom(Class<Object> type, Type genericType, MediaType mediaType,
                ServerRequestContext context) throws WebApplicationException {
            return "from reader suffix";
        }

        @Override
        public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
            throw new IllegalStateException("should never have been called");
        }

        @Override
        public Object readFrom(Class<Object> aClass, Type type, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap<String, String> multivaluedMap, InputStream inputStream)
                throws WebApplicationException {
            throw new IllegalStateException("should never have been called");
        }
    }

}
