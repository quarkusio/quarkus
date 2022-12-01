package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static org.hamcrest.CoreMatchers.equalTo;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class CustomJsonProviderTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(GreetingResource.class, Greeting.class, Greeting2.class, BaseCustomWriter.class,
                                    CustomWriter.class);
                }
            });

    @Test
    public void customWriter() {
        RestAssured.with().accept(ContentType.JSON).get("/greeting/custom")
                .then().statusCode(200).body("message", equalTo("canned"));
    }

    @Test
    public void standardWriter() {
        RestAssured.with().accept(ContentType.JSON).get("/greeting/standard")
                .then().statusCode(200).body("message", equalTo("test"));
    }

    @Path("greeting")
    public static class GreetingResource {

        @Path("custom")
        @GET
        public Greeting get() {
            return new Greeting("test");
        }

        @Path("standard")
        @GET
        public Greeting2 standard() {
            return new Greeting2("test");
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

    public static class Greeting2 {

        private final String message;

        public Greeting2(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    @Produces({ MediaType.APPLICATION_JSON, "application/*+json" })
    public static abstract class BaseCustomWriter implements MessageBodyWriter<Object> {

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return Greeting.class.isAssignableFrom(type);
        }
    }

    @Provider
    public static class CustomWriter extends BaseCustomWriter {
        @Override
        public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
                throws IOException, WebApplicationException {
            entityStream.write("{\"message\": \"canned\"}".getBytes(StandardCharsets.UTF_8));
        }
    }
}
