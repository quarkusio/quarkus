package io.quarkus.vertx.http.devmode;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;

public class RestAssuredRandomPortDevModeTest {
    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(HelloRoute.class)
                    .addAsResource(new StringAsset("quarkus.http.port=0"), "application.properties"));

    @Test
    void restAssuredBaseUriIsSetWithRandomPort() {
        assertTrue(RestAssured.baseURI.startsWith("http"), "baseURI should be set: " + RestAssured.baseURI);
        assertNotEquals(8080, RestAssured.port, "port should be random, not default 8080");
        assertTrue(RestAssured.port > 0, "port should be set: " + RestAssured.port);

        RestAssured.get("/hello").then()
                .statusCode(200)
                .body(is("hello"));
    }

    @ApplicationScoped
    public static class HelloRoute {
        @Inject
        Router router;

        public void register(@Observes StartupEvent ev) {
            router.get("/hello").handler(rc -> rc.response().end("hello"));
        }
    }
}
