package io.quarkus.vertx.http.devmode;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;

public class RestAssuredOverrideByRuntimePropertyPortProdModeTest {

    @RegisterExtension
    static final QuarkusProdModeTest prodMode = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyRoute.class))
            .setRuntimeProperties(Map.of("quarkus.http.port", "9292"))
            .setRun(true)
            .forceRandomizedHttpPort();

    @Test
    void restAssuredUsesRandomPort() {
        RestAssured.given().get("/hello").then()
                .statusCode(200)
                .body(Matchers.equalTo("hello"));
        // forceRandomizedHttpPort() take precedence
        Assertions.assertThat(prodMode.configuredHttpPort()).isNotEqualTo(9292);
    }

    @ApplicationScoped
    static class MyRoute {
        public void register(@Observes Router router) {
            router.route("/hello").handler(rc -> rc.response().end("hello"));
        }
    }
}
