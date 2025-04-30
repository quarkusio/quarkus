package io.quarkus.smallrye.health.test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.smallrye.health.runtime.SmallRyeHealthHandler;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

class HealthRouteDispatchThreadTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Routes.class, OffloadingHandler.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @Test
    void testHealthInvokedOnBlockingThread() {
        RestAssured.given()
                .when().get("/my-health")
                .then().statusCode(200);

    }

    @ApplicationScoped
    static final class Routes {

        public void init(@Observes Router router) {
            router.get("/my-health").handler(new OffloadingHandler());
            router.get("/my-health").handler(new SmallRyeHealthHandler());
        }
    }

    static final class OffloadingHandler implements Handler<RoutingContext> {

        @Override
        public void handle(RoutingContext routingContext) {
            // execute next handler on the blocking thread
            ExecutorService executor = null;
            try {
                executor = Executors.newSingleThreadExecutor();
                executor.execute(routingContext::next);
            } finally {
                if (executor != null) {
                    executor.shutdown();
                }
            }
        }
    }
}
