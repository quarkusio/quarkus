package io.quarkus.vertx.http;

import static io.restassured.RestAssured.given;

import jakarta.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class HostValidationAllowedHostsTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(LocalBeanRegisteringRoute.class)
                    .addAsResource(new StringAsset("""
                            quarkus.http.host-validation.allowed-hosts=public-api.com,another-public-api.com
                            """), "application.properties"));

    @Test
    void localHostRequest() {
        given().header("Host", "localhost:8081")
                .get("/test").then()
                .statusCode(403);
    }

    @Test
    void localHostloopbackRequest() {
        given().header("Host", "127.0.0.1:8081")
                .get("/test").then()
                .statusCode(403);
    }

    @Test
    void publicHostRequest() {
        given().header("Host", "public-api.com:8080")
                .get("/test").then()
                .statusCode(200);
    }

    @Test
    void anotherPublicHostRequest() {
        given().header("Host", "another-public-api.com:8080")
                .get("/test").then()
                .statusCode(200);
    }

    static class LocalBeanRegisteringRoute {

        void init(@Observes Router router) {
            Handler<RoutingContext> handler = rc -> rc.response().end("test route");

            router.get("/test").handler(handler);
        }
    }
}
