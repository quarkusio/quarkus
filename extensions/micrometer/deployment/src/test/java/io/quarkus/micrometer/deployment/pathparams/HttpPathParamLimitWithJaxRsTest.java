package io.quarkus.micrometer.deployment.pathparams;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.micrometer.test.Util;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class HttpPathParamLimitWithJaxRsTest {
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
    public static final int ARITY_LIMIT = 100;

    @Test
    void testWithResteasyOK() throws InterruptedException {
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

        // Verify 405 responses
        for (int i = 0; i < COUNT; i++) {
            RestAssured.delete("/jaxrs").then().statusCode(405);
            RestAssured.patch("/jaxrs/foo-" + i).then().statusCode(405);
        }

        Util.waitForMeters(registry.find("http.server.requests").timers(), COUNT * 2);

        Assertions.assertEquals(COUNT, registry.find("http.server.requests")
                .tag("uri", "/jaxrs").tag("method", "DELETE").timers().iterator().next().count());
        Assertions.assertEquals(ARITY_LIMIT - 2, registry.find("http.server.requests")
                .tag("method", "PATCH").timers().size()); // -2 because of the two other uri: /jaxrs and /jaxrs/{message}.
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

    }
}
