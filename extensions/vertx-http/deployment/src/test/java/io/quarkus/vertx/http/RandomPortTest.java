package io.quarkus.vertx.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.restassured.RestAssured;
import io.smallrye.config.SmallRyeConfig;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class RandomPortTest {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.http.test-port=0"),
                            "application.properties"));

    @TestHTTPResource
    URL url;
    @Inject
    VertxHttpConfig vertxHttpConfig;

    @Test
    public void portShouldNotBeZero() {
        assertThat(url.getPort()).isNotZero();
    }

    @Test
    public void testActualPortAccessibleToApp() {
        RestAssured.get("/test").then().body(Matchers.equalTo(Integer.toString(url.getPort())));
        RestAssured.get("/app").then().body(Matchers.equalTo(Integer.toString(url.getPort())));
    }

    @Test
    void mappingPortIsZero() {
        assertThat(vertxHttpConfig.testPort()).isZero();
    }

    public static class AppClass {
        @Inject
        SmallRyeConfig config;

        public void route(@Observes Router router) {
            router.route("/test").handler(new Handler<RoutingContext>() {
                @Override
                public void handle(RoutingContext event) {
                    event.response().end(config.getValue("quarkus.http.test-port", String.class));
                }
            });
            router.route("/app").handler(new Handler<RoutingContext>() {
                @Override
                public void handle(RoutingContext event) {
                    event.response().end(config.getValue("quarkus.http.port", String.class));
                }
            });
        }
    }
}
