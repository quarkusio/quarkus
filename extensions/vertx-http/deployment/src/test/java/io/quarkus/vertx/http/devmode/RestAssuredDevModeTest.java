package io.quarkus.vertx.http.devmode;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;

public class RestAssuredDevModeTest {
    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addClass(HelloRoute.class));

    @Test
    void restAssuredBaseUriIsSet() {
        assertTrue(RestAssured.baseURI.startsWith("http"), "baseURI should be set: " + RestAssured.baseURI);
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
