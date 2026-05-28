package io.quarkus.vertx.http.devmode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;

public class RestAssuredPortOverrideByAppPropertiesDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest devMode = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyRoute.class)
                    .addAsResource(new StringAsset("""
                            quarkus.http.port=42929
                            """), "application.properties"))
            .forceRandomizedHttpPort();

    @Test
    void restAssuredUsesConfiguredPort() {
        RestAssured.given().get("/hello").then()
                .statusCode(200)
                .body(Matchers.equalTo("hello"));
        //        forceRandomizedHttpPort() take precedence
        Assertions.assertThat(devMode.configuredHttpPort()).isNotEqualTo(42929);
    }

    @ApplicationScoped
    static class MyRoute {
        public void register(@Observes Router router) {
            router.route("/hello").handler(rc -> rc.response().end("hello"));
        }
    }
}
