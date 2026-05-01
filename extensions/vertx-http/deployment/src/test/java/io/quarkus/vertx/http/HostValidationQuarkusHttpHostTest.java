package io.quarkus.vertx.http;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;

import jakarta.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class HostValidationQuarkusHttpHostTest {

    @RegisterExtension
    static QuarkusDevModeTest runner = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(LocalBeanRegisteringRoute.class)
                    .addAsResource(new StringAsset("""
                            quarkus.http.host=localhost
                                  """), "application.properties"));

    @Test
    void localHostloopbackRequest() {
        given().header("Host", "127.0.0.1:8081")
                .get("/test").then()
                .statusCode(200)
                .body(equalTo("test route"));
    }

    @Test
    void localHostRequest() {
        given().header("Host", "localhost:8081")
                .get("/test").then()
                .statusCode(200)
                .body(equalTo("test route"));
    }

    @Test
    void publicHostRequest() {
        given().header("Host", "public-api.com:8080")
                .get("/test").then()
                .statusCode(403)
                .body(emptyString());
    }

    static class LocalBeanRegisteringRoute {

        void init(@Observes Router router) {
            Handler<RoutingContext> handler = rc -> rc.response().end("test route");

            router.get("/test").handler(handler);
        }
    }
}
