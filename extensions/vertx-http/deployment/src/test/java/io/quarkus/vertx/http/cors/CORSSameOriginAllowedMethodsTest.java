package io.quarkus.vertx.http.cors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import jakarta.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

class CORSSameOriginAllowedMethodsTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(LocalBeanRegisteringRoute.class)
                    .addAsResource(new StringAsset("""
                            quarkus.http.cors.enabled=true
                            quarkus.http.cors.methods=GET,PUT,POST
                            quarkus.http.cors.origins=http://www.quarkus.io
                            """), "application.properties"));

    @Test
    void testMethodAllowedForCrossOriginRequest() {
        String origin = "http://localhost:8081";
        given().header("Origin", origin)
                .get("/test").then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", origin);
        origin = "http://www.quarkus.io";
        given().header("Origin", origin)
                .get("/test").then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", origin);
    }

    @Test
    void testMethodNotAllowedForCrossOriginRequest() {
        // same origin request
        String origin = "http://localhost:8081";
        given().header("Origin", origin)
                .patch("/test").then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", origin);
        // cross origin request
        origin = "http://www.quarkus.io";
        given().header("Origin", origin)
                .patch("/test").then()
                .statusCode(403)
                .statusLine(containsString("CORS Rejected - Invalid method"));
    }

    static class LocalBeanRegisteringRoute {

        void init(@Observes Router router) {
            Handler<RoutingContext> handler = rc -> rc.response().end("test route");

            router.get("/test").handler(handler);
            router.patch("/test").handler(handler);
            router.options("/test").handler(handler);
        }
    }
}
