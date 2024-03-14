package io.quarkus.resteasy.reactive.server.test.response;

import static io.restassured.RestAssured.when;

import java.io.IOException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

//https://github.com/quarkusio/quarkus/issues/17168
class NullHeaderTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(NullFilter.class, NullResource.class));

    @Test
    void nullHeaderTest() {
        when()
                .get("/null")
                .then().statusCode(200)
                .header("nullHeader", "");
    }

    @Provider
    public static class NullFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
                throws IOException {
            responseContext.getHeaders().add("nullHeader", null);
        }
    }

    @Path("/null")
    public static class NullResource {
        @GET
        public Response ok() {
            return Response.ok().build();
        }
    }
}
