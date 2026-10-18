package io.quarkus.rest.client.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class NotBodyParameterTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest();

    @TestHTTPResource
    String baseUrl;

    @RestClient
    Client client;

    @Test
    public void testCorrectBodyParameter() {
        final Client client = RestClientBuilder.newBuilder()
                .baseUri(baseUrl)
                .build(Client.class);

        assertEquals("def", client.test("abc", "def"));
    }

    @Test
    public void testUrlAnnotationNotABodyParameter() {
        assertEquals("abc", client.testUrlAnnotation(baseUrl, "abc"));
    }

    @NotBody
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface MyTestAnnotation {

    }

    @Path("test")
    @RegisterRestClient(baseUri = "http://not-a-thing")
    public interface Client {

        @POST
        String test(@MyTestAnnotation String someCustomParameter, String foo);

        @POST
        String testUrlAnnotation(@Url String uri, String foo);
    }

    @Path("test")
    public static class Resource {

        @POST
        public String test(String foo) {
            return foo;
        }
    }

}
