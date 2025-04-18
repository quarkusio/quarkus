package io.quarkus.rest.client.reactive.subresource;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ClientMessageBodyReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.test.QuarkusUnitTest;

public class ParameterizedParentInterfaceTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(Resource.class)
                    .addClass(ClientLocator.class)
                    .addClass(ClientLocatorBase.class)
                    .addClass(TranslationSubResource.class)
                    .addClass(EnglishSubResource.class)
                    .addClass(ShortsSubResource.class)
                    .addClass(HelloSubResource.class)
                    .addClass(EachCellAsListElementClientMessageBodyReader.class));

    @RestClient
    ClientLocator client;

    @Test
    void testRestCalls() {
        {
            String result = client.string().subResource().subResource().subResource().get();
            assertThat(result).isEqualTo("Hello,World");
        }

        {
            Long result = client.number().subResource().subResource().subResource().get();
            assertThat(result).isEqualTo(42L);
        }

        {
            List<String> result = client.testListOnRoot();
            assertThat(result).contains("Hello", "World");
        }
    }

    @Test
    void testParameterizedTypeWithSameTypeArgumentsAllowed() {
        Assertions.assertDoesNotThrow(() -> QuarkusRestClientBuilder.newBuilder().baseUri(URI.create("http://localhost:8081"))
                .build(SameTypeArgumentInherit.class));
    }

    @Path("")
    public static class Resource {
        @Path("greetings/translations/english/shorts/hello")
        @GET
        public String greet() {
            return "Hello,World";
        }

        @Path("greetings-count/translations/english/shorts/hello")
        @GET
        public Long greetingCount() {
            return 42L;
        }
    }

    public interface ClientLocatorBase<T, V> {
        @Path("greetings/translations")
        TranslationSubResource<T> string();

        @Path("greetings-count/translations")
        TranslationSubResource<V> number();
    }

    @RegisterRestClient(baseUri = "http://localhost:8081")
    @Path("")
    public interface ClientLocator extends ClientLocatorBase<String, Long> {

        @Path("greetings/translations/english/shorts/hello")
        @GET
        List<String> testListOnRoot();
    }

    public interface TranslationSubResourceBaseBase<T> {
        @Path("english")
        EnglishSubResource<T> subResource();
    }

    public interface TranslationSubResourceBase<U> extends TranslationSubResourceBaseBase<U> {
    }

    public interface TranslationSubResource<R> extends TranslationSubResourceBase<R> {
    }

    public interface EnglishSubResource<Z> {
        @Path("shorts")
        ShortsSubResource<Z> subResource();
    }

    public interface ShortsSubResource<Y> {
        @Path("hello")
        HelloSubResource<Y> subResource();
    }

    public interface HelloSubResource<V> {
        @GET
        V get();
    }

    public interface Z<S> {
        @GET
        @Path("something")
        S get();
    }

    public interface Y<L> extends Z<L> {

    }

    @Path("")
    public interface SameTypeArgumentInherit extends Y<Long>, Z<Long> {

    }

    @Provider
    public static class EachCellAsListElementClientMessageBodyReader implements ClientMessageBodyReader<List<String>> {

        @Override
        public List<String> readFrom(Class<List<String>> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap<String, String> httpHeaders, InputStream entityStream, RestClientRequestContext context)
                throws IOException, WebApplicationException {
            return readFrom(type, genericType, annotations, mediaType, httpHeaders, entityStream);
        }

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return genericType.getTypeName().equals("java.util.List<java.lang.String>");
        }

        @Override
        public List<String> readFrom(Class<List<String>> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
                throws IOException, WebApplicationException {
            String body = new String(entityStream.readAllBytes(), StandardCharsets.UTF_8);
            return Arrays.asList(body.split(","));
        }
    }
}
