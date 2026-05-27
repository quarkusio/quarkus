package io.quarkus.vertx.http.devmode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;

public class RestAssuredPortProdModeTest {

    @RegisterExtension
    static final QuarkusProdModeTest test = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyRoute.class))
            .setRun(true)
            .withRandomPort();

    @Test
    void restAssuredUsesRandomPort() {
        RestAssured.given().get("/hello").then()
                .statusCode(200)
                .body(Matchers.equalTo("hello"));
    }

    @ApplicationScoped
    static class MyRoute {
        public void register(@Observes Router router) {
            router.route("/hello").handler(rc -> rc.response().end("hello"));
        }
    }
}
