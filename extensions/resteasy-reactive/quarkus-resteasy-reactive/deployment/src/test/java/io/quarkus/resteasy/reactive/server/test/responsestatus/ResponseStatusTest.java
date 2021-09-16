package io.quarkus.resteasy.reactive.server.test.responsestatus;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.common.Status;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;

public class ResponseStatusTest {

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Test
    public void should_return_changed_status() {
        int expectedStatus = 201;
        RestAssured
                .given()
                .get("/test")
                .then()
                .statusCode(expectedStatus);
    }

    @Path("/test")
    public static class TestResource {

        @Status(201)
        @GET
        public Uni<String> getTest() {
            return Uni.createFrom().item("test");
        }
    }
}
