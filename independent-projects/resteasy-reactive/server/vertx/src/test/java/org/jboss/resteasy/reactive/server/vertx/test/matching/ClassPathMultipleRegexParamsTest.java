package org.jboss.resteasy.reactive.server.vertx.test.matching;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ClassPathMultipleRegexParamsTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClass(ResourceWithRegexOnClass.class);
                }
            });

    @Test
    public void testRegexWithMultipleParamsOnClassPath() {
        given()
                .when()
                .get("/hello/foo758945c8-fcea-44dd-8608-371346e82cff/second/bar")
                .then()
                .statusCode(200)
                .body(is("first:foo758945c8-fcea-44dd-8608-371346e82cff, third:bar"));
    }

    @Test
    public void testNonMatchingRegex() {
        given()
                .when()
                .get("/hello/INVALID/second/bar")
                .then()
                .statusCode(404);
    }

    @Path("/hello/{first:foo[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}}/second/{third}")
    public static class ResourceWithRegexOnClass {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String greeting(@PathParam("first") String first, @PathParam("third") String third) {
            return "first:%s, third:%s".formatted(first, third);
        }
    }
}
