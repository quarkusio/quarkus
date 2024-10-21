package io.quarkus.websockets.next.test.nonwebsocketconnection;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.vertx.ext.web.Router;

public class NonWebSocketConnectionIgnoredTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Echo.class);
            });

    @Test
    void testNonWebSocketConnection() {
        given().when()
                .get("/echo")
                .then()
                .statusCode(200)
                .body(is("ok"));
    }

    @WebSocket(path = "/echo")
    public static class Echo {

        @OnTextMessage
        String process(String message) {
            return message;
        }

    }

    static void registerRoute(@Observes Router router) {
        router.route("/echo").handler(rc -> rc.response().end("ok"));
    }

}
