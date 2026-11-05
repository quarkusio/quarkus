package io.quarkus.smallrye.health.test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.http.runtime.filters.Filters;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import io.smallrye.common.vertx.ContextLocals;
import io.vertx.ext.web.RoutingContext;

/**
 * Blocking health checks must see the Vert.x context locals stored by the request filters (for example a tenant id),
 * while staying isolated from each other.
 */
class BlockingChecksRequestContextLocalsTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TenantFilter.class, TenantCheck1.class, TenantCheck2.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    void testBlockingChecksSeeRequestLocals() {
        try {
            RestAssured.defaultParser = Parser.JSON;
            RestAssured.given().header("X-Tenant", "acme").when().get("/q/health").then()
                    .body("status", is("UP"),
                            "checks.size()", is(2),
                            "checks.data.tenant", containsInAnyOrder("acme", "acme"),
                            "checks.data.other-check-key", containsInAnyOrder("absent", "absent"));
        } finally {
            RestAssured.reset();
        }
    }

    @ApplicationScoped
    public static class TenantFilter {

        void register(@Observes Filters filters) {
            filters.register(new io.vertx.core.Handler<RoutingContext>() {
                @Override
                public void handle(RoutingContext rc) {
                    String tenant = rc.request().getHeader("X-Tenant");
                    if (tenant != null) {
                        ContextLocals.put("tenant", tenant);
                    }
                    rc.next();
                }
            }, 100);
        }
    }

    static HealthCheckResponse describe(String name, String ownKey, String otherKey) {
        String tenant = ContextLocals.<String> get("tenant").orElse("absent");
        ContextLocals.put(ownKey, "set");
        String other = ContextLocals.<String> get(otherKey).orElse("absent");
        return HealthCheckResponse.named(name).up().withData("tenant", tenant).withData("other-check-key", other).build();
    }

    @Liveness
    public static class TenantCheck1 implements HealthCheck {

        @Override
        public HealthCheckResponse call() {
            return describe("check1", "check1", "check2");
        }
    }

    @Liveness
    public static class TenantCheck2 implements HealthCheck {

        @Override
        public HealthCheckResponse call() {
            return describe("check2", "check2", "check1");
        }
    }
}
