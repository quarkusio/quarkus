package io.quarkus.micrometer.deployment.binder;

import static io.restassured.RestAssured.when;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Application;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.micrometer.test.ServletEndpoint;
import io.quarkus.micrometer.test.Util;
import io.quarkus.micrometer.test.VertxWebEndpoint;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class UriTagWithHttpApplicationRootTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.http.root-path", "/foo")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.binder.http-client.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder.http-server.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder.vertx.enabled", "true")
            .overrideConfigKey("pingpong/mp-rest/url", "${test.url}")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Util.class,
                            PingPongResource.class,
                            PingPongResource.PingPongRestClient.class,
                            ServletEndpoint.class,
                            VertxWebEndpoint.class,
                            BarApp.class));

    @Inject
    MeterRegistry registry;

    @Test
    public void test() throws Exception {
        RestAssured.basePath = "/";

        // If you invoke requests, http server and client meters should be registered
        // Leading context root (/foo) should be stripped from resulting _server_ tag
        // Application path (/bar) only impacts REST endpoints

        when().get("/foo/bar/ping/one").then().statusCode(200);
        when().get("/foo/bar/ping/two").then().statusCode(200);
        when().get("/foo/bar/ping/three").then().statusCode(200);
        when().get("/foo/vertx/item/123").then().statusCode(200);
        when().get("/foo/servlet/12345").then().statusCode(200);

        // TODO: These are the current answers, but they aren't quite right

        // URIs for server should include Application Path: /bar/ping/{message}, /bar/pong/{message}
        Assertions.assertEquals(1, registry.find("http.server.requests").tag("uri", "/ping/{message}").timers().size(),
                Util.foundServerRequests(registry, "/bar/ping/{message} should be returned by JAX-RS"));
        Assertions.assertEquals(1, registry.find("http.server.requests").tag("uri", "/pong/{message}").timers().size(),
                Util.foundServerRequests(registry, "/bar/pong/{message} should be returned by JAX-RS"));

        // Application Path does not apply to non-rest endpoints: /vertx/item/{id}
        Assertions.assertEquals(1, registry.find("http.server.requests").tag("uri", "/vertx/item/{id}").timers().size(),
                Util.foundServerRequests(registry,
                        "Vert.x Web template path (/vertx/item/:id) should be detected/translated to /vertx/item/{id}."));
    }

    @ApplicationPath("/bar")
    public static class BarApp extends Application {
    }

    @Path("/")
    @Singleton
    public static class PingPongResource {

        @RegisterRestClient(configKey = "pingpong")
        public interface PingPongRestClient {

            @Path("/foo/bar/pong/{message}")
            @GET
            String pingpong(@PathParam("message") String message);
        }

        @Inject
        @RestClient
        PingPongRestClient pingRestClient;

        @GET
        @Path("pong/{message}")
        public String pong(@PathParam("message") String message) {
            return message;
        }

        @GET
        @Path("ping/{message}")
        public String ping(@PathParam("message") String message) {
            return pingRestClient.pingpong(message);
        }
    }
}
