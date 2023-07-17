package io.quarkus.resteasy.reactive.server.test.resource.basic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.common.providers.serialisers.MessageReaderUtil;
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

public class BasicGenericTypesHandlingTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive archive = ShrinkWrap.create(JavaArchive.class);
                    archive.addClasses(AbstractResource.class, TestResource.class, Input.class, Output.class,
                            TestMessageBodyReader.class, TestMessageBodyWriter.class);
                    return archive;
                }
            });

    @Test
    public void test() {
        RestAssured.with().body("hello").contentType("text/test").post("/test")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("out / hello"));
    }

    public static abstract class AbstractResource<I, O> {

        protected abstract O convert(I i);

        @POST
        @Produces("text/test")
        @Consumes("text/test")
        public O handle(I i) {
            return convert(i);
        }
    }

    @Path("/test")
    public static class TestResource extends AbstractResource<Input, Output> {

        @Override
        protected Output convert(Input input) {
            return new Output("out / " + input.getInMessage());
        }
    }

    public static class Input {

        private final String inMessage;

        public Input(String inMessage) {
            this.inMessage = inMessage;
        }

        public String getInMessage() {
            return inMessage;
        }
    }

    public static class Output {

        private final String outMessage;

        public Output(String outMessage) {
            this.outMessage = outMessage;
        }

        public String getOutMessage() {
            return outMessage;
        }
    }

    @Provider
    @Consumes("text/test")
    public static class TestMessageBodyReader implements ServerMessageBodyReader<Object> {

        @Override
        public boolean isReadable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo lazyMethod,
                MediaType mediaType) {
            return genericType.getTypeName().equals(Input.class.getName());
        }

        @Override
        public Object readFrom(Class<Object> type, Type genericType, MediaType mediaType, ServerRequestContext context)
                throws WebApplicationException, IOException {
            return new Input(MessageReaderUtil.readString(context.getInputStream(), mediaType));
        }

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            throw new IllegalStateException("should never have been called");
        }

        @Override
        public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
                throws IOException, WebApplicationException {
            throw new IllegalStateException("should never have been called");
        }
    }

    @Provider
    @Produces("text/test")
    public static class TestMessageBodyWriter implements ServerMessageBodyWriter<Object> {

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target, MediaType mediaType) {
            return genericType.getTypeName().equals(Output.class.getName());
        }

        @Override
        public void writeResponse(Object o, Type genericType, ServerRequestContext context)
                throws WebApplicationException, IOException {
            if (genericType.getTypeName().equals(Output.class.getName())) {
                context.getOrCreateOutputStream().write(((Output) o).getOutMessage().getBytes(StandardCharsets.UTF_8));
            } else {
                throw new IllegalStateException("Writer called with generic type: " + genericType.getTypeName());
            }
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
    }

}
