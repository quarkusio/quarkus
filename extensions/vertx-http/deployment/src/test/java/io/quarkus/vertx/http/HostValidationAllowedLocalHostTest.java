package io.quarkus.vertx.http;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;

import jakarta.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class HostValidationAllowedLocalHostTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(LocalBeanRegisteringRoute.class)
                    .addAsResource(new StringAsset(
                            "quarkus.http.host-validation.require-localhost=true\n"),
                            "application.properties"));

    @Test
    void localHostRequest() {
        given().header("Host", "localhost:8081")
                .get("/test").then()
                .statusCode(200)
                .body(equalTo("test route"));
    }

    @Test
    void localHostCaseInsensitiveRequest() {
        given().header("Host", "LocalHost:8081")
                .get("/test").then()
                .statusCode(200)
                .body(equalTo("test route"));
    }

    @Test
    void loopbackRequest() {
        given().header("Host", "127.0.0.1:8081")
                .get("/test").then()
                .statusCode(200)
                .body(equalTo("test route"));
    }

    @Test
    void publicHostRequest() {
        given().header("Host", "public-api.com:8080")
                .get("/test").then()
                .statusCode(400)
                .body(emptyString());
    }

    static class LocalBeanRegisteringRoute {

        void init(@Observes Router router) {
            Handler<RoutingContext> handler = rc -> rc.response().end("test route");
            router.get("/test").handler(handler);
        }
    }
}
