package io.quarkus.micrometer.deployment.pathparams;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.micrometer.test.Util;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class HttpPathParamLimitWithJaxRs400Test {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.binder.http-client.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder.http-server.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder.vertx.enabled", "true")
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .withApplicationRoot((jar) -> jar
                    .addClasses(Util.class,
                            Resource.class));

    @Inject
    MeterRegistry registry;

    public static final int COUNT = 101;

    @Test
    void testWithResteasy400() throws InterruptedException {
        registry.clear();
        // Test a JAX-RS endpoint with GET /jaxrs and GET /jaxrs/{message}
        // Verify OK response
        for (int i = 0; i < COUNT; i++) {
            RestAssured.get("/jaxrs").then().statusCode(200);
            RestAssured.get("/jaxrs/foo-" + i).then().statusCode(200);
        }

        // Verify metrics
        Util.waitForMeters(registry.find("http.server.requests").timers(), COUNT);

        Assertions.assertEquals(COUNT, registry.find("http.server.requests")
                .tag("uri", "/jaxrs").timers().iterator().next().count());
        Assertions.assertEquals(COUNT, registry.find("http.server.requests")
                .tag("uri", "/jaxrs/{message}").timers().iterator().next().count());

        // Verify method producing a 400
        for (int i = 0; i < COUNT; i++) {
            RestAssured.get("/bad").then().statusCode(400);
            RestAssured.get("/bad/foo-" + i).then().statusCode(400);
        }

        Util.waitForMeters(registry.find("http.server.requests").timers(), COUNT * 2);

        Assertions.assertEquals(COUNT, registry.find("http.server.requests")
                .tag("uri", "/bad").tag("method", "GET").timers().iterator().next().count());
        Assertions.assertEquals(4, registry.find("http.server.requests")
                .tag("method", "GET").timers().size()); // Pattern recognized

    }

    @Path("/")
    @Singleton
    public static class Resource {

        @GET
        @Path("/jaxrs")
        public String jaxrs() {
            return "hello";
        }

        @GET
        @Path("/jaxrs/{message}")
        public String jaxrsWithPathParam(@PathParam("message") String message) {
            return "hello " + message;
        }

        @GET
        @Path("/bad")
        public Response bad() {
            return Response.status(400).build();
        }

        @GET
        @Path("/bad/{message}")
        public Response bad(@PathParam("message") String message) {
            return Response.status(400).build();
        }

        @GET
        @Path("/fail")
        public Response fail() {
            return Response.status(500).build();
        }

        @GET
        @Path("/fail/{message}")
        public Response fail(@PathParam("message") String message) {
            return Response.status(500).build();
        }

    }
}
