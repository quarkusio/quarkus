package io.quarkus.resteasy.reactive.server.test.headers;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.ResponseHeader;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestMulti;
import org.jboss.resteasy.reactive.RestQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
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

    @Test
    public void testReturnRestMulti() {
        Map<String, String> expectedHeaders = Map.of(
                "Access-Control-Allow-Origin", "foo",
                "Keep-Alive", "bar");
        RestAssured
                .given()
                .get("/test/rest-multi")
                .then()
                .statusCode(200)
                .headers(expectedHeaders);
    }

    @Test
    public void testReturnRestMulti2() {
        RestAssured
                .given()
                .get("/test/rest-multi2")
                .then()
                .statusCode(200)
                .headers(Map.of(
                        "Access-Control-Allow-Origin", "foo",
                        "Keep-Alive", "bar"));

        RestAssured
                .given()
                .get("/test/rest-multi2?keepAlive=dummy")
                .then()
                .statusCode(200)
                .headers(Map.of(
                        "Access-Control-Allow-Origin", "foo",
                        "Keep-Alive", "dummy"));
    }

    @Test
    public void testReturnRestMulti3() {
        RestAssured
                .given()
                .get("/test/rest-multi3")
                .then()
                .statusCode(200)
                .headers(Map.of(
                        "header1", "foo",
                        "header2", "bar"));

        RestAssured
                .given()
                .get("/test/rest-multi3?h1=h1&h2=h2")
                .then()
                .statusCode(200)
                .headers(Map.of(
                        "header1", "h1",
                        "header2", "h2"));
    }

    @Test
    public void testReturnRestMulti4() {
        RestAssured
                .given()
                .get("/test/rest-multi2")
                .then()
                .statusCode(200)
                .contentType(ContentType.TEXT)
                .headers(Map.of(
                        "Access-Control-Allow-Origin", "foo",
                        "Keep-Alive", "bar"));

        RestAssured
                .given()
                .get("/test/rest-multi2?keepAlive=dummy")
                .then()
                .statusCode(200)
                .contentType(ContentType.TEXT)
                .headers(Map.of(
                        "Access-Control-Allow-Origin", "foo",
                        "Keep-Alive", "dummy"));
    }

    @Test
    public void testReturnRestMulti5() {
        RestAssured
                .given()
                .get("/test/rest-multi3")
                .then()
                .statusCode(200)
                .headers(Map.of(
                        "header1", "foo",
                        "header2", "bar"));

        RestAssured
                .given()
                .get("/test/rest-multi3?h1=h1&h2=h2")
                .then()
                .statusCode(200)
                .headers(Map.of(
                        "header1", "h1",
                        "header2", "h2"));
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

        @ResponseHeader(name = "Access-Control-Allow-Origin", value = "*")
        @ResponseHeader(name = "Keep-Alive", value = "timeout=5, max=997")
        @GET
        @Path("/rest-multi")
        public RestMulti<String> getTestRestMulti() {
            return RestMulti.fromMultiData(Multi.createFrom().item("test")).header("Access-Control-Allow-Origin", "foo")
                    .header("Keep-Alive", "bar").build();
        }

        @GET
        @Path("/rest-multi2")
        public RestMulti<String> getTestRestMulti2(@DefaultValue("bar") @RestQuery String keepAlive) {
            return RestMulti.fromMultiData(Multi.createFrom().item("test")).header("Access-Control-Allow-Origin", "foo")
                    .header("Keep-Alive", keepAlive).build();
        }

        @GET
        @Path("/rest-multi3")
        @Produces("application/octet-stream")
        public RestMulti<byte[]> getTestRestMulti3(@DefaultValue("foo") @RestQuery("h1") String header1,
                @DefaultValue("bar") @RestQuery("h2") String header2) {
            return RestMulti.fromUniResponse(getWrapper(header1, header2), Wrapper::getData, Wrapper::getHeaders);
        }

        @GET
        @Path("/rest-multi4")
        public RestMulti<byte[]> getTestRestMulti4(@DefaultValue("bar") @RestQuery String keepAlive) {
            return RestMulti.fromMultiData(Multi.createFrom().item("test".getBytes(StandardCharsets.UTF_8)))
                    .header("Access-Control-Allow-Origin", "foo")
                    .header("Keep-Alive", keepAlive).header("Content-Type", MediaType.TEXT_PLAIN).build();
        }

        @GET
        @Path("/rest-multi5")
        public RestMulti<byte[]> getTestRestMulti5(@DefaultValue("foo") @RestQuery("h1") String header1,
                @DefaultValue("bar") @RestQuery("h2") String header2) {
            return RestMulti.fromUniResponse(getWrapper(header1, header2), Wrapper::getData, Wrapper::getHeaders);
        }

        private IllegalArgumentException createException() {
            IllegalArgumentException result = new IllegalArgumentException();
            result.setStackTrace(EMPTY_STACK_TRACE);
            return result;
        }

        private Uni<Wrapper> getWrapper(String header1, String header2) {
            return Uni.createFrom().item(
                    () -> new Wrapper(Multi.createFrom().item("test".getBytes(StandardCharsets.UTF_8)), header1, header2));
        }

        private static final class Wrapper {
            public final Multi<byte[]> data;

            public final Map<String, List<String>> headers;

            public Wrapper(Multi<byte[]> data, String header1, String header2) {
                this.data = data;
                this.headers = Map.of("header1", List.of(header1), "header2", List.of(header2));
            }

            public Multi<byte[]> getData() {
                return data;
            }

            public Map<String, List<String>> getHeaders() {
                return headers;
            }
        }
    }
}
