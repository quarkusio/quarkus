package io.quarkus.vertx.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;

/**
 * Reproduce <a href="https://github.com/quarkusio/quarkus/issues/36234">NullPointerException for request with empty
 * Host header</a>.
 */
public class EmptyHostTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(BeanRegisteringRouteUsingObserves.class));

    @Test
    public void testWithEmptyHost() {
        assertEquals(RestAssured.given().header("Host", "").get("/hello").asString(), "Hello World! ");

    }

    @ApplicationScoped
    static class BeanRegisteringRouteUsingObserves {

        public void register(@Observes Router router) {

            router.route("/hello").handler(ctx -> ctx.response().end("Hello World! " + ctx.request().host()));
        }

    }

}
