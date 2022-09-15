package org.jboss.resteasy.reactive.server.vertx.test.response;

import static io.restassured.RestAssured.when;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

//https://github.com/quarkusio/quarkus/issues/17168
class NullHeaderTestCase {

    @RegisterExtension
    static ResteasyReactiveUnitTest runner = new ResteasyReactiveUnitTest()
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
