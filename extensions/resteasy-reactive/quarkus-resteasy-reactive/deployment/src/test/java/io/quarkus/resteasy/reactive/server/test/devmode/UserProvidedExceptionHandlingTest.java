package io.quarkus.resteasy.reactive.server.test.devmode;

import java.util.function.Supplier;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class UserProvidedExceptionHandlingTest {

    @RegisterExtension
    static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Resource.class, CustomExceptionMapper.class);
                }

            });

    @Test
    public void testDefaultErrorHandler() {
        RestAssured.given().accept("text/html")
                .get("/test/exception")
                .then()
                .statusCode(999);
    }

    @Test
    public void testNotFoundErrorHandler() {
        RestAssured.given().accept("text/html")
                .get("/test/exception2")
                .then()
                .statusCode(999);
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

    @Provider
    public static class CustomExceptionMapper implements ExceptionMapper<Exception> {
        @Override
        public Response toResponse(Exception exception) {
            return Response.status(999).build();
        }
    }

}
