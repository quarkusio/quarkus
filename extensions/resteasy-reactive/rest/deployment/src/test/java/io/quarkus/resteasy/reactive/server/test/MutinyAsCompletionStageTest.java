package io.quarkus.resteasy.reactive.server.test;

import static org.hamcrest.Matchers.equalTo;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;

public class MutinyAsCompletionStageTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClass(TestResource.class);
                }
            });

    @Test
    public void testOk() {
        RestAssured.get("/test/ok")
                .then()
                .statusCode(200)
                .body(equalTo("test"));
    }

    @Test
    public void testError() {
        RestAssured.get("/test/error")
                .then()
                .statusCode(400);
    }

    @Path("test")
    public static class TestResource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        @Path("/ok")
        public CompletionStage<String> ok() {
            return Uni.createFrom().item("test").subscribeAsCompletionStage();
        }

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        @Path("/error")
        public CompletionStage<String> error() {
            return Uni.createFrom().<String> failure(new BadRequestException()).subscribeAsCompletionStage();
        }
    }

}
