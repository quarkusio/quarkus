package org.jboss.resteasy.reactive.server.vertx.test.matching;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class InvalidHeaderTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(TestResource.class);
                }
            });

    @Test
    void test() {
        given()
                .header("Accept", "application/json")
                .when().get("/test/1")
                .then()
                .statusCode(200)
                .body(is("{\"id\": \"1\"}"));

        given()
                .header("Accept", "text/plain")
                .when().get("/test/1")
                .then()
                .statusCode(200)
                .body(is("1"));

        given()
                .header("Accept", "foobar")
                .when().get("/test/1")
                .then()
                .statusCode(400);
    }

    @Path("/test")
    public static class TestResource {

        @GET
        @Produces("*/*")
        public String hello() {
            return "Hello from Quarkus REST";
        }

        @GET
        @Path("/{id}")
        @Produces(MediaType.TEXT_PLAIN)
        public String file(@RestPath String id) {
            return id;
        }

        @GET
        @Path("/{id}")
        @Produces(MediaType.APPLICATION_JSON)
        public String file2(@RestPath String id) {
            return "{\"id\": \"" + id + "\"}";
        }
    }
}
