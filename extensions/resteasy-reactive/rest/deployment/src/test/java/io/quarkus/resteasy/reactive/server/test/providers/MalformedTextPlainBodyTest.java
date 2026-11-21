package io.quarkus.resteasy.reactive.server.test.providers;

import static io.restassured.RestAssured.given;

import java.util.function.Supplier;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class MalformedTextPlainBodyTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(NumberResource.class);
                }

            });

    @Test
    public void testInvalidNumber() {
        given()
                .contentType("text/plain")
                .body("abc")
                .when()
                .post("/number/wrapper")
                .then()
                .statusCode(400);
    }

    @Test
    public void testInvalidPrimitive() {
        given()
                .contentType("text/plain")
                .body("abc")
                .when()
                .post("/number/primitive")
                .then()
                .statusCode(400);
    }

    @Path("/number")
    public static class NumberResource {

        @POST
        @Path("/wrapper")
        @Consumes(MediaType.TEXT_PLAIN)
        @Produces(MediaType.TEXT_PLAIN)
        public String wrapper(Integer value) {
            return "ok";
        }

        @POST
        @Path("/primitive")
        @Consumes(MediaType.TEXT_PLAIN)
        @Produces(MediaType.TEXT_PLAIN)
        public String primitive(int value) {
            return "ok";
        }
    }
}
