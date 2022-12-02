package io.quarkus.vertx.http.router;

import static org.hamcrest.Matchers.is;

import javax.enterprise.event.Observes;
import javax.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class RouterEventTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(RouteProducer.class));

    @Test
    public void testRoute() {
        RestAssured.when().get("/boom").then().statusCode(200).body(is("ok"));

        RestAssured.given()
                .body("An example body")
                .contentType("text/plain")
                .post("/post")
                .then()
                .body(is("1"));
    }

    @Singleton
    public static class RouteProducer {

        private static int counter;

        void observeRouter(@Observes Router router) {
            counter++;
            router.get("/boom").handler(ctx -> ctx.response().setStatusCode(200).end("ok"));
            Route post = router.post("/post");
            post.consumes("text/plain");
            post.handler(BodyHandler.create());
            post.handler(ctx -> ctx.response().end(Integer.toString(counter)));
        }

    }

}
