package io.quarkus.micrometer.deployment.binder;

import static io.restassured.RestAssured.when;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.micrometer.test.PingPongResource;
import io.quarkus.micrometer.test.ServletEndpoint;
import io.quarkus.micrometer.test.Util;
import io.quarkus.micrometer.test.VertxWebEndpoint;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class UriTagTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.binder.http-client.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder.http-server.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder.http-server.match-patterns", "/one=/two")
            .overrideConfigKey("quarkus.micrometer.binder.http-server.ignore-patterns", "/two")
            .overrideConfigKey("quarkus.micrometer.binder.vertx.enabled", "true")
            .overrideConfigKey("pingpong/mp-rest/url", "${test.url}")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Util.class,
                            PingPongResource.class,
                            PingPongResource.PingPongRestClient.class,
                            ServletEndpoint.class,
                            VertxWebEndpoint.class));

    @Inject
    MeterRegistry registry;

    @Test
    public void testMetricFactoryCreatedMetrics() throws Exception {
        RestAssured.basePath = "/";

        // If you invoke requests, http server and client meters should be registered

        when().get("/one").then().statusCode(200);
        when().get("/two").then().statusCode(200);
        when().get("/ping/one").then().statusCode(200);
        when().get("/ping/two").then().statusCode(200);
        when().get("/ping/three").then().statusCode(200);
        when().get("/vertx/item/123").then().statusCode(200);
        when().get("/vertx/item/1/123").then().statusCode(200);
        when().get("/servlet/12345").then().statusCode(200);

        System.out.println("Server paths\n" + Util.listMeters(registry.find("http.server.requests").meters(), "uri"));
        System.out.println("Client paths\n" + Util.listMeters(registry.find("http.server.requests").meters(), "uri"));

        // /one should map to /two, which is ignored. Neither should exist w/ timers
        Assertions.assertEquals(0, registry.find("http.server.requests").tag("uri", "/one").timers().size(),
                "/one is mapped to /two, which should be ignored. Found:\n"
                        + Util.listMeters(registry.find("http.server.requests").meters(), "uri"));
        Assertions.assertEquals(0, registry.find("http.server.requests").tag("uri", "/two").timers().size(),
                "/two should be ignored. Found:\n"
                        + Util.listMeters(registry.find("http.server.requests").meters(), "uri"));

        // URIs for server: /ping/{message}, /pong/{message}, /vertx/item/{id}, /vertx/item/{id}/{sub}, /servlet/
        Assertions.assertEquals(1, registry.find("http.server.requests").tag("uri", "/ping/{message}").timers().size(),
                "/ping/{message} should be returned by JAX-RS. Found:\n"
                        + Util.listMeters(registry.find("http.server.requests").meters(), "uri"));
        Assertions.assertEquals(1, registry.find("http.server.requests").tag("uri", "/pong/{message}").timers().size(),
                "/pong/{message} should be returned by JAX-RS. Found:\n"
                        + Util.listMeters(registry.find("http.server.requests").meters(), "uri"));
        Assertions.assertEquals(1, registry.find("http.server.requests").tag("uri", "/vertx/item/{id}").timers().size(),
                "Vert.x Web template path (/vertx/item/:id) should be detected/translated to /vertx/item/{id}. Found:\n"
                        + Util.listMeters(registry.find("http.server.requests").meters(), "uri"));
        Assertions.assertEquals(1, registry.find("http.server.requests").tag("uri", "/vertx/item/{id}/{sub}").timers().size(),
                "Vert.x Web template path (/vertx/item/:id/:sub) should be detected/translated to /vertx/item/{id}/{sub}. Found:\n"
                        + Util.listMeters(registry.find("http.server.requests").meters(), "uri"));

        Assertions.assertEquals(1, registry.find("http.server.requests").tag("uri", "/servlet").timers().size(),
                "Servlet path (/servlet) should be used for servlet. Found:\n"
                        + Util.listMeters(registry.find("http.server.requests").meters(), "uri"));

        // TODO: #15231
        // URIs For client: /pong/{message}
        //        Assertions.assertEquals(1, registry.find("http.client.requests").tag("uri", "/pong/{message}").timers().size(),
        //                "/pong/{message} should be returned by Rest client.  Found:\n"
        //                        + Util.listMeters(registry.find("http.client.requests").meters(), "uri"));
    }
}
