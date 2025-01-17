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
import io.vertx.ext.web.RoutingContext;

public class HttpPathParamLimitWithReactiveRoutes500Test {
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
    void testWithReactiveRoute500() throws InterruptedException {
        registry.clear();

        for (int i = 0; i < COUNT; i++) {
            RestAssured.get("/rr").then().statusCode(500);
            RestAssured.get("/rr/foo-" + i).then().statusCode(500);
        }

        Util.waitForMeters(registry.find("http.server.requests").timers(), COUNT);

        Assertions.assertEquals(COUNT, registry.find("http.server.requests")
                .tag("uri", "/rr").tag("method", "GET")
                .timers().iterator().next().count());
        Assertions.assertEquals(COUNT, registry.find("http.server.requests")
                .tag("method", "GET").tag("uri", "/rr/{message}")
                .timers().iterator().next().count());
    }

    @Singleton
    public static class Resource {

        @Route(path = "/rr", methods = Route.HttpMethod.GET)
        public void rr(RoutingContext rc) {
            rc.response().setStatusCode(500).end("hello");
        }

        @Route(path = "/rr/:message", methods = Route.HttpMethod.GET)
        public void rrWithPathParam(@Param String message, RoutingContext rc) {
            rc.response().setStatusCode(500).end("hello " + message);
        }
    }
}
