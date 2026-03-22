package io.quarkus.vertx.http.proxy;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.ext.web.Router;

public class InvalidProxyHostTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RouteInitializer.class))
            .overrideRuntimeConfigKey("quarkus.http.proxy.proxy-address-forwarding", "true");

    @Test
    public void test() {
        given()
                .header("X-Forwarded-For", ":abcd")
                .get("/path")
                .then()
                .statusCode(400);

        given()
                .header("X-Forwarded-For", "1.2.3.4")
                .get("/path")
                .then()
                .body(equalTo("hello"));
    }

    @ApplicationScoped
    public static class RouteInitializer {

        public void register(@Observes Router router) {
            router.route("/path").handler(rc -> rc.response().end("hello"));
        }

    }
}
