package io.quarkus.resteasy.reactive.server.test.injection;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestHeader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class HeaderFieldNoScopeTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(Resource.class));

    @Test
    public void test() {
        given()
                .header("foo", "f")
                .header("bar", "b")
                .when()
                .get("/test")
                .then()
                .statusCode(200)
                .body(is("foo: f, bar: b"));

        when()
                .get("/test")
                .then()
                .statusCode(200)
                .body(is("foo: null, bar: null"));
    }

    @Path("/test")
    public static class Resource {

        @HeaderParam("foo")
        String foo;

        @RestHeader
        String bar;

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String hello() {
            return "foo: " + foo + ", bar: " + bar;
        }
    }
}
