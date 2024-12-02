package io.quarkus.micrometer.deployment.pathparams;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.micrometer.test.Util;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;

public class HttpPathParamLimitWithProgrammaticRoutes500Test {
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
    void testWithProgrammaticRoutes500() throws InterruptedException {
        registry.clear();
        // Verify OK response
        for (int i = 0; i < COUNT; i++) {
            RestAssured.get("/programmatic").then().statusCode(200);
            RestAssured.get("/programmatic/foo-" + i).then().statusCode(200);
        }

        // Verify metrics
        Util.waitForMeters(registry.find("http.server.requests").timers(), COUNT);

        Assertions.assertEquals(COUNT, registry.find("http.server.requests")
                .tag("uri", "/programmatic").timers().iterator().next().count());
        Assertions.assertEquals(COUNT, registry.find("http.server.requests")
                .tag("uri", "/programmatic/{message}").timers().iterator().next().count());

        // Verify 405 responses
        for (int i = 0; i < COUNT; i++) {
            RestAssured.get("/bad").then().statusCode(500);
            RestAssured.get("/bad/foo-" + i).then().statusCode(500);
        }

        Util.waitForMeters(registry.find("http.server.requests").timers(), COUNT * 2);

        Assertions.assertEquals(COUNT, registry.find("http.server.requests")
                .tag("uri", "/bad").tag("method", "GET").timers().iterator().next().count());
        Assertions.assertEquals(4, registry.find("http.server.requests")
                .tag("method", "GET").timers().size());
    }

    @Singleton
    public static class Resource {

        void registerProgrammaticRoutes(@Observes Router router) {
            router.get("/programmatic").handler(rc -> {
                rc.response().end("hello");
            });
            router.get("/programmatic/:message").handler(rc -> {
                rc.response().end("hello " + rc.pathParam("message"));
            });

            router.get("/bad").handler(rc -> {
                rc.response().setStatusCode(500).end("hello");
            });
            router.get("/bad/:message").handler(rc -> {
                rc.response().setStatusCode(500).end("hello " + rc.pathParam("message"));
            });
        }

    }
}
