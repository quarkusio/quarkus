package io.quarkus.micrometer.deployment.binder;

import static io.restassured.RestAssured.when;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.quarkus.micrometer.test.HelloResource;
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
                            VertxWebEndpoint.class,
                            HelloResource.class));

    static SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @BeforeAll
    static void setRegistry() {
        registry = new SimpleMeterRegistry();
        Metrics.addRegistry(registry);
    }

    @AfterAll()
    static void removeRegistry() {
        Metrics.removeRegistry(registry);
    }

    @Test
    public void testGetRequests() throws Exception {
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

        System.out.println("Server paths\n" + Util.listMeters(registry, "http.server.requests"));
        System.out.println("Client paths\n" + Util.listMeters(registry, "http.client.requests"));

        // /one should map to /two, which is ignored. Neither should exist w/ timers
        Assertions.assertEquals(0, registry.find("http.server.requests").tag("uri", "/one").timers().size(),
                Util.foundServerRequests(registry, "/one is mapped to /two, which should be ignored."));
        Assertions.assertEquals(0, registry.find("http.server.requests").tag("uri", "/two").timers().size(),
                Util.foundServerRequests(registry, "/two should be ignored."));

        // URIs for server: /ping/{message}, /pong/{message}, /vertx/item/{id}, /vertx/item/{id}/{sub}, /servlet/
        Assertions.assertEquals(1, registry.find("http.server.requests").tag("uri", "/ping/{message}").timers().size(),
                Util.foundServerRequests(registry, "/ping/{message} should be returned by JAX-RS."));
        Assertions.assertEquals(1, registry.find("http.server.requests").tag("uri", "/pong/{message}").timers().size(),
                Util.foundServerRequests(registry, "/pong/{message} should be returned by JAX-RS."));
        Assertions.assertEquals(1, registry.find("http.server.requests").tag("uri", "/vertx/item/{id}").timers().size(),
                Util.foundServerRequests(registry,
                        "Vert.x Web template path (/vertx/item/:id) should be detected/translated to /vertx/item/{id}."));
        Assertions.assertEquals(1, registry.find("http.server.requests").tag("uri", "/vertx/item/{id}/{sub}").timers().size(),
                Util.foundServerRequests(registry,
                        "Vert.x Web template path (/vertx/item/:id/:sub) should be detected/translated to /vertx/item/{id}/{sub}."));
        Assertions.assertEquals(1, registry.find("http.server.requests").tag("uri", "/servlet").timers().size(),
                Util.foundServerRequests(registry, "Servlet path (/servlet) should be used for servlet"));

        // TODO: #15231
        // URIs For client: /pong/{message}
        //        Assertions.assertEquals(1, registry.find("http.client.requests").tag("uri", "/pong/{message}").timers().size(),
        //                Util.foundClientRequests(registry, "/pong/{message} should be returned by Rest client."));

        when().get("/hello/one").then().statusCode(200);
        when().get("/hello/two").then().statusCode(200);
        when().head("/hello/three").then().statusCode(200);
        when().head("/hello/four").then().statusCode(200);
        when().get("/vertx/echo/thing1").then().statusCode(200);
        when().get("/vertx/echo/thing2").then().statusCode(200);
        when().head("/vertx/echo/thing3").then().statusCode(200);
        when().head("/vertx/echo/thing4").then().statusCode(200);

        // GET and HEAD are two different methods, so double these up
        Assertions.assertEquals(2, registry.find("http.server.requests").tag("uri", "/hello/{message}").timers().size(),
                Util.foundServerRequests(registry, "/hello/{message} should have two timers (GET and HEAD)."));
        Assertions.assertEquals(2, registry.find("http.server.requests").tag("uri", "/vertx/echo/{msg}").timers().size(),
                Util.foundServerRequests(registry, "/vertx/echo/{msg} should have two timers (GET and HEAD)."));
    }
}
