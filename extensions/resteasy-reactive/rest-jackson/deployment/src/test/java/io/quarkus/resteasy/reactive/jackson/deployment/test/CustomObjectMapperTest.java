package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
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
import com.fasterxml.jackson.databind.SerializationFeature;

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
    void test() {
        given().body("{\"Request\":{\"value\":\"FIRST\"}}")
                .contentType(ContentType.JSON)
                .post("/server/dummy")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo("0"));

        // ContextResolver was invoked for both reader and writer
        when().get("/server/count")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo("2"));

        given().body("{\"Request2\":{\"value\":\"FIRST\"}}")
                .contentType(ContentType.JSON)
                .post("/server/dummy2")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo("0"));

        // ContextResolver was invoked for both reader and writer because different types where used
        when().get("/server/count")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo("4"));

        given().body("{\"Request\":{\"value\":\"FIRST\"}}")
                .contentType(ContentType.JSON)
                .post("/server/dummy")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo("0"));

        // ContextResolver was not invoked because the types have already been cached
        when().get("/server/count")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo("4"));

        given().body("{\"Request2\":{\"value\":\"FIRST\"}}")
                .contentType(ContentType.JSON)
                .post("/server/dummy2")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo("0"));

        // ContextResolver was not invoked because the types have already been cached
        when().get("/server/count")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(equalTo("4"));
    }

    private static void doTest() {

    }

    @Path("/server")
    public static class MyResource {
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Path("dummy")
        public Dummy dummy(Request request) {
            return Dummy.valueOf(request.value);
        }

        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Path("dummy2")
        public Dummy2 dummy2(Request2 request) {
            return Dummy2.valueOf(request.value);
        }

        @GET
        @Path("count")
        public long count() {
            return CustomObjectMapperContextResolver.COUNT.get();
        }
    }

    public enum Dummy {
        FIRST,
        SECOND
    }

    public enum Dummy2 {
        FIRST,
        SECOND
    }

    public static class Request {
        protected String value;

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

    public static class Request2 extends Request {
    }

    @Provider
    @Unremovable
    public static class CustomObjectMapperContextResolver implements ContextResolver<ObjectMapper> {

        static final AtomicLong COUNT = new AtomicLong();

        @Override
        public ObjectMapper getContext(final Class<?> type) {
            COUNT.incrementAndGet();
            final ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE)
                    .enable(SerializationFeature.WRITE_ENUMS_USING_INDEX);
            return objectMapper;
        }
    }
}
