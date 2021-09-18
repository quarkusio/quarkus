package io.quarkus.resteasy.reactive.server.test.responsestatus;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.jboss.resteasy.reactive.Status;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class ResponseStatusTest {

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Test
    public void should_return_changed_status_uni() {
        int expectedStatus = 201;
        RestAssured
                .given()
                .get("/test/uni")
                .then()
                .statusCode(expectedStatus);
    }

    @Test
    public void should_return_changed_status_multi() {
        int expectedStatus = 201;
        RestAssured
                .given()
                .get("/test/multi")
                .then()
                .statusCode(expectedStatus);
    }

    @Test
    public void should_return_changed_status_completion() {
        int expectedStatus = 201;
        RestAssured
                .given()
                .get("/test/completion")
                .then()
                .statusCode(expectedStatus);
    }

    @Test
    public void should_return_changed_status_plain() {
        int expectedStatus = 201;
        RestAssured
                .given()
                .get("/test/plain")
                .then()
                .statusCode(expectedStatus);
    }

    @Test
    public void should_not_change_status_uni() {
        int expectedStatus = 500;
        RestAssured
                .given()
                .get("/test/exception_uni")
                .then()
                .statusCode(expectedStatus);
    }

    @Test
    public void should_not_change_status_multi() {
        int expectedStatus = 500;
        RestAssured
                .given()
                .get("/test/exception_multi")
                .then()
                .statusCode(expectedStatus);
    }

    @Test
    public void should_not_change_status_completion() {
        int expectedStatus = 500;
        RestAssured
                .given()
                .get("/test/exception_completion")
                .then()
                .statusCode(expectedStatus);
    }

    @Test
    public void should_not_change_status_plain() {
        int expectedStatus = 500;
        RestAssured
                .given()
                .get("/test/exception_plain")
                .then()
                .statusCode(expectedStatus);
    }

    @Path("/test")
    public static class TestResource {

        @Status(201)
        @GET
        @Path("/uni")
        public Uni<String> getTestUni() {
            return Uni.createFrom().item("test");
        }

        @Status(201)
        @GET
        @Path("/multi")
        public Multi<String> getTestMulti() {
            return Multi.createFrom().item("test");
        }

        @Status(201)
        @GET
        @Path("/completion")
        public CompletionStage<String> getTestCompletion() {
            return CompletableFuture.supplyAsync(() -> "test");
        }

        @Status(201)
        @GET
        @Path("/plain")
        public String getTestPlain() {
            return "test";
        }

        @Status(201)
        @GET
        @Path(("/exception_uni"))
        public Uni<String> throwExceptionUni() {
            return Uni.createFrom().failure(new IllegalArgumentException());
        }

        @Status(201)
        @GET
        @Path("/exception_multi")
        public Multi<String> throwExceptionMulti() {
            return Multi.createFrom().failure(new IllegalArgumentException());
        }

        @Status(201)
        @GET
        @Path("/exception_completion")
        public CompletionStage<String> throwExceptionCompletion() {
            return CompletableFuture.failedFuture(new IllegalArgumentException());
        }

        @Status(201)
        @GET
        @Path("/exception_plain")
        public String throwExceptionPlain() {
            throw new IllegalArgumentException();
        }

    }
}
