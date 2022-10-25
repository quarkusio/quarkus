package io.quarkus.resteasy.reactive.server.test.multipart;

import static io.restassured.RestAssured.given;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.Supplier;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.common.providers.serialisers.PrimitiveBodyHandler;
import org.jboss.resteasy.reactive.server.multipart.MultipartPartReadingException;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class MalformedMultipartInputTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(MyEnumMessageBodyReader.class, TestMapper.class,
                                    TestEndpoint.class,
                                    Input.class, MyEnum.class);
                }

            });

    @Test
    public void properInput() {
        given()
                .multiPart("format", "FOO", "text/myenum")
                .accept("text/plain")
                .when()
                .post("/test")
                .then()
                .statusCode(200);
    }

    @Test
    public void malformedInput() {
        given()
                .multiPart("format", "FOO2", "text/myenum")
                .accept("text/plain")
                .when()
                .post("/test")
                .then()
                .statusCode(999);
    }

    @Path("test")
    public static class TestEndpoint {

        @Produces(MediaType.TEXT_PLAIN)
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @POST
        public MyEnum test(@MultipartForm Input formData) {
            return formData.format;
        }
    }

    public static class Input {
        @RestForm
        @PartType("text/myenum")
        public MyEnum format;
    }

    public enum MyEnum {
        FOO,
        BAR
    }

    @Provider
    public static class TestMapper implements ExceptionMapper<MultipartPartReadingException> {
        @Override
        public Response toResponse(MultipartPartReadingException e) {
            return Response.status(999).build();
        }
    }

    @Provider
    @Consumes("text/myenum")
    public static class MyEnumMessageBodyReader extends PrimitiveBodyHandler implements ServerMessageBodyReader<MyEnum> {
        @Override
        public boolean isReadable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo lazyMethod,
                MediaType mediaType) {
            return type.equals(MyEnum.class);
        }

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type.equals(MyEnum.class);
        }

        @Override
        public MyEnum readFrom(Class<MyEnum> type, Type genericType, MediaType mediaType, ServerRequestContext context)
                throws WebApplicationException, IOException {
            try {
                return MyEnum.valueOf(readFrom(context.getInputStream(), false));
            } catch (IllegalArgumentException e) {
                throw new IOException(e);
            }
        }

        @Override
        public MyEnum readFrom(Class<MyEnum> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
                throws IOException, WebApplicationException {
            try {
                return MyEnum.valueOf(readFrom(entityStream, false));
            } catch (IllegalArgumentException e) {
                throw new IOException(e);
            }
        }
    }

}
