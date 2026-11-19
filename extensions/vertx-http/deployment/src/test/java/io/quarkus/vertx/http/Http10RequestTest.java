package io.quarkus.vertx.http;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import jakarta.enterprise.event.Observes;

import org.apache.http.ProtocolVersion;
import org.apache.http.params.CoreProtocolPNames;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.vertx.ext.web.Router;

// Reproduces part of https://github.com/quarkusio/quarkus/issues/58546
public class Http10RequestTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(BeanRegisteringRoute.class))
            .setBeforeAllCustomizer(() -> System.setProperty("vertx.disableWebsockets", "false"))
            .setAfterAllCustomizer(() -> System.clearProperty("vertx.disableWebsockets"));

    @Test
    public void testHttp10Request() {
        given(new RequestSpecBuilder()
                .setConfig(RestAssuredConfig.config().httpClient(
                        new HttpClientConfig().setParam(CoreProtocolPNames.PROTOCOL_VERSION,
                                new ProtocolVersion("HTTP", 1, 0))))
                .build())
                .get("/test")
                .then()
                .statusCode(200)
                .body(equalTo("hello"));
    }

    static class BeanRegisteringRoute {

        void init(@Observes Router router) {
            router.get("/test").handler(rc -> rc.response().end("hello"));
        }
    }
}
