package io.quarkus.websockets.next.test.endpoints;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.websockets.next.OnMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketServerException;

public class TooManyOnMessageTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(TooManyOnMessage.class);
            })
            .setExpectedException(WebSocketServerException.class);

    @Test
    void verifyThatEndpointWithMultipleOnMessageMethodsFailsToDeploy() {

    }

    @WebSocket("/ws")
    public static class TooManyOnMessage {
        @OnMessage
        public void onMessage(String message) {
        }

        @OnMessage
        public void onMessage2(String message) {
        }
    }

}
