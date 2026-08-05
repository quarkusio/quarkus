package io.quarkus.vertx.http;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class MaxBodySizeTest {

    private static final String APP_PROPS = "quarkus.http.limits.max-body-size=1K\n";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties")
                    .addClasses(Routes.class));

    @Test
    public void testRequestWithinLimit() {
        given()
                .body("small")
                .contentType("text/plain")
                .post("/echo")
                .then()
                .statusCode(200)
                .body(is("small"));
    }

    @Test
    public void testRequestExceedingLimitWithContentLength() {
        // Create a body larger than 1K
        String largeBody = "x".repeat(2048);
        given()
                .body(largeBody)
                .contentType("text/plain")
                .post("/echo")
                .then()
                .statusCode(413);
    }

    @Test
    public void testRequestExactlyAtLimit() {
        // 1K = 1024 bytes
        String exactBody = "x".repeat(1024);
        given()
                .body(exactBody)
                .contentType("text/plain")
                .post("/echo")
                .then()
                .statusCode(200);
    }

    @ApplicationScoped
    static class Routes {
        public void register(@Observes Router router) {
            router.post("/echo").handler(BodyHandler.create());
            router.post("/echo").handler(rc -> rc.response().end(rc.body().asString()));
        }
    }
}
