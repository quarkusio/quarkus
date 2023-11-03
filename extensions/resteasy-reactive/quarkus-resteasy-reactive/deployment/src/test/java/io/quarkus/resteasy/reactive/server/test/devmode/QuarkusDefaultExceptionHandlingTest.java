package io.quarkus.resteasy.reactive.server.test.devmode;

import static org.hamcrest.CoreMatchers.containsString;

import java.util.function.Supplier;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class QuarkusDefaultExceptionHandlingTest {

    @RegisterExtension
    static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Resource.class);
                }

            });

    @Test
    public void testDefaultErrorHandler() {
        RestAssured.given().accept("text/html")
                .get("/test/exception")
                .then()
                .statusCode(500)
                .body(containsString("Internal Server Error"), containsString("dummy exception"));
    }

    @Test
    public void testNotFoundErrorHandler() {
        RestAssured.given().accept("text/html")
                .get("/test/exception2")
                .then()
                .statusCode(404)
                .body(containsString("404 - Resource Not Found"));
    }

    @Path("test")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public static class Resource {

        @Path("exception")
        @GET
        @Produces("text/html")
        public String exception() {
            throw new RuntimeException("dummy exception");
        }
    }
}
