package io.quarkus.resteasy.reactive.server.test.mediatype;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class MalformedAcceptHeaderTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(HelloResource.class, NotFoundExceptionMapper.class);
                }
            });

    @Test
    public void invalidAcceptWhenNoRouteMatched() {
        // the ExceptionMapper response entity is written without a resource target having been resolved,
        // so writer negotiation parses the Accept header and must not fail on the unparseable value
        given().accept("/")
                .get("/does-not-exist")
                .then()
                .statusCode(404)
                .body(is("not found"));
    }

    @Test
    public void invalidAcceptOnMatchedRouteWithoutProduces() {
        // an unparseable Accept value behaves as if no Accept header was sent
        given().accept("/")
                .get("/hello/dynamic")
                .then()
                .statusCode(200)
                .body(is("hello"));
    }

    @Test
    public void partiallyInvalidAcceptOnMatchedRouteWithoutProduces() {
        // the valid part of the Accept header is still used for negotiation
        given().accept("text/plain, /")
                .get("/hello/dynamic")
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .body(is("hello"));
    }

    @Test
    public void invalidAcceptOnMatchedRouteWithProduces() {
        // a fully unparseable Accept header is a client syntax error
        given().accept("/")
                .get("/hello/produces")
                .then()
                .statusCode(400);
    }

    @Test
    public void validButUnmatchedAcceptOnMatchedRouteWithProduces() {
        // a parseable Accept that does not overlap @Produces remains a negotiation failure
        given().accept("application/xml")
                .get("/hello/produces")
                .then()
                .statusCode(406);
    }

    @Test
    public void partiallyInvalidAcceptOnMatchedRouteWithProduces() {
        // valid tokens are still used for negotiation; unparseable ones are skipped
        given().accept("text/plain, /")
                .get("/hello/produces")
                .then()
                .statusCode(200)
                .body(is("hello"));
    }

    @Provider
    public static class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

        @Override
        public Response toResponse(NotFoundException exception) {
            return Response.status(404).entity("not found").build();
        }
    }

    @Path("hello")
    public static class HelloResource {

        @GET
        @Path("dynamic")
        public Response dynamic() {
            // returning Response without an explicit type forces writer selection to go through
            // DynamicEntityWriter which negotiates against the Accept header
            return Response.ok("hello").build();
        }

        @GET
        @Path("produces")
        @Produces("text/plain")
        public String produces() {
            return "hello";
        }
    }
}
