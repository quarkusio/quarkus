package io.quarkus.vertx.http.router;

import static org.hamcrest.Matchers.is;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;

/**
 * Test is located here so that {@code VertxCurrentContextFactory} is used within req. context implementation.
 * See also https://github.com/quarkusio/quarkus/issues/37741
 */
public class ReqContextActivationTerminationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(BeanWithObserver.class));

    @Test
    public void testRoute() {
        RestAssured.when().get("/boom").then().statusCode(200).body(is("ok"));
    }

    @Singleton
    public static class BeanWithObserver {

        private static int counter;

        void observeRouter(@Observes StartupEvent startup, Router router) {
            router.get("/boom").handler(ctx -> {
                // context starts as inactive; we perform manual activation/termination and assert
                Assertions.assertEquals(false, Arc.container().requestContext().isActive());
                Arc.container().requestContext().activate();
                Assertions.assertEquals(true, Arc.container().requestContext().isActive());
                Arc.container().requestContext().terminate();
                Assertions.assertEquals(false, Arc.container().requestContext().isActive());
                ctx.response().setStatusCode(200).end("ok");
            });
        }

    }
}
