package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import java.util.Objects;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.http.ContentType;

public class CustomObjectMapperTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withEmptyApplication();

    /**
     * Because we have configured the server Object Mapper instance with:
     * `objectMapper.enable(SerializationFeature.WRAP_ROOT_VALUE);`
     */
    @Test
    void serverShouldUnwrapRootElement() {
        given().body("{\"Request\":{\"value\":\"good\"}}")
                .contentType(ContentType.JSON)
                .post("/server")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo("good"));
    }

    @Path("/server")
    public static class MyResource {
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        public String post(Request request) {
            return request.value;
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

    @Provider
    @Unremovable
    public static class CustomObjectMapperContextResolver implements ContextResolver<ObjectMapper> {

        @Override
        public ObjectMapper getContext(final Class<?> type) {
            final ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);
            return objectMapper;
        }
    }
}
