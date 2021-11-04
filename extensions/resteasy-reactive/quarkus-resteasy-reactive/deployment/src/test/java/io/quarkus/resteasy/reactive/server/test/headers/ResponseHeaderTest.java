package io.quarkus.resteasy.reactive.server.test.headers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.jboss.resteasy.reactive.ResponseHeader;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.Headers;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class ResponseHeaderTest {

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(TestResource.class));

    @Test
    public void testReturnUni() {
        Map<String, String> expectedHeaders = Map.of(
                "Access-Control-Allow-Origin", "*",
                "Keep-Alive", "timeout=5, max=997");
        RestAssured
                .given()
                .get("/test/uni")
                .then()
                .statusCode(200)
                .headers(expectedHeaders);
    }

    @Test
    public void testReturnUniAndContainsResponseStatus() {
        RestAssured
                .given()
                .get("/test/uni2")
                .then()
                .statusCode(201)
                .headers(Collections.singletonMap("foo", "bar"));
    }

    @Test
    public void testReturnMulti() {
        Map<String, String> expectedHeaders = Map.of(
                "Access-Control-Allow-Origin", "*",
                "Keep-Alive", "timeout=5, max=997");
        RestAssured
                .given()
                .get("/test/multi")
                .then()
                .statusCode(200)
                .headers(expectedHeaders);
    }

    @Test
    public void testReturnCompletionStage() {
        Map<String, String> expectedHeaders = Map.of(
                "Access-Control-Allow-Origin", "*",
                "Keep-Alive", "timeout=5, max=997");
        RestAssured
                .given()
                .get("/test/completion")
                .then()
                .statusCode(200)
                .headers(expectedHeaders);
    }

    @Test
    public void testReturnString() {
        Map<String, String> expectedHeaders = Map.of(
                "Access-Control-Allow-Origin", "*",
                "Keep-Alive", "timeout=5, max=997");
        RestAssured
                .given()
                .get("/test/plain")
                .then()
                .statusCode(200)
                .headers(expectedHeaders);
    }

    @Test
    public void testUniThrowsException() {
        Headers headers = RestAssured.given().get("/test/exception_uni")
                .then().extract().headers();
        assertFalse(headers.hasHeaderWithName("Access-Control-Allow-Origin"));

    }

    @Test
    public void testMultiThrowsException() {
        Headers headers = RestAssured.given().get("/test/exception_multi")
                .then().extract().headers();
        assertFalse(headers.hasHeaderWithName("Access-Control-Allow-Origin"));
    }

    @Test
    public void testCompletionStageThrowsException() {
        Headers headers = RestAssured.given().get("/test/exception_completion")
                .then().extract().headers();
        assertFalse(headers.hasHeaderWithName("Access-Control-Allow-Origin"));
    }

    @Test
    public void testStringThrowsException() {
        Headers headers = RestAssured.given().get("/test/exception_plain")
                .then().extract().headers();
        assertFalse(headers.hasHeaderWithName("Access-Control-Allow-Origin"));
    }

    @Path("/test")
    public static class TestResource {

        private static final StackTraceElement[] EMPTY_STACK_TRACE = new StackTraceElement[0];

        @ResponseHeader(name = "Access-Control-Allow-Origin", value = "*")
        @ResponseHeader(name = "Keep-Alive", value = "timeout=5, max=997")
        @GET
        @Path(("/uni"))
        public Uni<String> getTestUni() {
            return Uni.createFrom().item("test");
        }

        @ResponseHeader(name = "foo", value = "bar")
        @ResponseStatus(201)
        @GET
        @Path(("/uni2"))
        public Uni<String> getTestUni2() {
            return Uni.createFrom().item("test");
        }

        @ResponseHeader(name = "Access-Control-Allow-Origin", value = "*")
        @ResponseHeader(name = "Keep-Alive", value = "timeout=5, max=997")
        @GET
        @Path("/multi")
        public Multi<String> getTestMulti() {
            return Multi.createFrom().item("test");
        }

        @ResponseHeader(name = "Access-Control-Allow-Origin", value = "*")
        @ResponseHeader(name = "Keep-Alive", value = "timeout=5, max=997")
        @GET
        @Path("/completion")
        public CompletionStage<String> getTestCompletion() {
            return CompletableFuture.supplyAsync(() -> "test");
        }

        @ResponseHeader(name = "Access-Control-Allow-Origin", value = "*")
        @ResponseHeader(name = "Keep-Alive", value = "timeout=5, max=997")
        @GET
        @Path("/plain")
        public String getTestPlain() {
            return "test";
        }

        @ResponseHeader(name = "Access-Control-Allow-Origin", value = "*")
        @GET
        @Path(("/exception_uni"))
        public Uni<String> throwExceptionUni() {
            return Uni.createFrom().failure(createException());
        }

        @ResponseHeader(name = "Access-Control-Allow-Origin", value = "*")
        @GET
        @Path("/exception_multi")
        public Multi<String> throwExceptionMulti() {
            return Multi.createFrom().failure(createException());
        }

        @ResponseHeader(name = "Access-Control-Allow-Origin", value = "*")
        @Path("/exception_completion")
        public CompletionStage<String> throwExceptionCompletion() {
            return CompletableFuture.failedFuture(createException());
        }

        @ResponseHeader(name = "Access-Control-Allow-Origin", value = "*")
        @GET
        @Path("/exception_plain")
        public String throwExceptionPlain() {
            throw createException();
        }

        private IllegalArgumentException createException() {
            IllegalArgumentException result = new IllegalArgumentException();
            result.setStackTrace(EMPTY_STACK_TRACE);
            return result;
        }
    }
}
