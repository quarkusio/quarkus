package io.quarkus.resteasy.reactive.server.test.resource.basic;

import java.io.IOException;
import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class AbortFilterWithLargePayloadTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(TestResource.class, AbortingFilter.class);
                    return war;
                }
            });

    @Test
    public void test() {
        RestAssured.get("/test")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("hello"));

        io.restassured.response.Response response = RestAssured.with().header("abort", "true").get("/test")
                .then()
                .statusCode(999).extract().response();
        Assertions.assertEquals(30464, response.body().asByteArray().length);
    }

    @Path("/test")
    public static class TestResource {

        @GET
        public String hello() {
            return "hello";
        }
    }

    @Provider
    @PreMatching
    public static class AbortingFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            if (requestContext.getHeaderString("abort") != null) {
                // the magic number of 30464 is where the overflow code path used to fail
                requestContext.abortWith(
                        Response.status(999).type(MediaType.APPLICATION_OCTET_STREAM).entity(new byte[30464]).build());
            }
        }
    }
}
