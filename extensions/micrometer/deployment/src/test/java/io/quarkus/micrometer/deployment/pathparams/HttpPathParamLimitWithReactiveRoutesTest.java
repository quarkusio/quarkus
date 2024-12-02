package io.quarkus.micrometer.deployment.pathparams;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.micrometer.test.Util;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.Route;
import io.restassured.RestAssured;

public class HttpPathParamLimitWithReactiveRoutesTest {
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
    void testWithReactiveRouteOK() throws InterruptedException {
        registry.clear();
        // Verify OK response
        for (int i = 0; i < COUNT; i++) {
            RestAssured.get("/rr").then().statusCode(200);
            RestAssured.get("/rr/foo-" + i).then().statusCode(200);
        }

        // Verify metrics
        Util.waitForMeters(registry.find("http.server.requests").timers(), COUNT);

        Assertions.assertEquals(COUNT, registry.find("http.server.requests")
                .tag("uri", "/rr").timers().iterator().next().count());
        Assertions.assertEquals(COUNT, registry.find("http.server.requests")
                .tag("uri", "/rr/{message}").timers().iterator().next().count());

        // Verify 405 responses
        for (int i = 0; i < COUNT; i++) {
            RestAssured.delete("/rr").then().statusCode(405);
            RestAssured.patch("/rr/foo-" + i).then().statusCode(501); // Not totally sure why reactive routes return a 501, it's not necessarily wrong, just different.
        }

        Util.waitForMeters(registry.find("http.server.requests").timers(), COUNT * 2);

        Assertions.assertEquals(COUNT, registry.find("http.server.requests")
                .tag("uri", "/rr").tag("method", "DELETE").timers().iterator().next().count());
        Assertions.assertEquals(ARITY_LIMIT - 2, registry.find("http.server.requests")
                .tag("method", "PATCH").timers().size()); // -2 because of the two other uri: /rr and /rr/{message}.
    }

    @Singleton
    public static class Resource {

        @Route(path = "/rr", methods = Route.HttpMethod.GET)
        public String rr() {
            return "hello";
        }

        @Route(path = "/rr/:message", methods = Route.HttpMethod.GET)
        public String rrWithPathParam(@Param String message) {
            return "hello " + message;
        }
    }
}
