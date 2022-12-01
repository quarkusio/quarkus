package io.quarkus.resteasy.reactive.server.test.status;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.jboss.resteasy.reactive.ResponseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class ResponseStatusTest {

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(TestResource.class));

    @Test
    public void testReturnUni() {
        RestAssured
                .given()
                .get("/test/uni")
                .then()
                .statusCode(201);
    }

    @Test
    public void testReturnMulti() {
        RestAssured
                .given()
                .get("/test/multi")
                .then()
                .statusCode(202);
    }

    @Test
    public void testReturnCompletionStage() {
        RestAssured
                .given()
                .get("/test/completion")
                .then()
                .statusCode(203);
    }

    @Test
    public void testReturnString() {
        RestAssured
                .given()
                .get("/test/plain")
                .then()
                .statusCode(204);
    }

    @Test
    public void testUniThrowsException() {
        RestAssured
                .given()
                .get("/test/exception_uni")
                .then()
                .statusCode(500);
    }

    @Test
    public void testMultiThrowsException() {
        RestAssured
                .given()
                .get("/test/exception_multi")
                .then()
                .statusCode(500);
    }

    @Test
    public void testCompletionStageThrowsException() {
        RestAssured
                .given()
                .get("/test/exception_completion")
                .then()
                .statusCode(500);
    }

    @Test
    public void testStringThrowsException() {
        RestAssured
                .given()
                .get("/test/exception_plain")
                .then()
                .statusCode(500);
    }

    @Path("/test")
    public static class TestResource {

        private static final StackTraceElement[] EMPTY_STACK_TRACE = new StackTraceElement[0];

        @ResponseStatus(201)
        @GET
        @Path("/uni")
        public Uni<String> getTestUni() {
            return Uni.createFrom().item("test");
        }

        @ResponseStatus(202)
        @GET
        @Path("/multi")
        public Multi<String> getTestMulti() {
            return Multi.createFrom().item("test");
        }

        @ResponseStatus(203)
        @GET
        @Path("/completion")
        public CompletionStage<String> getTestCompletion() {
            return CompletableFuture.supplyAsync(() -> "test");
        }

        @ResponseStatus(204)
        @GET
        @Path("/plain")
        public String getTestPlain() {
            return "test";
        }

        @ResponseStatus(201)
        @GET
        @Path(("/exception_uni"))
        public Uni<String> throwExceptionUni() {
            return Uni.createFrom().failure(createException());
        }

        @ResponseStatus(201)
        @GET
        @Path("/exception_multi")
        public Multi<String> throwExceptionMulti() {
            return Multi.createFrom().failure(createException());
        }

        @ResponseStatus(201)
        @GET
        @Path("/exception_completion")
        public CompletionStage<String> throwExceptionCompletion() {
            return CompletableFuture.failedFuture(createException());
        }

        @ResponseStatus(201)
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
