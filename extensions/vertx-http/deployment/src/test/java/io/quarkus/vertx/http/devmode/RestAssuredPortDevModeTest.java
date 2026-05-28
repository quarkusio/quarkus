package io.quarkus.vertx.http.devmode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;

public class RestAssuredPortDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyRoute.class))
            .forceRandomizedHttpPort();

    @Test
    void restAssuredUsesConfiguredPort() {
        RestAssured.given().get("/hello").then()
                .statusCode(200)
                .body(Matchers.equalTo("hello"));
        // should be random
        Assertions.assertThat(test.configuredHttpPort()).isNotEqualTo(8080);
        Assertions.assertThat(test.configuredHttpPort()).isNotEqualTo(8081);
    }

    @ApplicationScoped
    static class MyRoute {
        public void register(@Observes Router router) {
            router.route("/hello").handler(rc -> rc.response().end("hello"));
        }
    }
}
