package io.quarkus.vertx.http;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.ext.web.Router;

/**
 * Tests that route ordering works correctly when multiple handlers
 * are registered for overlapping paths with different orders.
 */
public class RouteOrderingTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Routes.class));

    @Test
    public void testOrderedRoutesExecuteInCorrectOrder() {
        // The response header should reflect the order of execution
        given().get("/ordered")
                .then()
                .statusCode(200)
                .header("X-First", is("1"))
                .header("X-Second", is("2"))
                .body(is("done"));
    }

    @Test
    public void testSpecificRouteMatchesBeforeWildcard() {
        given().get("/specific")
                .then()
                .statusCode(200)
                .body(is("specific"));
    }

    @Test
    public void testWildcardRouteFallback() {
        given().get("/anything-else")
                .then()
                .statusCode(200)
                .body(is("wildcard"));
    }

    @ApplicationScoped
    static class Routes {
        public void register(@Observes Router router) {
            // Lower order = executes first
            router.route("/ordered").order(1).handler(rc -> {
                rc.response().putHeader("X-First", "1");
                rc.next();
            });
            router.route("/ordered").order(2).handler(rc -> {
                rc.response().putHeader("X-Second", "2");
                rc.response().end("done");
            });

            // Specific route should match before wildcard
            router.route("/specific").handler(rc -> rc.response().end("specific"));
            router.route("/*").order(Integer.MAX_VALUE).handler(rc -> rc.response().end("wildcard"));
        }
    }
}
