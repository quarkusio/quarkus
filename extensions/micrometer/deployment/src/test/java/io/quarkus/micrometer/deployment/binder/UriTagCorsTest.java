package io.quarkus.micrometer.deployment.binder;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.quarkus.micrometer.test.HelloResource;
import io.quarkus.micrometer.test.Util;
import io.quarkus.micrometer.test.VertxWebEndpoint;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class UriTagCorsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.binder.http-server.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder.vertx.enabled", "true")
            .overrideConfigKey("quarkus.http.cors", "true")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Util.class,
                            VertxWebEndpoint.class,
                            HelloResource.class));

    final static SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @BeforeAll
    static void setRegistry() {
        Metrics.addRegistry(registry);
    }

    @AfterAll()
    static void removeRegistry() {
        Metrics.removeRegistry(registry);
    }

    @Test
    public void testCORSPreflightRequest() throws InterruptedException {
        String origin = "http://custom.origin.quarkus";
        String methods = "GET,POST";
        String headers = "X-Custom";
        RestAssured.given()
                .header("Origin", origin)
                .header("Access-Control-Request-Method", methods)
                .header("Access-Control-Request-Headers", headers)
                .when().options("/hello/world").then()
                .header("Access-Control-Allow-Origin", origin)
                .header("Access-Control-Allow-Methods", methods)
                .header("Access-Control-Allow-Headers", headers)
                .statusCode(200);

        RestAssured.given()
                .header("Origin", origin)
                .header("Access-Control-Request-Method", methods)
                .header("Access-Control-Request-Headers", headers)
                .when().options("/vertx/echo/anything").then()
                .header("Access-Control-Allow-Origin", origin)
                .header("Access-Control-Allow-Methods", methods)
                .header("Access-Control-Allow-Headers", headers)
                .statusCode(200);

        RestAssured.given()
                .header("Origin", origin)
                .header("Access-Control-Request-Method", methods)
                .header("Access-Control-Request-Headers", headers)
                .when().options("/vertx/item/123").then()
                .header("Access-Control-Allow-Origin", origin)
                .header("Access-Control-Allow-Methods", methods)
                .header("Access-Control-Allow-Headers", headers)
                .statusCode(200);

        RestAssured.given()
                .when().options("/vertx/echo/anything").then()
                .statusCode(200);

        RestAssured.given()
                .when().options("/hello/world").then()
                .statusCode(200);

        int i = 0;
        do {
            i++;
            Thread.sleep(3); // Try to let updates to SimpleMeterRegistry settle
        } while (registry.find("http.server.requests").timers().size() < 3 && i < 10);

        // CORS pre-flight
        Assertions.assertEquals(1, registry.find("http.server.requests").tag("uri", "/cors-preflight").timers().size(),
                Util.foundServerRequests(registry, "/cors-preflight should be used for preflight requests"));

        Timer t = registry.find("http.server.requests").tag("uri", "/cors-preflight").timer();
        Assertions.assertEquals(3, t.count(), "/cors-preflight should be checked 3 times");

        // Normal OPTIONS requests
        Assertions.assertEquals(1, registry.find("http.server.requests").tag("uri", "/vertx/echo/{msg}").timers().size(),
                Util.foundServerRequests(registry, "/vertx/echo/{msg} for options request"));
        Assertions.assertEquals(1, registry.find("http.server.requests").tag("uri", "/hello/{message}").timers().size(),
                Util.foundServerRequests(registry, "/hello/{message} for options request"));
    }
}
