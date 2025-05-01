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
import java.util.Map;

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

public class SubResourceGenericsTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(Resource.class)
                    .addClass(ClientLocator.class)
                    .addClass(TranslationSubResource.class)
                    .addClass(EnglishSubResource.class)
                    .addClass(ShortsSubResource.class)
                    .addClass(HelloSubResource.class)
                    .addClass(EachCellAsListElementClientMessageBodyReader.class)
                    .addClass(CellsAsMapEntryClientMessageBodyReader.class));

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

        {
            List<String> result = client.list().subResource().subResource().subResource().get();
            assertThat(result).contains("Hello", "World");
        }

        {
            Map<String, String> result = client.map().subResource().subResource().subResource().get();
            assertThat(result).containsEntry("Hello", "World");
        }
    }

    @Test
    void testFailureSubResourceLocatorMethodWithUnresolvedTypeVariable() {

        try {
            QuarkusRestClientBuilder.newBuilder().baseUri(URI.create("http://localhost:8081"))
                    .build(TranslationSubResource.class);
        } catch (Exception e) {
            assertThat(e.getMessage()).endsWith(
                    "Failed to generate client for class interface io.quarkus.rest.client.reactive.subresource.SubResourceGenericsTest$TranslationSubResource : Type variable R of method io.quarkus.rest.client.reactive.subresource.SubResourceGenericsTest$EnglishSubResource<R> subResource() in class io.quarkus.rest.client.reactive.subresource.SubResourceGenericsTest$TranslationSubResource could not be resolved.");
            return;
        }
        Assertions.fail("Should have thrown an exception");
    }

    @Test
    void testFailureRestClientMethodWithUnresolvedTypeVariable() {

        try {
            QuarkusRestClientBuilder.newBuilder().baseUri(URI.create("http://localhost:8081")).build(HelloSubResource.class);
        } catch (Exception e) {
            assertThat(e.getMessage()).endsWith(
                    "Failed to generate client for class interface io.quarkus.rest.client.reactive.subresource.SubResourceGenericsTest$HelloSubResource : Type variable V of method V get() in class io.quarkus.rest.client.reactive.subresource.SubResourceGenericsTest$HelloSubResource could not be resolved.");
            return;
        }
        Assertions.fail("Should have thrown an exception");
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

    @RegisterRestClient(baseUri = "http://localhost:8081")
    @Path("")
    public interface ClientLocator {

        @Path("greetings/translations")
        TranslationSubResource<String> string();

        @Path("greetings-count/translations")
        TranslationSubResource<Long> number();

        @Path("greetings/translations")
        TranslationSubResource<List<String>> list();

        @Path("greetings/translations")
        TranslationSubResource<Map<String, String>> map();

        @Path("greetings/translations/english/shorts/hello")
        @GET
        List<String> testListOnRoot();
    }

    public interface TranslationSubResource<R> {
        @Path("english")
        EnglishSubResource<R> subResource();
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

    @Provider
    public static class CellsAsMapEntryClientMessageBodyReader implements ClientMessageBodyReader<Map<String, String>> {

        @Override
        public Map<String, String> readFrom(Class<Map<String, String>> type, Type genericType, Annotation[] annotations,
                MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream,
                RestClientRequestContext context) throws IOException, WebApplicationException {
            return readFrom(type, genericType, annotations, mediaType, httpHeaders, entityStream);
        }

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return genericType.getTypeName().equals("java.util.Map<java.lang.String, java.lang.String>");
        }

        @Override
        public Map<String, String> readFrom(Class<Map<String, String>> type, Type genericType, Annotation[] annotations,
                MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
                throws IOException, WebApplicationException {
            String body = new String(entityStream.readAllBytes(), StandardCharsets.UTF_8);
            String[] split = body.split(",");
            return Map.of(split[0], split[1]);
        }
    }
}
