package io.quarkus.rest.client.reactive.jackson.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;

import org.apache.http.HttpStatus;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.quarkus.jackson.ObjectMapperCustomizer;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.rest.client.reactive.jackson.ClientObjectMapper;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class DifferentObjectMapperForClientAndServerTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withEmptyApplication();

    @TestHTTPResource
    URI uri;

    MyClientUnwrappingRootElement clientUnwrappingRootElement;
    MyClientNotUnwrappingRootElement clientNotUnwrappingRootElement;

    @BeforeEach
    public void setup() {
        clientUnwrappingRootElement = QuarkusRestClientBuilder.newBuilder().baseUri(uri)
                .build(MyClientUnwrappingRootElement.class);

        clientNotUnwrappingRootElement = QuarkusRestClientBuilder.newBuilder().baseUri(uri)
                .build(MyClientNotUnwrappingRootElement.class);
    }

    /**
     * Because we have configured the server Object Mapper instance with:
     * `objectMapper.enable(SerializationFeature.WRAP_ROOT_VALUE);`
     */
    @Test
    void serverShouldWrapRootElement() {
        given().get("/server").then()
                .statusCode(HttpStatus.SC_OK)
                .body("Request.value", equalTo("good"));
    }

    /**
     * Because MyClientUnwrappingRootElement is using `@RegisterProvider(ClientObjectMapperUnwrappingRootElement.class)` which
     * is configured with: `.enable(DeserializationFeature.UNWRAP_ROOT_VALUE)`.
     */
    @Test
    void shouldClientUseCustomObjectMapperUnwrappingRootElement() {
        assertFalse(ClientObjectMapperUnwrappingRootElement.USED.get());
        Request request = clientUnwrappingRootElement.get();
        assertEquals("good", request.value);
        assertTrue(ClientObjectMapperUnwrappingRootElement.USED.get());
    }

    /**
     * Because MyClientNotUnwrappingRootElement uses `@ClientObjectMapper`
     * which is configured with: `.disable(DeserializationFeature.UNWRAP_ROOT_VALUE)`.
     */
    @Test
    void shouldClientUseCustomObjectMapperNotUnwrappingRootElement() {
        assertFalse(MyClientNotUnwrappingRootElement.CUSTOM_OBJECT_MAPPER_USED.get());
        Request request = clientNotUnwrappingRootElement.get();
        assertNull(request.value);
        assertTrue(MyClientNotUnwrappingRootElement.CUSTOM_OBJECT_MAPPER_USED.get());
    }

    @Path("/server")
    public static class MyResource {
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Request get() {
            return new Request("good");
        }
    }

    @Path("/server")
    @Produces(MediaType.APPLICATION_JSON)
    @RegisterProvider(ClientObjectMapperUnwrappingRootElement.class)
    public interface MyClientUnwrappingRootElement {
        @GET
        Request get();
    }

    @Path("/server")
    @Produces(MediaType.APPLICATION_JSON)
    public interface MyClientNotUnwrappingRootElement {
        AtomicBoolean CUSTOM_OBJECT_MAPPER_USED = new AtomicBoolean(false);

        @GET
        Request get();

        @ClientObjectMapper
        static ObjectMapper objectMapper(ObjectMapper defaultObjectMapper) {
            CUSTOM_OBJECT_MAPPER_USED.set(true);
            return defaultObjectMapper.copy()
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .disable(DeserializationFeature.UNWRAP_ROOT_VALUE);
        }
    }

    public static class Request {
        private String value;

        public Request() {

        }

        public Request(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Request request = (Request) o;
            return Objects.equals(value, request.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    public static class ClientObjectMapperUnwrappingRootElement implements ContextResolver<ObjectMapper> {
        static final AtomicBoolean USED = new AtomicBoolean(false);

        @Override
        public ObjectMapper getContext(Class<?> type) {
            USED.set(true);
            return new ObjectMapper().enable(DeserializationFeature.UNWRAP_ROOT_VALUE);
        }
    }

    @Singleton
    public static class ServerCustomObjectMapperDisallowUnknownProperties implements ObjectMapperCustomizer {

        @Override
        public void customize(ObjectMapper objectMapper) {
            objectMapper.enable(SerializationFeature.WRAP_ROOT_VALUE);
        }
    }
}
