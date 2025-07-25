package io.quarkus.websockets.next.test.errors;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocketClient;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.WebSocketException;

public class ServerErrorHandlerOnClientEndpointTest {
    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Echo.class);
            })
            .assertException(e -> {
                assertInstanceOf(WebSocketException.class, e);
                assertTrue(e.getMessage().contains(
                        "@OnError callback on @WebSocketClient must not accept WebSocketConnection"));
            });

    @Test
    void trigger() {
    }

    @WebSocketClient(path = "/echo")
    public static class Echo {
        @OnTextMessage
        void echo(String msg) {
        }

        @OnError
        String handle(Exception e, WebSocketConnection serverConnection) {
            return "error";
        }
    }
}
