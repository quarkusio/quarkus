package io.quarkus.websockets.next.test.devmode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.websockets.next.BasicWebSocketConnector;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;

/**
 * Just to make sure the Dev UI is not broken if no server endpoint is defined.
 */
public class NoServerEndpointDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest testConfig = new QuarkusDevModeTest()
            .withApplicationRoot(root -> root
                    .addClass(MyBean.class));

    @Test
    public void testConnectorIsInjected() {
        assertEquals("1", RestAssured.get("mybeantest").then().statusCode(200).extract().body().asString());
    }

    @Singleton
    public static class MyBean {

        @Inject
        BasicWebSocketConnector connector;

        void addRoute(@Observes Router router) {
            router.get("/mybeantest").handler(rc -> {
                rc.end(connector != null ? "1" : "0");
            });
        }

    }

}
