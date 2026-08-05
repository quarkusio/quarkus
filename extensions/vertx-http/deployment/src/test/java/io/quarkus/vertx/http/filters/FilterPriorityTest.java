package io.quarkus.vertx.http.filters;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.http.runtime.filters.Filters;
import io.vertx.ext.web.Router;

/**
 * Tests that filter priority ordering works correctly.
 * Higher priority values execute first.
 */
public class FilterPriorityTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Routes.class));

    @Test
    public void testFilterExecutionOrder() {
        // Both filters set X-Order, higher priority (20) runs first, then lower (10) overwrites
        given().get("/test")
                .then()
                .statusCode(200)
                .header("X-Order", is("low-priority-last"))
                .header("X-High-Priority", is("executed"))
                .header("X-Low-Priority", is("executed"))
                .body(is("ok"));
    }

    @Test
    public void testAllFiltersExecuted() {
        given().get("/test")
                .then()
                .statusCode(200)
                .header("X-High-Priority", is("executed"))
                .header("X-Low-Priority", is("executed"));
    }

    @ApplicationScoped
    static class Routes {
        public void filters(@Observes Filters filters) {
            // Higher priority = executes first
            filters.register(rc -> {
                rc.response().putHeader("X-High-Priority", "executed");
                rc.response().putHeader("X-Order", "high-priority-first");
                rc.next();
            }, 20);

            filters.register(rc -> {
                rc.response().putHeader("X-Low-Priority", "executed");
                rc.response().putHeader("X-Order", "low-priority-last");
                rc.next();
            }, 10);
        }

        public void register(@Observes Router router) {
            router.get("/test").handler(rc -> rc.response().end("ok"));
        }
    }
}
