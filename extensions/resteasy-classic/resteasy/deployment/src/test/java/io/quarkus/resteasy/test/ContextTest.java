package io.quarkus.resteasy.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class ContextTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyResource.class));

    @Test
    public void testContextInjection() {
        Assertions.assertEquals("ok", RestAssured.get("/ctxt").asString());
    }

    @Path("/ctxt")
    public static class MyResource {
        @Context
        HttpServerRequest request;
        @Context
        HttpServerResponse response;
        @Context
        Vertx vertx;
        @Context
        io.vertx.core.Context vertxContext;
        @Context
        SecurityContext securityContext;
        @Context
        RoutingContext rc;

        @GET
        public String test() {
            if (request == null || request.path() == null) {
                throw new IllegalStateException("Request not injected");
            }
            if (response == null) {
                throw new IllegalStateException("Response not injected");
            }
            if (rc == null) {
                throw new IllegalStateException("Routing Context not injected");
            }
            if (vertx == null) {
                throw new IllegalStateException("Vert.x not injected");
            }
            if (vertxContext == null) {
                throw new IllegalStateException("Vert.x context not injected");
            }
            if (securityContext == null) {
                throw new IllegalStateException("Security context not injected");
            }
            return "ok";
        }
    }
}
