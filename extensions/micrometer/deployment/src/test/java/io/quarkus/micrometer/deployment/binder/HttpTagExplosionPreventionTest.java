package io.quarkus.micrometer.deployment.binder;

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

public class HttpTagExplosionPreventionTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.binder.http-client.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder.http-server.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder.vertx.enabled", "true")
            .overrideConfigKey("pingpong/mp-rest/url", "${test.url}")
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .overrideConfigKey("quarkus.micrometer.binder.http-server.suppress4xx-errors", "true")
            .withApplicationRoot((jar) -> jar
                    .addClasses(Util.class,
                            Resource.class));

    @Inject
    MeterRegistry registry;

    @Test
    public void test() throws Exception {
        RestAssured.get("/api/hello").then().statusCode(200);
        Util.waitForMeters(registry.find("http.server.requests").timers(), 1);
        Assertions.assertEquals(1, registry.find("http.server.requests").tag("uri", "/api/hello").timers().size());

        RestAssured.get("/api/hello/foo").then().statusCode(200);
        Util.waitForMeters(registry.find("http.server.requests").timers(), 2);
        Assertions.assertEquals(1, registry.find("http.server.requests").tag("uri", "/api/hello/{message}").timers().size());

        RestAssured.delete("/api/hello").then().statusCode(405);
        Util.waitForMeters(registry.find("http.server.requests").timers(), 3);
        Assertions.assertEquals(1, registry.find("http.server.requests").tag("uri", "UNKNOWN").timers().size());

        RestAssured.delete("/api/hello/foo").then().statusCode(405);
        RestAssured.delete("/api/hello/bar").then().statusCode(405);
        RestAssured.delete("/api/hello/baz").then().statusCode(405);
        Util.waitForMeters(registry.find("http.server.requests").timers(), 6);
        Assertions.assertEquals(4,
                registry.find("http.server.requests").tag("uri", "UNKNOWN").timers().iterator().next().count());

        for (int i = 0; i < 10; i++) {
            RestAssured.get("/api/failure").then().statusCode(500);
            RestAssured.get("/api/failure/bar" + i).then().statusCode(500);
        }
        Util.waitForMeters(registry.find("http.server.requests").timers(), 6 + 10);

        Assertions.assertEquals(2, registry.find("http.server.requests").tag("uri", "UNKNOWN").timers().size()); // 2 different set of tags
        Assertions.assertEquals(1, registry.find("http.server.requests").tag("uri", "/api/failure/{message}").timers().size());
    }

    @Path("/")
    @Singleton
    public static class Resource {

        @GET
        @Path("api/hello/{message}")
        public String hello(@PathParam("message") String message) {
            return message;
        }

        @GET
        @Path("api/hello/")
        public String hello() {
            return "hello";
        }

        @GET
        @Path("api/failure")
        public Response failure() {
            return Response.status(500).build();
        }

        @GET
        @Path("api/failure/{message}")
        public Response failure(@PathParam("message") String message) {
            return Response.status(500).build();
        }

    }
}
