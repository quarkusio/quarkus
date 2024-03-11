package io.quarkus.rest.client.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import java.net.URI;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class CustomMessageBodyReaderUsesAnnotationsTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest();

    @TestHTTPResource
    URI baseUri;

    private Client client;

    @BeforeEach
    public void before() {
        client = QuarkusRestClientBuilder.newBuilder()
                .baseUri(baseUri)
                .register(PersonMessageBodyReader.class)
                .build(Client.class);
    }

    @Test
    public void fromAnnotation() {
        Person person = client.fromAnnotation();
        assertEquals("from-annotation", person.name());
    }

    @Test
    public void unset() {
        Person person = client.unset();
        assertEquals("unset", person.name());
    }

    @Path("test")
    public interface Client {

        @GET
        @PersonName("from-annotation")
        Person fromAnnotation();

        @GET
        Person unset();
    }

    @Path("test")
    public static class Endpoint {

        @GET
        public Person get() {
            return new Person("dummy");
        }
    }

    public record Person(String name) {

    }

    @Documented
    @Target({ ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PersonName {
        String value();
    }

    public static class PersonMessageBodyReader implements MessageBodyReader<Person> {

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type.equals(Person.class);
        }

        @Override
        public Person readFrom(Class<Person> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap<String, String> httpHeaders, InputStream entityStream) {
            PersonName personName = null;
            if (annotations != null) {
                for (Annotation annotation : annotations) {
                    if (annotation instanceof PersonName pn) {
                        personName = pn;
                        break;
                    }
                }
            }
            if (personName == null) {
                return new Person("unset");
            }
            return new Person(personName.value());
        }
    }
}
