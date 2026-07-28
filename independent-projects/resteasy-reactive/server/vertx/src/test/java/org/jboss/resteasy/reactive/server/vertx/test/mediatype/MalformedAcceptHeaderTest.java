package org.jboss.resteasy.reactive.server.vertx.test.mediatype;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class MalformedAcceptHeaderTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(HelloResource.class, NotFoundMapper.class);
                }
            });

    @Test
    public void testMalformedAcceptHeaderDoesNotCrash() {
        given()
                .accept("/") // A malformed Accept header that traditionally triggered IllegalArgumentException
                .get("/hello")
                .then()
                .statusCode(200) // Should fallback gracefully and succeed
                .body(equalTo("hello"));
    }

    @Test
    public void testMalformedAcceptHeaderWithNotFoundExceptionMapper() {
        given()
                .accept("/")
                .get("/does-not-exist")
                .then()
                .statusCode(404)
                .body("error", equalTo("not found"));
    }

    @Test
    public void testOtherMalformedAcceptHeaders() {
        // No slash at all
        given()
                .accept("blah")
                .get("/hello")
                .then()
                .statusCode(200)
                .body(equalTo("hello"));
    }

    @Path("hello")
    public static class HelloResource {

        @Produces("text/plain")
        @GET
        public String hello() {
            return "hello";
        }
    }

    @jakarta.ws.rs.ext.Provider
    public static class NotFoundMapper implements jakarta.ws.rs.ext.ExceptionMapper<jakarta.ws.rs.NotFoundException> {
        @Override
        public jakarta.ws.rs.core.Response toResponse(jakarta.ws.rs.NotFoundException exception) {
            return jakarta.ws.rs.core.Response.status(404)
                    .type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
                    .entity("{\"error\":\"not found\"}")
                    .build();
        }
    }
}
