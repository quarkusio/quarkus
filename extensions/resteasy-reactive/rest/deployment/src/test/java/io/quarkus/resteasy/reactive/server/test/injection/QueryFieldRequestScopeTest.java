package io.quarkus.resteasy.reactive.server.test.injection;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class QueryFieldRequestScopeTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(Resource.class));

    @Test
    public void test() {
        when()
                .get("/test?foo=f&bar=b")
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
    @RequestScoped
    public static class Resource {

        @QueryParam("foo")
        String foo;

        @RestQuery
        String bar;

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String hello() {
            return "foo: " + foo + ", bar: " + bar;
        }
    }
}
